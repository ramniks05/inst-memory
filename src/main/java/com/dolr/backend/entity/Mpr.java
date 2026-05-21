package com.dolr.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mprs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mpr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(name = "division_name", nullable = false, length = 255)
    private String divisionName;

    @Column(nullable = false, length = 500)
    private String subject;

    /** MONTHLY, QUARTERLY, YEARLY */
    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType;

    /** Display string, e.g. "2024-25" */
    @Column(name = "financial_year", nullable = false, length = 10)
    private String financialYear;

    /**
     * Start year of the financial year as an integer (e.g. 2024 for FY 2024-25).
     * Use this for filtering and sorting — not the display string.
     */
    @Column(name = "financial_year_start")
    private Integer financialYearStart;

    /** Display string: "April", "Q1 (Apr–Jun)", or null for YEARLY. */
    @Column(name = "period_label", length = 50)
    private String periodLabel;

    /**
     * Numeric period for filtering/sorting:
     *   MONTHLY   → calendar month number (Jan=1 … Dec=12, so Apr=4, Mar=3)
     *   QUARTERLY → quarter number 1–4
     *   YEARLY    → null
     */
    @Column(name = "period_value")
    private Integer periodValue;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "stored_relative_path", nullable = false, length = 1024)
    private String storedRelativePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;
}
