package com.dolr.backend.repository;

import com.dolr.backend.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {

	Optional<Designation> findByNameIgnoreCase(String name);

	List<Designation> findByActiveTrueOrderBySortOrderAscNameAsc();

	List<Designation> findAllByOrderBySortOrderAscNameAsc();
}
