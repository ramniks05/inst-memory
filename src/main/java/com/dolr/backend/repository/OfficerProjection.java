package com.dolr.backend.repository;

import org.springframework.beans.factory.annotation.Value;

public interface OfficerProjection {

    Long getId();

    String getFullName();

    String getEmail();

    String getMobileNumber();

    String getDepartment();

    String getDivision();

    String getDesignation();

    Boolean getApproved();

    @Value("#{target.reportingOfficer != null ? target.reportingOfficer.id : null}")
    Long getReportingOfficerId();
}