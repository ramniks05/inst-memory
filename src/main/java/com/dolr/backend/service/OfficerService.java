package com.dolr.backend.service;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.ChangePasswordRequest;
import com.dolr.backend.dto.UpdateOfficerRequest;
import com.dolr.backend.entity.Designation;
import com.dolr.backend.entity.Division;
import com.dolr.backend.entity.User;
import com.dolr.backend.repository.DesignationRepository;
import com.dolr.backend.repository.DivisionRepository;
import com.dolr.backend.repository.OfficerProjection;
import com.dolr.backend.repository.UserRepository;
import com.dolr.backend.security.RoleCodes;
import com.dolr.backend.util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfficerService {

	private final UserRepository userRepository;
	private final DivisionRepository divisionRepository;
	private final DesignationRepository designationRepository;
	private final PasswordEncoder passwordEncoder;

	public List<OfficerProjection> getAllOfficers() {
		return userRepository.findByIsOfficerTrueAndApprovedTrue();
	}

	/** Approved officer accounts the admin UI may edit or retire. */
	@Transactional(readOnly = true)
	public Optional<User> findEditableOfficerForAdmin(long id) {
		Optional<User> o = userRepository.findById(id).filter(u ->
				Boolean.TRUE.equals(u.getIsOfficer()) && Boolean.TRUE.equals(u.getApproved()));
		o.ifPresent(u -> {
			if (u.getDesignationRef() != null) {
				u.getDesignationRef().getId();
			}
			if (u.getDivisionRef() != null) {
				u.getDivisionRef().getId();
			}
			if (u.getReportingOfficer() != null) {
				u.getReportingOfficer().getId();
			}
		});
		return o;
	}

	public List<OfficerProjection> getOfficersByDivision(String department, String division) {
		List<OfficerProjection> officers =
				userRepository.findOfficersByDivision(department, division);

		if (officers.isEmpty()) {
			return userRepository.findByRole("ADMIN");
		}

		return officers;
	}

	@Transactional
	public ApiResponse<Void> updateOfficer(Long id, UpdateOfficerRequest request) {

		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Officer not found"));

		String emailNorm = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
		userRepository.findByEmailIgnoreCase(emailNorm)
				.ifPresent(existing -> {
					if (!existing.getId().equals(id)) {
						throw new RuntimeException("Email already exists");
					}
				});

		String mobileDigits = PhoneNumbers.digitsOnly(request.getMobileNumber());
		if (mobileDigits.length() != 10) {
			throw new RuntimeException("Mobile number must be exactly 10 digits");
		}

		user.setFullName(request.getFullName());
		user.setEmail(emailNorm);
		user.setMobileNumber(mobileDigits);
		user.setDepartment(request.getDepartment());

		if (request.getDivision() != null) {
			user.setDivision(request.getDivision());
		}
		if (request.getDesignation() != null) {
			user.setDesignation(request.getDesignation());
		}

		if (request.getDesignationId() != null) {
			Designation des = designationRepository.findById(request.getDesignationId())
					.orElseThrow(() -> new RuntimeException("Designation not found"));
			user.setDesignationRef(des);
			user.setDesignation(des.getName());
			if (Boolean.TRUE.equals(des.getHandlesAllDivisions())) {
				user.setDivisionRef(null);
				user.setDivision("All divisions");
			} else {
				if (user.getDivisionRef() == null && request.getDivisionId() == null) {
					throw new RuntimeException(
							"This designation is tied to a single division: set divisionId (or assign a division first).");
				}
			}
		}

		if (request.getDivisionId() != null) {
			Designation eff = user.getDesignationRef();
			if (eff != null && Boolean.TRUE.equals(eff.getHandlesAllDivisions())) {
				throw new RuntimeException(
						"Cannot assign a division: the current designation applies to all divisions.");
			}
			Division div = divisionRepository.findById(request.getDivisionId())
					.orElseThrow(() -> new RuntimeException("Division not found"));
			user.setDivisionRef(div);
			user.setDivision(div.getName());
		}

		if (request.getRole() != null) {
			String r = request.getRole().trim();
			if (RoleCodes.isAdmin(r)) {
				user.setRole(RoleCodes.ADMIN);
			} else if (RoleCodes.OFFICER.equalsIgnoreCase(r)) {
				user.setRole(RoleCodes.OFFICER);
			} else {
				throw new RuntimeException("Role must be ADMIN or OFFICER");
			}
		}

		if (request.getReportingOfficerId() != null) {
			User reportingOfficer = userRepository.findById(request.getReportingOfficerId())
					.orElseThrow(() -> new RuntimeException("Reporting officer not found"));
			user.setReportingOfficer(reportingOfficer);
		} else {
			user.setReportingOfficer(null);
		}

		userRepository.save(user);

		return ApiResponse.ok("Officer updated successfully");
	}

	@Transactional
	public ApiResponse<Void> deleteOfficer(Long id) {

		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Officer not found"));

		user.setApproved(false);
		user.setIsOfficer(false);

		userRepository.save(user);

		return ApiResponse.ok("Officer removed successfully");
	}

	@Transactional
	public ApiResponse<Void> changePassword(ChangePasswordRequest request) {

		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
			throw new RuntimeException("Old password is incorrect");
		}

		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new RuntimeException("New password and confirm password do not match");
		}

		if (request.getNewPassword().length() < 6) {
			throw new RuntimeException("Password must be at least 6 characters");
		}

		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
			throw new RuntimeException("New password cannot be same as old password");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));

		userRepository.save(user);

		return ApiResponse.ok("Password changed successfully");
	}
}
