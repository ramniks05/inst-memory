package com.dolr.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListItemResponse {

	private Long id;
	private String title;
	private String documentTypeName;
	private String uploadedByName;
	private String visibleToDesignationsSummary;
	private LocalDateTime uploadDate;
	/** Absolute path from host root, including context path when configured (for API clients). */
	private String downloadPath;
}
