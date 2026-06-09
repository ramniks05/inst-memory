package com.dolr.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MprServiceTest {

	@Test
	void parseFyStart_extractsStartYear() {
		assertEquals(2024, MprService.parseFyStart("2024-25"));
		assertNull(MprService.parseFyStart("invalid"));
	}

	@Test
	void periodValueOf_monthlyQuarterlyAndYearly() {
		assertEquals(4, MprService.periodValueOf("MONTHLY", "April"));
		assertEquals(1, MprService.periodValueOf("MONTHLY", "January"));
		assertEquals(2, MprService.periodValueOf("QUARTERLY", "Q2 (Jul–Sep)"));
		assertNull(MprService.periodValueOf("YEARLY", null));
		assertNull(MprService.periodValueOf("MONTHLY", "NotAMonth"));
	}

	@Test
	void currentFinancialYear_isConsistentWithStartYear() {
		String fy = MprService.currentFinancialYear();
		assertNotNull(fy);
		assertTrue(fy.matches("\\d{4}-\\d{2}"));
		assertEquals(MprService.currentFinancialYearStart(), MprService.parseFyStart(fy));
	}
}
