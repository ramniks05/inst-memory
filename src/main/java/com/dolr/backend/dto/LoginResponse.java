package com.dolr.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long id;
    private String fullName;
    private String email;
    private String department;
    private String division;
    private String designation;
    private String mobileNumber;
    private String role;

    /** Mirrors {@link com.dolr.backend.entity.User#getIsOfficer()} for post-login redirects. */
    private Boolean isOfficer;

    /** Precomputed from full {@link com.dolr.backend.entity.User} rules (FKs, roster). */
    private Boolean portalAdministrator;

    private String token;
}