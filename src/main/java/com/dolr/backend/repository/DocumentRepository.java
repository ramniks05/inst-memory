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
			WHERE d.uploadDate >= :uploadFrom
			ORDER BY d.uploadDate DESC
			""")
	List<Document> findAllForAdminListingFrom(@Param("uploadFrom") LocalDateTime uploadFrom);

	@Query("""
			SELECT DISTINCT d FROM Document d
			JOIN FETCH d.documentType
			JOIN FETCH d.uploadedBy
			LEFT JOIN FETCH d.visibleToDesignations
			WHERE d.uploadDate < :uploadToExclusive
			ORDER BY d.uploadDate DESC
			""")
	List<Document> findAllForAdminListingBefore(@Param("uploadToExclusive") LocalDateTime uploadToExclusive);

	@Query("""
			SELECT DISTINCT d FROM Document d
			JOIN FETCH d.documentType
			JOIN FETCH d.uploadedBy
			LEFT JOIN FETCH d.visibleToDesignations
			WHERE d.uploadDate >= :uploadFrom
			AND d.uploadDate < :uploadToExclusive
			ORDER BY d.uploadDate DESC
			""")
	List<Document> findAllForAdminListingBetween(
			@Param("uploadFrom") LocalDateTime uploadFrom,
			@Param("uploadToExclusive") LocalDateTime uploadToExclusive);

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
