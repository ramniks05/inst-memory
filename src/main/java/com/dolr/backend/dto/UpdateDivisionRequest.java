package com.dolr.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDivisionRequest {

	@Size(max = 255)
	private String name;

	private Integer sortOrder;

	private Boolean active;
}
