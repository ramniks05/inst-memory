package com.dolr.backend.service;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.DocumentListItemResponse;
import com.dolr.backend.entity.Designation;
import com.dolr.backend.entity.Document;
import com.dolr.backend.entity.DocumentType;
import com.dolr.backend.entity.User;
import com.dolr.backend.repository.DesignationRepository;
import com.dolr.backend.repository.DocumentRepository;
import com.dolr.backend.repository.DocumentTypeRepository;
import com.dolr.backend.repository.UserRepository;
import com.dolr.backend.security.JwtService;
import com.dolr.backend.security.RoleCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final DocumentTypeRepository documentTypeRepository;
	private final DesignationRepository designationRepository;
	private final UserRepository userRepository;
	private final JwtService jwtService;

	@Value("${app.documents.upload-dir:uploads/documents}")
	private String uploadDirConfig;

	@Value("${server.servlet.context-path:}")
	private String servletContextPath;

	@Transactional(readOnly = true)
	public List<DocumentListItemResponse> listVisibleForViewer(User viewer) {
		User live = userRepository.findById(viewer.getId()).orElse(viewer);
		List<Document> list;
		if (RoleCodes.isPortalAdministrator(live)) {
			list = documentRepository.findAllForAdminListing();
		} else if (live.getDesignationRef() != null) {
			list = documentRepository.findVisibleForDesignation(live.getDesignationRef().getId());
		} else {
			list = List.of();
		}
		return list.stream().map(this::toListItem).toList();
	}

	@Transactional(readOnly = true)
	public List<DocumentListItemResponse> listAllForAdminListing() {
		return documentRepository.findAllForAdminListing().stream().map(this::toListItem).toList();
	}

	/** Optional inclusive date bounds (upload day). Pass null for open-ended. */
	@Transactional(readOnly = true)
	public List<DocumentListItemResponse> listAllForAdminListing(LocalDate fromInclusive, LocalDate toInclusive) {
		if (fromInclusive == null && toInclusive == null) {
			return listAllForAdminListing();
		}
		LocalDateTime fromDt = fromInclusive != null ? fromInclusive.atStartOfDay() : null;
		LocalDateTime toExclusive = toInclusive != null ? toInclusive.plusDays(1).atStartOfDay() : null;
		return documentRepository.findAllForAdminListingFiltered(fromDt, toExclusive).stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean isOfficerMissingDesignation(Long userId) {
		User u = userRepository.findById(userId).orElse(null);
		if (u == null || RoleCodes.isPortalAdministrator(u)) {
			return false;
		}
		return u.getDesignationRef() == null;
	}

	@Transactional(readOnly = true)
	public ApiResponse<List<DocumentListItemResponse>> listForBearer(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ApiResponse.error("Authentication required. Send Authorization: Bearer <token>.");
		}
		try {
			String email = jwtService.extractEmail(authHeader.substring(7).trim());
			User u = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase()).orElse(null);
			if (u == null) {
				return ApiResponse.error("User not found");
			}
			return ApiResponse.ok(listVisibleForViewer(u));
		} catch (Exception e) {
			return ApiResponse.error("Invalid token");
		}
	}

	private DocumentListItemResponse toListItem(Document d) {
		String vis = d.getVisibleToDesignations().stream()
				.map(Designation::getName)
				.sorted()
				.collect(Collectors.joining(", "));
		return DocumentListItemResponse.builder()
				.id(d.getId())
				.title(d.getTitle())
				.documentTypeName(d.getDocumentType().getName())
				.uploadedByName(d.getUploadedBy().getFullName())
				.visibleToDesignationsSummary(vis)
				.uploadDate(d.getUploadDate())
				.downloadPath(buildDocumentDownloadPath(d.getId()))
				.build();
	}

	private String buildDocumentDownloadPath(long documentId) {
		String cp = servletContextPath == null ? "" : servletContextPath.trim();
		if ("/".equals(cp)) {
			cp = "";
		}
		if (!cp.isEmpty() && !cp.startsWith("/")) {
			cp = "/" + cp;
		}
		return cp + "/documents/file/" + documentId;
	}

	/**
	 * Officers may publish PDFs. Administrators may not upload (view and manage reference data only).
	 */
	@Transactional
	public ApiResponse<Void> uploadPdfDocument(User uploader, String title, Long documentTypeId,
			List<Long> designationIds, MultipartFile file) {
		if (uploader == null) {
			return ApiResponse.error("You must be signed in to upload.");
		}
		User live = userRepository.findById(uploader.getId()).orElse(uploader);
		if (RoleCodes.isPortalAdministrator(live)) {
			return ApiResponse.error("Administrators cannot upload documents.");
		}
		if (!RoleCodes.isOfficerRole(live.getRole())) {
			return ApiResponse.error("Only officer accounts may upload documents.");
		}
		if (title == null || title.isBlank()) {
			return ApiResponse.error("Title is required");
		}
		if (documentTypeId == null) {
			return ApiResponse.error("Document type is required");
		}
		if (designationIds == null || designationIds.isEmpty()) {
			return ApiResponse.error("Select at least one designation that may view this document");
		}
		if (file == null || file.isEmpty()) {
			return ApiResponse.error("PDF file is required");
		}
		final byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			return ApiResponse.error("Could not read file");
		}
		if (!isPdfBytes(bytes, file)) {
			return ApiResponse.error("Only PDF files are allowed");
		}
		DocumentType dt = documentTypeRepository.findById(documentTypeId).orElse(null);
		if (dt == null || !Boolean.TRUE.equals(dt.getActive())) {
			return ApiResponse.error("Document type not found");
		}
		Set<Designation> desigs = new HashSet<>();
		for (Long id : designationIds) {
			if (id == null) {
				continue;
			}
			Designation des = designationRepository.findById(id).orElse(null);
			if (des == null || !Boolean.TRUE.equals(des.getActive())) {
				return ApiResponse.error("Invalid designation selected");
			}
			desigs.add(des);
		}
		if (desigs.isEmpty()) {
			return ApiResponse.error("Select at least one designation that may view this document");
		}

		Path base = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
		try {
			Files.createDirectories(base);
		} catch (IOException e) {
			return ApiResponse.error("Could not create upload directory");
		}

		String storedUuid = java.util.UUID.randomUUID() + ".pdf";
		String relative = Year.now().getValue() + "/" + storedUuid;
		Path absFile = base.resolve(relative);
		try {
			Files.createDirectories(absFile.getParent());
			Files.write(absFile, bytes);
		} catch (Exception e) {
			return ApiResponse.error("Could not save file");
		}

		String origName = file.getOriginalFilename();
		if (origName == null || origName.isBlank()) {
			origName = storedUuid;
		}

		Document doc = Document.builder()
				.title(title.trim())
				.documentType(dt)
				.uploadedBy(live)
				.originalFileName(origName.trim())
				.storedFileName(storedUuid)
				.storedRelativePath(relative.replace("\\", "/"))
				.fileSize((long) bytes.length)
				.uploadDate(LocalDateTime.now())
				.visibleToDesignations(desigs)
				.build();
		documentRepository.save(doc);
		return ApiResponse.ok("Document uploaded.");
	}

	@Transactional
	public ApiResponse<Void> deleteByAdmin(User admin, Long id) {
		if (admin == null || !RoleCodes.isPortalAdministrator(admin)) {
			return ApiResponse.error("Only administrators can delete documents.");
		}
		Document d = documentRepository.findById(id).orElse(null);
		if (d == null) {
			return ApiResponse.error("Document not found");
		}
		deleteStoredFile(d);
		documentRepository.delete(d);
		return ApiResponse.ok("Document deleted.");
	}

	private void deleteStoredFile(Document d) {
		Path base = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
		Path p = base.resolve(d.getStoredRelativePath());
		try {
			Files.deleteIfExists(p);
			Path parent = p.getParent();
			if (parent != null && Files.isDirectory(parent)) {
				try (var stream = Files.list(parent)) {
					if (stream.findAny().isEmpty()) {
						Files.deleteIfExists(parent);
					}
				}
			}
		} catch (IOException ignored) {
			// best-effort cleanup
		}
	}

	@Transactional(readOnly = true)
	public ResponseEntity<Resource> downloadForViewer(long documentId, User viewer) {
		Document doc = documentRepository.findByIdWithVisibility(documentId).orElse(null);
		if (doc == null) {
			return ResponseEntity.notFound().build();
		}
		User live = userRepository.findById(viewer.getId()).orElse(viewer);
		if (!canView(live, doc)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		Path base = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
		Path path = base.resolve(doc.getStoredRelativePath());
		try {
			Resource resource = new UrlResource(path.toUri());
			if (!resource.exists() || !resource.isReadable()) {
				return ResponseEntity.notFound().build();
			}
			ContentDisposition disposition = ContentDisposition.inline()
					.filename(doc.getOriginalFileName(), StandardCharsets.UTF_8)
					.build();
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
					.body(resource);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private static boolean canView(User viewer, Document doc) {
		if (RoleCodes.isPortalAdministrator(viewer)) {
			return true;
		}
		if (viewer.getDesignationRef() == null) {
			return false;
		}
		Long vid = viewer.getDesignationRef().getId();
		return doc.getVisibleToDesignations().stream().anyMatch(d -> d.getId().equals(vid));
	}

	private static boolean isPdfBytes(byte[] bytes, MultipartFile file) {
		String ct = file.getContentType();
		if (ct == null || !ct.equalsIgnoreCase(MediaType.APPLICATION_PDF_VALUE)) {
			return false;
		}
		String name = file.getOriginalFilename();
		if (name == null || !name.toLowerCase().endsWith(".pdf")) {
			return false;
		}
		if (bytes.length < 5) {
			return false;
		}
		return new String(bytes, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF");
	}
}
