package com.dolr.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String title;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_type_id", nullable = false)
	private DocumentType documentType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploaded_by_user_id", nullable = false)
	private User uploadedBy;

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

	@ManyToMany
	@JoinTable(
			name = "document_visible_designations",
			joinColumns = @JoinColumn(name = "document_id"),
			inverseJoinColumns = @JoinColumn(name = "designation_id")
	)
	@Builder.Default
	private Set<Designation> visibleToDesignations = new HashSet<>();
}
