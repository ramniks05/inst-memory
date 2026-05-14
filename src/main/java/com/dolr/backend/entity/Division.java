package com.dolr.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "divisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Division {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String name;

	@Column(name = "sort_order", nullable = false)
	@Builder.Default
	private Integer sortOrder = 0;

	@Column(nullable = false)
	@Builder.Default
	private Boolean active = true;
}
