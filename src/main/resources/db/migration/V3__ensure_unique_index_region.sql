-- V3: No-op migration (redundant with V2's unique constraint)
-- V2 already creates unique constraint 'uk_region_admin_triple' which provides a backing unique index
-- This migration is kept for version continuity but performs no operations
SELECT 1;


