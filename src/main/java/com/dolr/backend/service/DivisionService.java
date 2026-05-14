package com.dolr.backend.service;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.CreateDivisionRequest;
import com.dolr.backend.dto.DivisionResponse;
import com.dolr.backend.dto.UpdateDivisionRequest;
import com.dolr.backend.entity.Division;
import com.dolr.backend.repository.DivisionRepository;
import com.dolr.backend.security.AdminAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DivisionService {

	private final DivisionRepository divisionRepository;
	private final AdminAuthHelper adminAuthHelper;

	public List<DivisionResponse> listActiveDivisions() {
		return divisionRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	public ApiResponse<List<DivisionResponse>> listAllDivisionsForAdmin(String authHeader) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return ApiResponse.ok(listAllDivisionsAsAdmin());
	}

	public List<DivisionResponse> listAllDivisionsAsAdmin() {
		return divisionRepository.findAllByOrderBySortOrderAscNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public ApiResponse<DivisionResponse> createDivision(String authHeader, CreateDivisionRequest request) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return createDivisionAsAdmin(request);
	}

	@Transactional
	public ApiResponse<DivisionResponse> createDivisionAsAdmin(CreateDivisionRequest request) {
		if (request.getName() == null) {
			return ApiResponse.error("Name is required");
		}
		String name = request.getName().trim();
		if (name.isEmpty()) {
			return ApiResponse.error("Name is required");
		}
		if (divisionRepository.findByNameIgnoreCase(name).isPresent()) {
			return ApiResponse.error("A division with this name already exists");
		}
		int sort = request.getSortOrder() != null ? request.getSortOrder() : 0;
		Division saved = divisionRepository.save(Division.builder()
				.name(name)
				.sortOrder(sort)
				.active(true)
				.build());
		return ApiResponse.ok(toResponse(saved));
	}

	@Transactional
	public ApiResponse<DivisionResponse> updateDivision(String authHeader, Long id, UpdateDivisionRequest request) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return updateDivisionAsAdmin(id, request);
	}

	@Transactional
	public ApiResponse<DivisionResponse> updateDivisionAsAdmin(Long id, UpdateDivisionRequest request) {
		Division d = divisionRepository.findById(id)
				.orElse(null);
		if (d == null) {
			return ApiResponse.error("Division not found");
		}
		if (request.getName() != null && !request.getName().isBlank()) {
			String newName = request.getName().trim();
			Optional<Division> clash = divisionRepository.findByNameIgnoreCase(newName);
			if (clash.isPresent() && !clash.get().getId().equals(id)) {
				return ApiResponse.error("Another division already uses this name");
			}
			d.setName(newName);
		}
		if (request.getSortOrder() != null) {
			d.setSortOrder(request.getSortOrder());
		}
		if (request.getActive() != null) {
			d.setActive(request.getActive());
		}
		return ApiResponse.ok(toResponse(divisionRepository.save(d)));
	}

	@Transactional
	public ApiResponse<Void> deactivateDivision(String authHeader, Long id) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return deactivateDivisionAsAdmin(id);
	}

	@Transactional
	public ApiResponse<Void> deactivateDivisionAsAdmin(Long id) {
		Division d = divisionRepository.findById(id).orElse(null);
		if (d == null) {
			return ApiResponse.error("Division not found");
		}
		d.setActive(false);
		divisionRepository.save(d);
		return ApiResponse.ok("Division deactivated");
	}

	private DivisionResponse toResponse(Division d) {
		return DivisionResponse.builder()
				.id(d.getId())
				.name(d.getName())
				.sortOrder(d.getSortOrder())
				.active(d.getActive())
				.build();
	}
}
