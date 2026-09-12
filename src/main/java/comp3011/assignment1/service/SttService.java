package comp3011.assignment1.service;

import comp3011.assignment1.dto.TranscriptionResult;
import reactor.core.publisher.Mono;

//Use Mono to give transcription result when the STT processing finishes
//without blocking the thread
public interface SttService {
    Mono<TranscriptionResult> transcribe(
    		byte[] audioBytes, String contentType);
}
