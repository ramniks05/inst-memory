package com.dolr.backend.controller;

import com.dolr.backend.security.AdminAuthHelper;
import com.dolr.backend.service.MprService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class MprFileController {

    private final AdminAuthHelper adminAuthHelper;
    private final MprService mprService;

    @GetMapping("/mpr/file/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id, HttpSession session) {
        return adminAuthHelper.userFromSession(session)
                .map(u -> mprService.download(u, id))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
