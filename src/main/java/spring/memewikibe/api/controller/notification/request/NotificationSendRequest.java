package spring.memewikibe.api.controller.notification.request;

import jakarta.validation.constraints.NotBlank;

public record NotificationSendRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Body is required")
    String body,

    String data
) {
}