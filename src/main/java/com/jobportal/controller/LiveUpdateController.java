package com.jobportal.controller;

import com.jobportal.model.UserAccount;
import com.jobportal.service.LiveUpdateService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class LiveUpdateController {
    private final LiveUpdateService liveUpdateService;

    public LiveUpdateController(LiveUpdateService liveUpdateService) {
        this.liveUpdateService = liveUpdateService;
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@AuthenticationPrincipal UserAccount user) {
        return liveUpdateService.subscribe(user.getId());
    }
}
