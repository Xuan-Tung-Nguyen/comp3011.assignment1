package comp3011.assignment1.error;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import comp3011.assignment1.dto.ErrorResponse;

//Handling unexpected controller exceptions
//Prevents using multiple try catch inside every controller.
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

    	//Avoid return exception message that leaking details
        ErrorResponse body =
                new ErrorResponse(
                        Instant.now().toString(),
                        500,
                        "Internal Server Error",
                        "An unexpected server error occurred.",
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(500)
                .body(body);
    }
}