import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const NOTIFY_WAIT_TIMEOUT_MS = 15000;
const VUS = __ENV.VUS ? parseInt(__ENV.VUS) : 10;

export const e2eLatency = new Trend('e2e_post_latency_ms', true);

export const options = {
  scenarios: {
    warmup: {
      executor: 'shared-iterations',
      exec: 'warmup',
      vus: VUS,
      iterations: VUS * 2,
      startTime: '0s',
    },
    measured: {
      executor: 'shared-iterations',
      exec: 'measured',
      vus: VUS,
      iterations: Math.max(VUS, 100),
      startTime: '15s',
    },
  },
};

function stompConnectFrame() {
  return `CONNECT\naccept-version:1.1,1.2\nheart-beat:0,0\n\n\0`;
}

function stompSubscribeFrame(destination, subId) {
  return `SUBSCRIBE\nid:${subId}\ndestination:${destination}\n\n\0`;
}

export function parseStompFrame(raw) {
  const frame = raw.endsWith('\0') ? raw.slice(0, -1) : raw;
  const [headerPart, ...bodyParts] = frame.split('\n\n');
  const lines = headerPart.split('\n');
  const command = lines[0];
  const headers = {};

  for (let i = 1; i < lines.length; i++) {
    const idx = lines[i].indexOf(':');

    if (idx > -1) {
      headers[lines[i].slice(0, idx)] = lines[i].slice(idx + 1);
    }
  }

  return {
    command,
    headers,
    body: bodyParts.join('\n\n'),
  };
}

function runPipeline(shouldRecord) {
  const email = `test_${uuidv4()}@example.com`;
  const password = 'testpassword123';
  const username = `smoke_${uuidv4().slice(0, 8)}`;

  const registerUser = http.post(
    'http://localhost:8080/user/graphql',
    JSON.stringify({
      query: `mutation{register(input:{username:"${username}",email:"${email}",password:"${password}"}){accessToken}}`,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
    },
  );

  check(registerUser, {
    'register succeeded': (r) =>
      r.status === 200 &&
      r.json('data.register.accessToken') != null,
  });

  const token = registerUser.json('data.register.accessToken');

  if (!token) {
    console.error(
      'Registration failed, aborting VU:',
      registerUser.body,
    );
    return;
  }

  const wsUrl = `ws://localhost:8083/ws?userId=${encodeURIComponent(email)}`;

  let subscribed = false;
  let notificationReceived = false;
  let postId = null;
  let t0 = null;

  const res = ws.connect(wsUrl, {}, function (socket) {
    socket.on('open', () => {
      socket.send(stompConnectFrame());
    });

    socket.on('message', (data) => {
      const frame = parseStompFrame(data);

      if (frame.command === 'CONNECTED' && !subscribed) {
        subscribed = true;

        socket.send(
          stompSubscribeFrame(
            '/user/queue/notifications',
            'sub-0',
          ),
        );

        t0 = Date.now();

        const createRes = http.post(
          'http://localhost:8080/feed/graphql',
          JSON.stringify({
            query: `mutation{createPost(input:{content:"smoke test post"}){id}}`,
          }),
          {
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${token}`,
            },
          },
        );

        check(createRes, {
          'createPost succeeded': (r) =>
            r.status === 200 &&
            r.json('data.createPost.id') != null,
        });

        postId = createRes.json('data.createPost.id');

        if (!postId) {
          console.error(
            'createPost failed, closing socket:',
            createRes.body,
          );
          socket.close();
        }
      }

      if (frame.command === 'MESSAGE' && postId) {
        try {
          const payload = JSON.parse(frame.body);

          if (payload.sourcePostId === postId) {
            const t1 = Date.now();
            const latency = t1 - t0;

            if (shouldRecord) {
              e2eLatency.add(latency);
            }

            notificationReceived = true;

            console.log(
              `Received matching notification. E2E latency: ${latency}ms`,
            );

            socket.close();
          }
        } catch (e) {
          console.error(
            'Failed to parse notification body:',
            frame.body,
          );
        }
      }
    });

    socket.on('error', (e) => {
      console.error('WS error:', e.error());
    });

    socket.setTimeout(() => {
      if (!notificationReceived) {
        console.error(
          `Timed out after ${NOTIFY_WAIT_TIMEOUT_MS}ms waiting for notification. postId=${postId}`,
        );
      }

      socket.close();
    }, NOTIFY_WAIT_TIMEOUT_MS);
  });

  check(res, {
    'ws handshake succeeded': (r) => r && r.status === 101,
  });

  check(null, {
    'notification received before timeout': () => notificationReceived,
  });

  sleep(1);
}

export function warmup() {
  runPipeline(false);
}

export function measured() {
  runPipeline(true);
}