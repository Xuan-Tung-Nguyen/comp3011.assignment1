package comp3011.assignment1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import comp3011.assignment1.controller.AdminController;
import comp3011.assignment1.service.ShutdownExecutor;
import org.springframework.test.annotation.DirtiesContext;

//Tests uptime and shutdown endpoints, including repeated shutdown requests and unexpected errors
@WebMvcTest(AdminController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ShutdownExecutor shutdownExecutor;

    //Testing uptime behaviour
    @Test
    void uptimeReturnsWellFormedTimestamps() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.utcServerStart").exists())
            .andExpect(jsonPath("$.utcNow").exists())
            .andExpect(jsonPath("$.serverUptimeSeconds").isNumber());
    }

    //Testing if controller actually called shutdown
    @Test
    void firstShutdownRequestIsAcceptedAndDelegatesToExecutor() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value("Graceful shutdown requested."));

        verify(shutdownExecutor, times(1)).initiateShutdown();
    }
    
    //Testing if it prevents the duplicate shutdown requests
    @Test
    void secondShutdownRequestReturnsConflictAndDoesNotDoubleTrigger() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shutdown")).andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"));

        verify(shutdownExecutor, times(1)).initiateShutdown(); // never called twice
    }

    //Testing unexpected exception by throwing response without exposing it
    @Test
    void unexpectedFailureDuringShutdownReturnsStandardErrorShape() throws Exception {
        doThrow(new RuntimeException("boom")).when(shutdownExecutor).initiateShutdown();

        mockMvc.perform(post("/api/v1/admin/shutdown"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
            .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"));
    }
}