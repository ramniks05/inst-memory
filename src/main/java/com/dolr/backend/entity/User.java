package com.dolr.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String mobileNumber;

    private String department;
    private String division;
    private String designation;

    private String password;

    /** Role code; see {@code roles} table (Flyway). Values: {@code OFFICER} (departmental), {@code ADMIN}. */
    private String role;

    private Boolean approved = false;

    private Boolean isOfficer = false;

    @ManyToOne
    @JoinColumn(name = "reporting_officer_id")
    private User reportingOfficer;

    /** Optional master division (sync {@link #division} text when assigned). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_fk_id")
    private Division divisionRef;

    /** Optional master designation (Secretary = all divisions; others usually need {@link #divisionRef}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_fk_id")
    private Designation designationRef;
}