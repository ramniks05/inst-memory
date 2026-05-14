package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.CreateDesignationRequest;
import com.dolr.backend.dto.DesignationResponse;
import com.dolr.backend.dto.UpdateDesignationRequest;
import com.dolr.backend.service.DesignationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/designations")
@RequiredArgsConstructor
public class AdminDesignationController {

	private final DesignationService designationService;

	@GetMapping
	public ApiResponse<List<DesignationResponse>> listAll(
			@RequestHeader("Authorization") String authHeader) {
		return designationService.listAllForAdmin(authHeader);
	}

	@PostMapping
	public ApiResponse<DesignationResponse> create(
			@RequestHeader("Authorization") String authHeader,
			@Valid @RequestBody CreateDesignationRequest request) {
		return designationService.create(authHeader, request);
	}

	@PutMapping("/{id}")
	public ApiResponse<DesignationResponse> update(
			@RequestHeader("Authorization") String authHeader,
			@PathVariable Long id,
			@Valid @RequestBody UpdateDesignationRequest request) {
		return designationService.update(authHeader, id, request);
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> deactivate(
			@RequestHeader("Authorization") String authHeader,
			@PathVariable Long id) {
		return designationService.deactivate(authHeader, id);
	}
}
