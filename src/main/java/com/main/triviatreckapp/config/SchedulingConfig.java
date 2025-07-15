package com.main.triviatreckapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler gameTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);                    // nombre de threads dans le pool
        scheduler.setThreadNamePrefix("quiz-sched-");
        scheduler.initialize();                      // indispensable pour l’initialisation interne
        return scheduler;
    }

}
