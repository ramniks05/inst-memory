package com.dolr.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDesignationRequest {

	@NotBlank
	@Size(max = 255)
	private String name;

	/** When true (e.g. Secretary), the officer is not tied to a single division. */
	private Boolean handlesAllDivisions;

	private Integer sortOrder;
}
