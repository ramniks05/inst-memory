package com.dolr.backend.controller;

import com.dolr.backend.security.AdminAuthHelper;
import com.dolr.backend.service.DocumentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class DocumentFileController {

	private final AdminAuthHelper adminAuthHelper;
	private final DocumentService documentService;

	@GetMapping("/documents/file/{id}")
	public ResponseEntity<Resource> download(@PathVariable long id, HttpSession session) {
		return adminAuthHelper.userFromSession(session)
				.map(u -> documentService.downloadForViewer(id, u))
				.orElseGet(() -> ResponseEntity.status(401).build());
	}
}
