-- ============================================================
-- Ordre de Mission — Backfill sequential numbers (one-time)
-- Usage: mysql -u root -p mission_db < migrate-numero.sql
-- Safe: only touches rows where numero IS NULL.
-- Numbers missions 1..N per creation year, oldest first.
-- Run AFTER starting the app once with the new code
-- (Hibernate ddl-auto=update creates the columns first).
-- Requires MySQL 8 (window functions).
-- ============================================================

UPDATE `mission` m
JOIN (
  SELECT `id`,
         ROW_NUMBER() OVER (
           PARTITION BY YEAR(`date_creation`)
           ORDER BY `date_creation`, `id`
         ) AS rn,
         YEAR(`date_creation`) AS y
  FROM `mission`
  WHERE `numero` IS NULL
) t ON t.`id` = m.`id`
SET m.`annee` = t.y,
    m.`numero` = t.rn;
