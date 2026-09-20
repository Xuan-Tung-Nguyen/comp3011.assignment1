package comp3011.assignment1.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

//Logs every API request’s method, path, status, and response time without exposing sensitive data
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	//Creates a logger for recording information about incoming requests
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    //Runs once for every incoming HTTP request
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
    	//Record the time when the request starts
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
        	//Calculate how long the request took to complete
            long durationMs = System.currentTimeMillis() - start;
            
            //Log only the HTTP method, path, status code, and duration
            //Headers and request/response bodies are intentionally excluded to ensure no sensitive information can be logged
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                response.getStatus(), durationMs);
        }
    }
}