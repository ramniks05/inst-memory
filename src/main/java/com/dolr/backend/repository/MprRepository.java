package com.dolr.backend.repository;

import com.dolr.backend.entity.Mpr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MprRepository extends JpaRepository<Mpr, Long> {

    /** Officer's own records — most common view. */
    @Query(value = "SELECT m FROM Mpr m WHERE m.uploadedBy.id = :officerId",
           countQuery = "SELECT count(m) FROM Mpr m WHERE m.uploadedBy.id = :officerId")
    Page<Mpr> findByUploadedByIdPage(@Param("officerId") Long officerId, Pageable pageable);

    /** All records (admin report view). */
    @Query(value = "SELECT m FROM Mpr m",
           countQuery = "SELECT count(m) FROM Mpr m")
    Page<Mpr> findAllPage(Pageable pageable);

    // ── Filter queries (used for report generation) ──────────────────────────

    /** Filter by financial year start (e.g. 2024 for FY 2024-25). */
    @Query(value = "SELECT m FROM Mpr m WHERE m.financialYearStart = :fyStart",
           countQuery = "SELECT count(m) FROM Mpr m WHERE m.financialYearStart = :fyStart")
    Page<Mpr> findByFyStart(@Param("fyStart") Integer fyStart, Pageable pageable);

    /** Filter by report type + FY start. */
    @Query(value = "SELECT m FROM Mpr m WHERE m.reportType = :type AND m.financialYearStart = :fyStart",
           countQuery = "SELECT count(m) FROM Mpr m WHERE m.reportType = :type AND m.financialYearStart = :fyStart")
    Page<Mpr> findByTypeAndFy(@Param("type") String type,
                               @Param("fyStart") Integer fyStart,
                               Pageable pageable);

    /** Filter by type + FY + specific period (month 1-12 or quarter 1-4). */
    @Query(value = "SELECT m FROM Mpr m WHERE m.reportType = :type AND m.financialYearStart = :fyStart AND m.periodValue = :period",
           countQuery = "SELECT count(m) FROM Mpr m WHERE m.reportType = :type AND m.financialYearStart = :fyStart AND m.periodValue = :period")
    Page<Mpr> findByTypeAndFyAndPeriod(@Param("type") String type,
                                        @Param("fyStart") Integer fyStart,
                                        @Param("period") Integer period,
                                        Pageable pageable);

    /** Filter by division name. */
    @Query(value = "SELECT m FROM Mpr m WHERE m.divisionName = :division",
           countQuery = "SELECT count(m) FROM Mpr m WHERE m.divisionName = :division")
    Page<Mpr> findByDivision(@Param("division") String division, Pageable pageable);

    /** Officer-scoped filter by type + FY. */
    @Query(value = "SELECT m FROM Mpr m WHERE m.uploadedBy.id = :officerId AND m.reportType = :type AND m.financialYearStart = :fyStart",
           countQuery = "SELECT count(m) FROM Mpr m WHERE m.uploadedBy.id = :officerId AND m.reportType = :type AND m.financialYearStart = :fyStart")
    Page<Mpr> findByOfficerTypeAndFy(@Param("officerId") Long officerId,
                                      @Param("type") String type,
                                      @Param("fyStart") Integer fyStart,
                                      Pageable pageable);
}
