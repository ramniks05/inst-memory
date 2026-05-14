package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.DivisionResponse;
import com.dolr.backend.service.DivisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/divisions")
@RequiredArgsConstructor
public class DivisionController {

	private final DivisionService divisionService;

	/** Active divisions for dropdowns (registration, officer filters, etc.). */
	@GetMapping
	public ApiResponse<List<DivisionResponse>> listActive() {
		return ApiResponse.ok(divisionService.listActiveDivisions());
	}
}
