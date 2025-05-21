package com.main.triviatreckapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.triviatreckapp.entities.Question;
import com.main.triviatreckapp.service.QuestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@SpringBootApplication
public class TriviatreckAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriviatreckAppApplication.class, args);
    }


    @Bean
    CommandLineRunner runner(QuestionService questionService) {
        return args -> {

            ObjectMapper mapper = new ObjectMapper();


            var resource = new ClassPathResource("static/Questions.json");
            List<Question> questions = mapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<Question>>() {}
            );
            questionService.saveAll(questions);

        };
    }

}
