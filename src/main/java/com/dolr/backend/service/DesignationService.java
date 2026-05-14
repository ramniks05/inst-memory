package com.dolr.backend.service;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.CreateDesignationRequest;
import com.dolr.backend.dto.DesignationResponse;
import com.dolr.backend.dto.UpdateDesignationRequest;
import com.dolr.backend.entity.Designation;
import com.dolr.backend.repository.DesignationRepository;
import com.dolr.backend.security.AdminAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DesignationService {

	private final DesignationRepository designationRepository;
	private final AdminAuthHelper adminAuthHelper;

	public List<DesignationResponse> listActiveDesignations() {
		return designationRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	public ApiResponse<List<DesignationResponse>> listAllForAdmin(String authHeader) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return ApiResponse.ok(listAllAsAdmin());
	}

	public List<DesignationResponse> listAllAsAdmin() {
		return designationRepository.findAllByOrderBySortOrderAscNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public ApiResponse<DesignationResponse> create(String authHeader, CreateDesignationRequest request) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return createAsAdmin(request);
	}

	@Transactional
	public ApiResponse<DesignationResponse> createAsAdmin(CreateDesignationRequest request) {
		if (request.getName() == null) {
			return ApiResponse.error("Name is required");
		}
		String name = request.getName().trim();
		if (name.isEmpty()) {
			return ApiResponse.error("Name is required");
		}
		if (designationRepository.findByNameIgnoreCase(name).isPresent()) {
			return ApiResponse.error("A designation with this name already exists");
		}
		boolean allDiv = Boolean.TRUE.equals(request.getHandlesAllDivisions());
		int sort = request.getSortOrder() != null ? request.getSortOrder() : 0;
		Designation saved = designationRepository.save(Designation.builder()
				.name(name)
				.handlesAllDivisions(allDiv)
				.sortOrder(sort)
				.active(true)
				.build());
		return ApiResponse.ok(toResponse(saved));
	}

	@Transactional
	public ApiResponse<DesignationResponse> update(String authHeader, Long id, UpdateDesignationRequest request) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return updateAsAdmin(id, request);
	}

	@Transactional
	public ApiResponse<DesignationResponse> updateAsAdmin(Long id, UpdateDesignationRequest request) {
		Designation d = designationRepository.findById(id).orElse(null);
		if (d == null) {
			return ApiResponse.error("Designation not found");
		}
		if (request.getName() != null && !request.getName().isBlank()) {
			String newName = request.getName().trim();
			Optional<Designation> clash = designationRepository.findByNameIgnoreCase(newName);
			if (clash.isPresent() && !clash.get().getId().equals(id)) {
				return ApiResponse.error("Another designation already uses this name");
			}
			d.setName(newName);
		}
		if (request.getHandlesAllDivisions() != null) {
			d.setHandlesAllDivisions(request.getHandlesAllDivisions());
		}
		if (request.getSortOrder() != null) {
			d.setSortOrder(request.getSortOrder());
		}
		if (request.getActive() != null) {
			d.setActive(request.getActive());
		}
		return ApiResponse.ok(toResponse(designationRepository.save(d)));
	}

	@Transactional
	public ApiResponse<Void> deactivate(String authHeader, Long id) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return deactivateAsAdmin(id);
	}

	@Transactional
	public ApiResponse<Void> deactivateAsAdmin(Long id) {
		Designation d = designationRepository.findById(id).orElse(null);
		if (d == null) {
			return ApiResponse.error("Designation not found");
		}
		d.setActive(false);
		designationRepository.save(d);
		return ApiResponse.ok("Designation deactivated");
	}

	private DesignationResponse toResponse(Designation d) {
		return DesignationResponse.builder()
				.id(d.getId())
				.name(d.getName())
				.handlesAllDivisions(Boolean.TRUE.equals(d.getHandlesAllDivisions()))
				.sortOrder(d.getSortOrder())
				.active(d.getActive())
				.build();
	}
}
