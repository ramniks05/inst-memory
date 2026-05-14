package com.dolr.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDesignationRequest {

	@Size(max = 255)
	private String name;

	private Boolean handlesAllDivisions;

	private Integer sortOrder;

	private Boolean active;
}
