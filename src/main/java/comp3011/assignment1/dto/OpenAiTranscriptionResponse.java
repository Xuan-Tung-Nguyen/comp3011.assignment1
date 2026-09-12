package comp3011.assignment1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) //ignore unneeded fields(logprobs, etc.)
public record OpenAiTranscriptionResponse(String text, Usage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
        String type,
        //Map the snake_case keys coming from the OpenAI JSON response 
        //to camelCase Java variables
        @JsonProperty("input_tokens") long inputTokens,
        @JsonProperty("output_tokens") long outputTokens,
        @JsonProperty("total_tokens") long totalTokens
    ) {
    }
}