package comp3011.assignment1.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import comp3011.assignment1.dto.TranscriptionResult;
import reactor.core.publisher.Mono;

@Service
@Profile("stub")
public class StubSttService implements SttService {
    @Override
    public Mono<TranscriptionResult> transcribe(byte[] audioBytes, String contentType) {
        return Mono.just(new TranscriptionResult(
            "stub transcript for " + audioBytes.length + " bytes", 1, 1));
    }
}