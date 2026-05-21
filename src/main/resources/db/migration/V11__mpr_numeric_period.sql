-- Add numeric period columns to mprs for reliable filtering and sorting.
--
-- financial_year_start : integer start year of the FY (e.g. 2024 for FY 2024-25).
--                        Sortable and range-queryable unlike the "2024-25" string.
--
-- period_value         : for MONTHLY -> calendar month number (April=4 … March=3)
--                        for QUARTERLY -> quarter number 1-4 (Q1=1 … Q4=4)
--                        for YEARLY  -> NULL
--
-- period_label and financial_year are kept as-is for display only.

ALTER TABLE mprs
    ADD COLUMN IF NOT EXISTS financial_year_start SMALLINT,
    ADD COLUMN IF NOT EXISTS period_value         SMALLINT;

-- Back-fill existing rows from the display strings (safe for any existing test data).
UPDATE mprs SET financial_year_start = CAST(SPLIT_PART(financial_year, '-', 1) AS SMALLINT)
WHERE financial_year_start IS NULL AND financial_year ~ '^[0-9]{4}-';

-- Back-fill period_value for MONTHLY rows from period_label text.
UPDATE mprs SET period_value = CASE period_label
    WHEN 'April'     THEN 4  WHEN 'May'       THEN 5
    WHEN 'June'      THEN 6  WHEN 'July'      THEN 7
    WHEN 'August'    THEN 8  WHEN 'September' THEN 9
    WHEN 'October'   THEN 10 WHEN 'November'  THEN 11
    WHEN 'December'  THEN 12 WHEN 'January'   THEN 1
    WHEN 'February'  THEN 2  WHEN 'March'     THEN 3
    ELSE NULL END
WHERE report_type = 'MONTHLY' AND period_value IS NULL;

-- Back-fill period_value for QUARTERLY rows.
UPDATE mprs SET period_value = CASE
    WHEN period_label LIKE 'Q1%' THEN 1
    WHEN period_label LIKE 'Q2%' THEN 2
    WHEN period_label LIKE 'Q3%' THEN 3
    WHEN period_label LIKE 'Q4%' THEN 4
    ELSE NULL END
WHERE report_type = 'QUARTERLY' AND period_value IS NULL;

-- Composite index for the most common filter: FY + type + period
CREATE INDEX IF NOT EXISTS idx_mprs_fy_type_period
    ON mprs (financial_year_start, report_type, period_value);

CREATE INDEX IF NOT EXISTS idx_mprs_division
    ON mprs (division_name);
