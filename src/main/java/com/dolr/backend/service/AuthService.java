package com.dolr.backend.service;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.AdminCreateOfficerRequest;
import com.dolr.backend.dto.LoginRequest;
import com.dolr.backend.dto.LoginResponse;
import com.dolr.backend.entity.Designation;
import com.dolr.backend.entity.Division;
import com.dolr.backend.entity.User;
import com.dolr.backend.repository.DesignationRepository;
import com.dolr.backend.repository.DivisionRepository;
import com.dolr.backend.repository.OfficerProjection;
import com.dolr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dolr.backend.security.JwtService;
import com.dolr.backend.security.RoleCodes;
import com.dolr.backend.util.PhoneNumbers;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final DesignationRepository designationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // LOGIN
    @Transactional(readOnly = true)
    public ApiResponse<LoginResponse> login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        LoginResponse loginResponse = LoginResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .department(user.getDepartment())
                .division(user.getDivision())
                .designation(user.getDesignation())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .isOfficer(user.getIsOfficer())
                .portalAdministrator(RoleCodes.isPortalAdministrator(user))
                .token(token)
                .build();

        return ApiResponse.ok(loginResponse);
    }

    @Transactional
    public ApiResponse<Void> createOfficerByAdmin(User admin, AdminCreateOfficerRequest req) {
        if (admin == null || !RoleCodes.isPortalAdministrator(admin)) {
            return ApiResponse.error("Only administrators can add officers.");
        }
        String email = normalizeEmail(req.getEmail());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return ApiResponse.error("Email already exists");
        }
        String mobileDigits = PhoneNumbers.digitsOnly(req.getMobileNumber());
        if (mobileDigits.length() != 10) {
            return ApiResponse.error("Mobile number must be exactly 10 digits");
        }
        Designation des = designationRepository.findById(req.getDesignationId()).orElse(null);
        if (des == null) {
            return ApiResponse.error("Designation not found");
        }
        Division divEntity = null;
        if (!Boolean.TRUE.equals(des.getHandlesAllDivisions())) {
            if (req.getDivisionId() == null) {
                return ApiResponse.error("Division is required for this designation");
            }
            divEntity = divisionRepository.findById(req.getDivisionId()).orElse(null);
            if (divEntity == null) {
                return ApiResponse.error("Division not found");
            }
        }
        User reporting = null;
        if (req.getReportingOfficerId() != null) {
            reporting = userRepository.findById(req.getReportingOfficerId()).orElse(null);
            if (reporting == null) {
                return ApiResponse.error("Reporting officer not found");
            }
        }
        String dept = req.getDepartment() != null && !req.getDepartment().isBlank()
                ? req.getDepartment().trim()
                : "—";

        User.UserBuilder b = User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .mobileNumber(mobileDigits)
                .department(dept)
                .password(passwordEncoder.encode(req.getPassword()))
                .role("OFFICER")
                .approved(true)
                .isOfficer(true)
                .designationRef(des)
                .designation(des.getName())
                .reportingOfficer(reporting);

        if (Boolean.TRUE.equals(des.getHandlesAllDivisions())) {
            b.divisionRef(null).division("All divisions");
        } else {
            b.divisionRef(divEntity).division(divEntity.getName());
        }

        userRepository.save(b.build());
        return ApiResponse.ok("Officer account created.");
    }

    @Transactional
    public ApiResponse<Void> changePassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        if (user == null) {
            return ApiResponse.error("Not signed in.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ApiResponse.error("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            return ApiResponse.error("New password must be at least 8 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return ApiResponse.error("New passwords do not match.");
        }
        if (currentPassword.equals(newPassword)) {
            return ApiResponse.error("New password must differ from the current password.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ApiResponse.ok(null);
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    // REPORTING OFFICERS DROPDOWN
    public List<OfficerProjection> getReportingOfficers(String department, String division) {

        List<OfficerProjection> officers =
                userRepository.findByDepartmentAndDivisionAndApprovedTrueAndIsOfficerTrue(
                        department, division
                );

        // fallback to administrators when no officers in scope
        if (officers.isEmpty()) {
            return userRepository.findByRole("ADMIN");
        }

        return officers;
    }

}