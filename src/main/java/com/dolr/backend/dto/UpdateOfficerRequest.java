package com.dolr.backend.dto;

import lombok.Data;

@Data
public class UpdateOfficerRequest {

    private String fullName;

    private String email;

    private String mobileNumber;

    private String department;

    private String division;

    private String designation;

    private Long divisionId;

    private Long designationId;

    private String role;

    private Long reportingOfficerId;
}