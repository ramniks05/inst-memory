package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.DesignationResponse;
import com.dolr.backend.service.DesignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/designations")
@RequiredArgsConstructor
public class DesignationController {

	private final DesignationService designationService;

	@GetMapping
	public ApiResponse<List<DesignationResponse>> listActive() {
		return ApiResponse.ok(designationService.listActiveDesignations());
	}
}
