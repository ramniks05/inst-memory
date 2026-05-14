package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.DocumentListItemResponse;
import com.dolr.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentService documentService;

	@GetMapping("/me")
	public ApiResponse<List<DocumentListItemResponse>> listMine(
			@RequestHeader(value = "Authorization", required = false) String authHeader) {
		return documentService.listForBearer(authHeader);
	}
}
