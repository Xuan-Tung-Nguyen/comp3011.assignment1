package comp3011.assignment1.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.service.SttService;
import comp3011.assignment1.service.TokenUsageTracker;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/speech")
public class SpeechController {

    private final SttService sttService;
    private final TokenUsageTracker tracker;

    public SpeechController(SttService sttService, TokenUsageTracker tracker) {
        this.sttService = sttService;
        this.tracker = tracker;
    }

    @PostMapping("/transcribe")
    //Change return type to Mono as the service is asynchronous
    public Mono<ResponseEntity<Map<String, String>>> transcribe(
            @RequestBody byte[] audioBytes,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        if (audioBytes == null || audioBytes.length == 0) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("message", "No audio data received.")));
        }

        return sttService.transcribe(audioBytes, contentType)
            .map(result -> {
                tracker.record(result.inputTokens(), result.outputTokens());
                return ResponseEntity.ok(Map.of("transcript", result.transcript()));
            })
            .onErrorResume(err -> Mono.just(
                ResponseEntity.status(502).body(Map.of("message", "Speech-to-text service unavailable."))
            ));
    }
}