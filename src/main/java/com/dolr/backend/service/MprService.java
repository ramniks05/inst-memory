package com.dolr.backend.service;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.entity.Mpr;
import com.dolr.backend.entity.User;
import com.dolr.backend.repository.MprRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MprService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB

    private final MprRepository mprRepository;

    @Value("${app.mpr.upload-dir:uploads/mprs}")
    private String uploadDir;

    @Transactional
    public ApiResponse<Void> upload(User officer, String divisionName, String subject,
            String reportType, String financialYear, String periodLabel,
            List<MultipartFile> files) {

        if (officer == null) return ApiResponse.error("Not signed in.");
        if (divisionName == null || divisionName.isBlank()) return ApiResponse.error("Division is required.");
        if (subject == null || subject.isBlank()) return ApiResponse.error("Subject is required.");
        if (reportType == null || !List.of("MONTHLY", "QUARTERLY", "YEARLY").contains(reportType))
            return ApiResponse.error("Invalid report type.");
        if (financialYear == null || financialYear.isBlank()) return ApiResponse.error("Financial year is required.");
        if (("MONTHLY".equals(reportType) || "QUARTERLY".equals(reportType))
                && (periodLabel == null || periodLabel.isBlank()))
            return ApiResponse.error("Period is required for " + reportType.toLowerCase() + " reports.");
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty))
            return ApiResponse.error("At least one file is required.");

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        try { Files.createDirectories(base); } catch (IOException e) {
            return ApiResponse.error("Could not create upload directory.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            if (file.getSize() > MAX_FILE_BYTES) return ApiResponse.error("File too large: " + file.getOriginalFilename() + " (max 50 MB).");
            String origName = file.getOriginalFilename();
            if (origName == null || origName.isBlank()) origName = "file";
            String ext = "";
            int dot = origName.lastIndexOf('.');
            if (dot >= 0) ext = origName.substring(dot);
            String storedName = UUID.randomUUID() + ext;
            String relative = Year.now().getValue() + "/" + storedName;
            Path absFile = base.resolve(relative);
            try {
                Files.createDirectories(absFile.getParent());
                file.transferTo(absFile.toFile());
            } catch (IOException e) {
                return ApiResponse.error("Could not save file: " + origName);
            }
            String fy = financialYear.trim();
            Integer fyStart = parseFyStart(fy);
            String label = "YEARLY".equals(reportType) ? null : periodLabel.trim();
            Integer pv = periodValueOf(reportType, label);

            Mpr mpr = Mpr.builder()
                    .uploadedBy(officer)
                    .divisionName(divisionName.trim())
                    .subject(subject.trim())
                    .reportType(reportType)
                    .financialYear(fy)
                    .financialYearStart(fyStart)
                    .periodLabel(label)
                    .periodValue(pv)
                    .originalFileName(origName.trim())
                    .storedFileName(storedName)
                    .storedRelativePath(relative.replace("\\", "/"))
                    .fileSize(file.getSize())
                    .uploadDate(LocalDateTime.now())
                    .build();
            mprRepository.save(mpr);
        }
        return ApiResponse.ok(null);
    }

    @Transactional(readOnly = true)
    public Page<Mpr> listByOfficerPaged(User officer, int page, int size) {
        return mprRepository.findByUploadedByIdPage(officer.getId(), pageable(page, size));
    }

    @Transactional
    public ApiResponse<Void> deleteByOwner(User officer, Long id) {
        Mpr mpr = mprRepository.findById(id).orElse(null);
        if (mpr == null) return ApiResponse.error("Record not found.");
        if (!mpr.getUploadedBy().getId().equals(officer.getId()))
            return ApiResponse.error("You can only delete your own records.");
        deleteFile(mpr);
        mprRepository.delete(mpr);
        return ApiResponse.ok(null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(User viewer, Long id) {
        Mpr mpr = mprRepository.findById(id).orElse(null);
        if (mpr == null) return ResponseEntity.notFound().build();
        if (!mpr.getUploadedBy().getId().equals(viewer.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = base.resolve(mpr.getStoredRelativePath());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();
            String origName = mpr.getOriginalFileName();
            String ct = mpr.getOriginalFileName().toLowerCase().endsWith(".pdf")
                    ? MediaType.APPLICATION_PDF_VALUE : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            ContentDisposition cd = ContentDisposition.inline()
                    .filename(origName, StandardCharsets.UTF_8).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(ct))
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void deleteFile(Mpr mpr) {
        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path p = base.resolve(mpr.getStoredRelativePath());
            Files.deleteIfExists(p);
        } catch (IOException ignored) {}
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "uploadDate"));
    }

    // ── Helpers for controller ──────────────────────────────────────────────

    public static List<String> financialYears() {
        int current = Year.now().getValue();
        // FY runs April–March; if we're in Jan–Mar the current FY started last year
        java.time.Month month = java.time.LocalDate.now().getMonth();
        int fyStart = month.getValue() < 4 ? current - 1 : current;
        List<String> years = new java.util.ArrayList<>();
        for (int y = fyStart; y >= fyStart - 5; y--) {
            years.add(y + "-" + String.format("%02d", (y + 1) % 100));
        }
        return years;
    }

    public static List<String> months() {
        return List.of("April", "May", "June", "July", "August", "September",
                "October", "November", "December", "January", "February", "March");
    }

    public static List<String> quarters() {
        return List.of("Q1 (Apr–Jun)", "Q2 (Jul–Sep)", "Q3 (Oct–Dec)", "Q4 (Jan–Mar)");
    }

    /**
     * Extracts the start year from a display string like "2024-25" → 2024.
     * Returns null if the string cannot be parsed.
     */
    public static Integer parseFyStart(String financialYear) {
        if (financialYear == null) return null;
        String[] parts = financialYear.split("-");
        try { return Integer.parseInt(parts[0].trim()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * Returns the numeric period value used for filtering:
     *   MONTHLY   → calendar month number (April=4 … March=3)
     *   QUARTERLY → 1–4 (parsed from "Q1 …", "Q2 …" etc.)
     *   YEARLY    → null
     */
    public static Integer periodValueOf(String reportType, String periodLabel) {
        if (periodLabel == null || "YEARLY".equals(reportType)) return null;
        if ("MONTHLY".equals(reportType)) {
            return switch (periodLabel.trim()) {
                case "January"   -> 1;  case "February"  -> 2;
                case "March"     -> 3;  case "April"     -> 4;
                case "May"       -> 5;  case "June"      -> 6;
                case "July"      -> 7;  case "August"    -> 8;
                case "September" -> 9;  case "October"   -> 10;
                case "November"  -> 11; case "December"  -> 12;
                default -> null;
            };
        }
        if ("QUARTERLY".equals(reportType) && periodLabel.startsWith("Q")) {
            try { return Integer.parseInt(String.valueOf(periodLabel.charAt(1))); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
