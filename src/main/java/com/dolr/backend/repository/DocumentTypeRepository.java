package com.dolr.backend.repository;

import com.dolr.backend.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

	List<DocumentType> findByActiveTrueOrderBySortOrderAscNameAsc();

	List<DocumentType> findAllByOrderBySortOrderAscNameAsc();

	Optional<DocumentType> findByNameIgnoreCase(String name);
}
