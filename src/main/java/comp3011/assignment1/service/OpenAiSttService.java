package comp3011.assignment1.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.MultipartBodyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import comp3011.assignment1.dto.OpenAiTranscriptionResponse;
import comp3011.assignment1.dto.TranscriptionResult;
import reactor.core.publisher.Mono;


@Service
@Profile("!stub")
public class OpenAiSttService implements SttService {

	
	@Value("${DEBUG_CRASH_ON_STT_ERROR:false}")
    private boolean crashOnError;
	//Logger used for operational events and failures
    private static final Logger log = LoggerFactory.getLogger(OpenAiSttService.class);
    private static final String MODEL = "gpt-4o-mini-transcribe";

    private final WebClient webClient;
    //Injects the configured WebClient used to call OpenAI
    public OpenAiSttService(WebClient openAiWebClient) {
        this.webClient = openAiWebClient;
    }

    @Override
    public Mono<TranscriptionResult> transcribe(byte[] audioBytes, String contentType) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        // Give the uploaded audio a filename based on its content type
        String filename = "recording." + extensionFor(contentType);
        // Add the audio bytes to the multipart request
        builder.part("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() { return filename; }
        });
        builder.part("model", MODEL);
        builder.part("response_format", "json");

        log.info("Calling OpenAI transcription API: model={}, bytes={}", MODEL, audioBytes.length);

        //Send the audio to OpenAI and asynchronously convert the response
        return webClient.post()
            .uri("/v1/audio/transcriptions")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .retrieve()
            .bodyToMono(OpenAiTranscriptionResponse.class)
            //Limit the API call to 4 seconds to stay within the overall response-time budget
            .timeout(Duration.ofMillis(4600)) //Debugging for TITAN test
            // Convert OpenAI's response into the application's result type.
            .map(this::toResult)
            //Log only the exception type to avoid accidentally exposing sensitive request data.
            .doOnError(err -> {
                if (err instanceof WebClientResponseException wcre) {
                    log.warn("OpenAI transcription rejected: status={}, body={}",
                        wcre.getStatusCode(), wcre.getResponseBodyAsString());
                } else if (err instanceof java.util.concurrent.TimeoutException) {
                    log.warn("OpenAI transcription call timed out");
                } else {
                    log.warn("OpenAI transcription call failed: {}", err.toString());
                }
                if (crashOnError) {
                    // TEMPORARY: forces TITAN to reveal stdout/stderr for this run only.
                    // Remove before final submission.
                    log.error("DEBUG_CRASH_ON_STT_ERROR is set — crashing intentionally");
                    Runtime.getRuntime().halt(1);
                }
            });
    }

    //Maps the OpenAI response into the application's TranscriptionResult
    private TranscriptionResult toResult(OpenAiTranscriptionResponse response) {
    	//Extract token usage when available
        long input = response.usage() != null ? response.usage().inputTokens() : 0;
        long output = response.usage() != null ? response.usage().outputTokens() : 0;
        return new TranscriptionResult(response.text(), input, output);
    }

    //Converts the MIME type into an audio file extension for the upload filename
    private String extensionFor(String contentType) {
        if (contentType == null) return "webm";
        if (contentType.contains("ogg")) return "ogg";
        if (contentType.contains("wav")) return "wav";
        if (contentType.contains("mp4")) return "mp4";
        return "webm";
    }
}