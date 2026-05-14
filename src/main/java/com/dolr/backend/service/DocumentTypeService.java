package com.dolr.backend.service;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.CreateDocumentTypeRequest;
import com.dolr.backend.dto.DocumentTypeResponse;
import com.dolr.backend.entity.DocumentType;
import com.dolr.backend.repository.DocumentTypeRepository;
import com.dolr.backend.security.AdminAuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentTypeService {

	private final DocumentTypeRepository documentTypeRepository;
	private final AdminAuthHelper adminAuthHelper;

	public List<DocumentTypeResponse> listActiveTypes() {
		return documentTypeRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	public ApiResponse<List<DocumentTypeResponse>> listAllForAdmin(String authHeader) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return ApiResponse.ok(listAllAsAdmin());
	}

	public List<DocumentTypeResponse> listAllAsAdmin() {
		return documentTypeRepository.findAllByOrderBySortOrderAscNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public ApiResponse<DocumentTypeResponse> create(String authHeader, CreateDocumentTypeRequest request) {
		Optional<String> deny = adminAuthHelper.denyUnlessAdmin(authHeader);
		if (deny.isPresent()) {
			return ApiResponse.error(deny.get());
		}
		return createAsAdmin(request);
	}

	@Transactional
	public ApiResponse<DocumentTypeResponse> createAsAdmin(CreateDocumentTypeRequest request) {
		if (request.getName() == null) {
			return ApiResponse.error("Name is required");
		}
		String name = request.getName().trim();
		if (name.isEmpty()) {
			return ApiResponse.error("Name is required");
		}
		if (documentTypeRepository.findByNameIgnoreCase(name).isPresent()) {
			return ApiResponse.error("A document type with this name already exists");
		}
		int sort = request.getSortOrder() != null ? request.getSortOrder() : 0;
		DocumentType saved = documentTypeRepository.save(DocumentType.builder()
				.name(name)
				.sortOrder(sort)
				.active(true)
				.build());
		return ApiResponse.ok(toResponse(saved));
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
		DocumentType t = documentTypeRepository.findById(id).orElse(null);
		if (t == null) {
			return ApiResponse.error("Document type not found");
		}
		t.setActive(false);
		documentTypeRepository.save(t);
		return ApiResponse.ok("Document type deactivated");
	}

	private DocumentTypeResponse toResponse(DocumentType t) {
		return DocumentTypeResponse.builder()
				.id(t.getId())
				.name(t.getName())
				.sortOrder(t.getSortOrder())
				.active(t.getActive())
				.build();
	}
}
