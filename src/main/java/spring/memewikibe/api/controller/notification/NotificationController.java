package spring.memewikibe.api.controller.notification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.notification.request.NotificationCreateTokenRequest;
import spring.memewikibe.application.notification.NotificationService;
import spring.memewikibe.support.response.ApiResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> registerToken(
        @Valid @RequestBody NotificationCreateTokenRequest request
    ) {
        notificationService.registerNotificationToken(request.token(), request.deviceId(), request.platform());
        return ApiResponse.success();
    }

}
