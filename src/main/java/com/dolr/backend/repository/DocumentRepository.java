package com.dolr.backend.repository;

import com.dolr.backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	@Query("""
			SELECT DISTINCT d FROM Document d
			JOIN FETCH d.visibleToDesignations
			WHERE d.id = :id
			""")
	Optional<Document> findByIdWithVisibility(@Param("id") Long id);

	@Query("""
			SELECT DISTINCT d FROM Document d
			JOIN FETCH d.documentType
			JOIN FETCH d.uploadedBy
			LEFT JOIN FETCH d.visibleToDesignations
			ORDER BY d.uploadDate DESC
			""")
	List<Document> findAllForAdminListing();

	@Query("""
			SELECT DISTINCT d FROM Document d
			JOIN FETCH d.documentType
			JOIN FETCH d.uploadedBy
			LEFT JOIN FETCH d.visibleToDesignations
			WHERE (:from IS NULL OR d.uploadDate >= :from)
			AND (:toExclusive IS NULL OR d.uploadDate < :toExclusive)
			ORDER BY d.uploadDate DESC
			""")
	List<Document> findAllForAdminListingFiltered(
			@Param("from") LocalDateTime from,
			@Param("toExclusive") LocalDateTime toExclusive);

	@Query("""
			SELECT DISTINCT d FROM Document d
			JOIN FETCH d.documentType
			JOIN FETCH d.uploadedBy
			JOIN d.visibleToDesignations v
			WHERE v.id = :designationId
			ORDER BY d.uploadDate DESC
			""")
	List<Document> findVisibleForDesignation(@Param("designationId") Long designationId);
}
