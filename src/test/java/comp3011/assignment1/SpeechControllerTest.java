package comp3011.assignment1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import comp3011.assignment1.controller.SpeechController;
import comp3011.assignment1.dto.TranscriptionResult;
import comp3011.assignment1.service.SttService;
import comp3011.assignment1.service.TokenUsageTracker;
import reactor.core.publisher.Mono;

@WebMvcTest(SpeechController.class)
@Import(TokenUsageTracker.class) //Import the real tracker to verify it was actually updated
class SpeechControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenUsageTracker tracker;
    @MockitoBean SttService sttService; //Testing the REST API with stub STT service

    //Test that valid audio is transcribed successfully and token usage is recorded
    @Test
    void successfulTranscriptionReturnsTextAndRecordsTokenUsage() throws Exception {
        when(sttService.transcribe(any(), any()))
            .thenReturn(Mono.just(new TranscriptionResult("hello world", 5, 3)));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/speech/transcribe")
                .contentType("audio/webm")
                .content("fake-audio-bytes".getBytes()))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transcript").value("hello world"));

        assertThat(tracker.getInputTokens()).isEqualTo(5);
        assertThat(tracker.getOutputTokens()).isEqualTo(3);
    }

    //Test that an empty audio request is rejected and the STT service is not called
    @Test
    void emptyAudioReturnsBadRequestWithoutCallingSttService() throws Exception {
        mockMvc.perform(post("/api/v1/speech/transcribe")
                .contentType("audio/webm")
                .content(new byte[0]))
            .andExpect(status().isBadRequest());

        verify(sttService, never()).transcribe(any(), any());
    }

    //Test that an STT service failure is handled as a 502 Bad Gateway response
    @Test
    void sttServiceFailureReturnsBadGatewayNotAServerCrash() throws Exception {
        when(sttService.transcribe(any(), any()))
            .thenReturn(Mono.error(new RuntimeException("upstream unavailable")));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/speech/transcribe")
                .contentType("audio/webm")
                .content("fake-audio-bytes".getBytes()))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadGateway());
    }
}
