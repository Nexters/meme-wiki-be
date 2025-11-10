package spring.memewikibe.api.controller.notification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import spring.memewikibe.api.controller.notification.request.NotificationCreateTokenRequest;
import spring.memewikibe.application.notification.NotificationTokenRegister;
import spring.memewikibe.support.response.ApiResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationTokenRegister notificationTokenRegister;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> registerToken(
        @Valid @RequestBody NotificationCreateTokenRequest request
    ) {
        notificationTokenRegister.registerToken(request.token());
        return ApiResponse.success();
    }

}
