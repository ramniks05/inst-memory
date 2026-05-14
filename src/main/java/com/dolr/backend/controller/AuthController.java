package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.LoginRequest;
import com.dolr.backend.dto.LoginResponse;
import com.dolr.backend.repository.OfficerProjection;
import com.dolr.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/officers")
	public ApiResponse<List<OfficerProjection>> getOfficers(
			@RequestParam String department,
			@RequestParam String division) {
		return ApiResponse.ok(authService.getReportingOfficers(department, division));
	}
}
