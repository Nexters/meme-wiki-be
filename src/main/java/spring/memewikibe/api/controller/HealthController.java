package spring.memewikibe.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.memewikibe.support.response.ApiResponse;

/**
 * Controller for health check endpoint.
 * Provides a simple endpoint to verify the application is running.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("healthy");
    }

}
