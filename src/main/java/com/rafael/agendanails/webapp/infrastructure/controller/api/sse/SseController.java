package com.rafael.agendanails.webapp.infrastructure.controller.api.sse;

import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.sse.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "Server-Sent Events notifications")
public class SseController {

    private final SseService sseService;

    @Operation(summary = "Subscribe to notifications", description = "Opens an SSE stream for notifications.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE stream opened",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getId();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        sseService.addEmitter(userId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Connection established for user: " + userId));
        } catch (IOException e) {
            sseService.removeEmitter(userId);
        }

        return emitter;
    }
}
