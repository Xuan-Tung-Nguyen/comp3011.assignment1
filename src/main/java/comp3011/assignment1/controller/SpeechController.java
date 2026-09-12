package comp3011.assignment1.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/speech")
public class SpeechController {

    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, String>> transcribeStub(
            @RequestBody byte[] audioBytes) {

    	//Stub transcription to test if everything works
        String stubTranscript =
                "Received "
                + audioBytes.length
                + " bytes (stub transcription for testing).";

        return ResponseEntity.ok(
                Map.of("transcript", stubTranscript)
        );
    }
}