package spring.memewikibe.api.controller.notification.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NotificationCreateTokenRequest(
    @NotBlank(message = "Token is required")
    String token,

    @NotBlank(message = "Device ID is required")
    String deviceId,

    @NotBlank(message = "Platform is required")
    @Pattern(regexp = "^(ANDROID|IOS|android|ios)$", message = "Platform must be either ANDROID or IOS")
    String platform
) {
}
