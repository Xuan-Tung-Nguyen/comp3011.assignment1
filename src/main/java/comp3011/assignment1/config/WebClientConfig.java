package comp3011.assignment1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Profile("!stub") //Never requires OpenAI Api Key to be set when running tests
public class WebClientConfig {

    @Bean
    public WebClient openAiWebClient(@Value("${OPENAI_API_KEY}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not set");
        }
        return WebClient.builder()
            .baseUrl("https://api.openai.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    }
}
