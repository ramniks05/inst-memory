package com.dolr.backend.repository;

import com.dolr.backend.entity.Division;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DivisionRepository extends JpaRepository<Division, Long> {

	Optional<Division> findByNameIgnoreCase(String name);

	List<Division> findByActiveTrueOrderBySortOrderAscNameAsc();

	List<Division> findAllByOrderBySortOrderAscNameAsc();
}
