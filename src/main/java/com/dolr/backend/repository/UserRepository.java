package com.dolr.backend.repository;

import com.dolr.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ── Auth ──────────────────────────────────────────────

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByRole(String role);

    List<OfficerProjection> findByDepartmentAndDivisionAndApprovedTrueAndIsOfficerTrue(
            String department, String division
    );

    List<OfficerProjection> findByRole(String role);

    // ── OfficerService ────────────────────────────────────

    // All approved officers for table
    List<OfficerProjection> findByIsOfficerTrueAndApprovedTrue();

    // Officers by division for dropdown
    @Query("""
        SELECT u FROM User u
        WHERE u.department = :department
          AND u.division   = :division
          AND u.isOfficer  = true
          AND u.approved   = true
    """)
    List<OfficerProjection> findOfficersByDivision(
            @Param("department") String department,
            @Param("division")   String division
    );
}