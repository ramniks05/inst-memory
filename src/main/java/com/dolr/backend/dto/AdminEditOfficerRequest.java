package com.dolr.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminEditOfficerRequest {

	@NotBlank
	@Size(max = 255)
	private String fullName;

	@NotBlank
	@Email
	@Size(max = 255)
	private String email;

	@NotBlank
	@Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
	private String mobileNumber;

	@Size(max = 255)
	private String department;

	@NotNull
	private Long designationId;

	private Long divisionId;

	private Long reportingOfficerId;
}
