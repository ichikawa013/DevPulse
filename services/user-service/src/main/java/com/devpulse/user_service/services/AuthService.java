package com.devpulse.user_service.services;


import com.devpulse.user_service.dto.requests.LoginRequest;
import com.devpulse.user_service.dto.requests.RegisterRequest;
import com.devpulse.user_service.dto.responses.AuthResponse;
import com.devpulse.user_service.entities.Role;
import com.devpulse.user_service.entities.User;
import com.devpulse.user_service.exception.BadRequestHandler;
import com.devpulse.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Value("${application.security.jwt.expiration-time}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-expiration}")
    private long refreshExpiration;


    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            log.warn("Email already exists: {}", registerRequest.getEmail());
            throw new BadRequestHandler("Email already exists");
        }
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            log.warn("Username already exists: {}", registerRequest.getUsername());
            throw new BadRequestHandler("Username already taken");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setDisplayName(registerRequest.getDisplayName());
        user.setRole(Role.ROLE_USER);
        user.setCreatedAt(Instant.now());

        userRepository.save(user);
        log.info("User registered with username: {}", user.getUsername());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        redisTemplate.opsForValue().set(
                "refresh_token:" + user.getUsername(),
                refreshToken,
                Duration.ofMillis(refreshExpiration)
        );

        return new AuthResponse(accessToken, refreshToken, jwtExpiration, "Bearer");
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        redisTemplate.opsForValue().set(
                "refresh_token:" + userDetails.getUsername(),
                refreshToken,
                refreshExpiration,
                TimeUnit.MILLISECONDS
        );

        return new AuthResponse(accessToken, refreshToken, jwtExpiration, "Bearer ");
    }

    public AuthResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);

        String storedToken = redisTemplate.opsForValue()
                .get("refresh_token:" + username);
        if(storedToken == null) {
            throw new BadRequestHandler("Session Expired, please login again");
        }

        if (!storedToken.equals(refreshToken)){
            throw new BadRequestHandler("Invalid refresh token");
        }

        UserDetails userDetails =  customUserDetailsService.loadUserByUsername(username);

        if(!jwtService.isTokenValid(refreshToken, userDetails)) {
            redisTemplate.delete("refresh_token:" + username);
            throw new BadRequestHandler("Refresh token expired");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return new AuthResponse(newAccessToken, null, jwtExpiration, "Bearer");
    }

    public void logout(String username) {
        redisTemplate.delete("refresh_token:" + username);
    }

}
