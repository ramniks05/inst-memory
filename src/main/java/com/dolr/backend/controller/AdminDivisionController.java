package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.CreateDivisionRequest;
import com.dolr.backend.dto.DivisionResponse;
import com.dolr.backend.dto.UpdateDivisionRequest;
import com.dolr.backend.service.DivisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/divisions")
@RequiredArgsConstructor
public class AdminDivisionController {

	private final DivisionService divisionService;

	@GetMapping
	public ApiResponse<List<DivisionResponse>> listAll(
			@RequestHeader("Authorization") String authHeader) {
		return divisionService.listAllDivisionsForAdmin(authHeader);
	}

	@PostMapping
	public ApiResponse<DivisionResponse> create(
			@RequestHeader("Authorization") String authHeader,
			@Valid @RequestBody CreateDivisionRequest request) {
		return divisionService.createDivision(authHeader, request);
	}

	@PutMapping("/{id}")
	public ApiResponse<DivisionResponse> update(
			@RequestHeader("Authorization") String authHeader,
			@PathVariable Long id,
			@Valid @RequestBody UpdateDivisionRequest request) {
		return divisionService.updateDivision(authHeader, id, request);
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> deactivate(
			@RequestHeader("Authorization") String authHeader,
			@PathVariable Long id) {
		return divisionService.deactivateDivision(authHeader, id);
	}
}
