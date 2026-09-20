package comp3011.assignment1;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import comp3011.assignment1.controller.StatsController;
import comp3011.assignment1.service.TokenUsageTracker;

//Checks that the stats endpoint correctly reports token usage
@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TokenUsageTracker tracker;

    @Test
    void statsReflectCurrentCounterValues() throws Exception {
        when(tracker.getInputTokens()).thenReturn(42L);
        when(tracker.getOutputTokens()).thenReturn(17L);

        mockMvc.perform(get("/api/v1/global/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inputTokens").value(42))
            .andExpect(jsonPath("$.outputTokens").value(17));
    }
}