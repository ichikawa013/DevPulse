package com.devpulse.ai;

import com.devpulse.ai.tools.KafkaLagTool;
import com.devpulse.ai.tools.LatencyTool;
import com.devpulse.ai.tools.LogTool;
import com.devpulse.ai.tools.PodStatusTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiApplication.class, args);
	}

    @Bean
    public ToolCallbackProvider devpulseTools(
            KafkaLagTool kafkaLagTool,
            PodStatusTool podStatusTool,
            LogTool logTool,
            LatencyTool latencyTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        kafkaLagTool,
                        podStatusTool,
                        logTool,
                        latencyTool
                )
                .build();
    }
}
