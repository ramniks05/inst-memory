package com.dolr.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTablePage {

	private List<DocumentListItemResponse> content;
	private int page;
	private int totalPages;
	private long totalElements;
	private int size;
	private boolean hasPrevious;
	private boolean hasNext;

	public boolean isEmpty() {
		return content == null || content.isEmpty();
	}

	public int getFromIndex() {
		if (totalElements == 0) {
			return 0;
		}
		return page * size + 1;
	}

	public int getToIndex() {
		if (totalElements == 0) {
			return 0;
		}
		return (int) Math.min(totalElements, (long) (page + 1) * size);
	}
}
