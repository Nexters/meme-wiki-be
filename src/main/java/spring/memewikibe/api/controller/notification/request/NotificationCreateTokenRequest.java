package spring.memewikibe.api.controller.notification.request;

import jakarta.validation.constraints.NotBlank;

public record NotificationCreateTokenRequest(
    @NotBlank(message = "Token is required")
    String token
) {
}
