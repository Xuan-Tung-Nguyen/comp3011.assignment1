package comp3011.assignment1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import comp3011.assignment1.controller.AdminController;
import comp3011.assignment1.service.ShutdownExecutor;
import comp3011.assignment1.service.ShutdownService;

import org.springframework.test.annotation.DirtiesContext;

//Tests uptime and shutdown endpoints, including repeated shutdown requests and unexpected errors
@WebMvcTest(AdminController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ShutdownService shutdownService;

    //Verifies the uptime endpoint returns a successful response with the expected fields.
    @Test
    void uptimeReturnsWellFormedTimestamps() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.utcServerStart").exists())
            .andExpect(jsonPath("$.utcNow").exists())
            .andExpect(jsonPath("$.serverUptimeSeconds").isNumber());
    }

    //Verifies a shutdown request is accepted when no shutdown is already in progress.
    @Test
    void acceptedShutdownRequestReturns202() throws Exception {
        when(shutdownService.requestShutdown()).thenReturn(true);

        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value("Graceful shutdown requested."));

        verify(shutdownService).requestShutdown();
    }

    //Verifies a second shutdown request returns 409 Conflict with the required error format.
    @Test
    void shutdownAlreadyInProgressReturns409WithSpecErrorShape() throws Exception {
        when(shutdownService.requestShutdown()).thenReturn(false);

        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Graceful shutdown is already in progress."))
            .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"));
    }

    //Verifies unexpected service failures return a standard 500 Internal Server Error response.
    @Test
    void unexpectedFailureReturnsStandardErrorShape() throws Exception {
        when(shutdownService.requestShutdown()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
            .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"));
    }
}
