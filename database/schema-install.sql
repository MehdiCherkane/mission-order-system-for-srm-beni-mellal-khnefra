-- ============================================================
-- Ordre de Mission — Safe fresh install (client)
-- Usage: mysql -u root -p < schema-install.sql
-- Safe: no DROP DATABASE, no DROP TABLE. Re-runnable.
-- Note: l'application utilise ddl-auto=update en dev et validate
-- en prod ; ce script cree le schema de base. Les employes,
-- vehicules et missions de demo sont crees par DataInitializer
-- uniquement si la table employee est vide.
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mission_db`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `mission_db`;

-- -----------------------------
-- Table: employee
-- -----------------------------
CREATE TABLE IF NOT EXISTS `employee` (
  `id` CHAR(36) NOT NULL,
  `matricule` VARCHAR(50) NOT NULL,
  `nom` VARCHAR(100) NOT NULL,
  `prenom` VARCHAR(100) NOT NULL,
  `fonction` VARCHAR(100) NULL,
  `service` VARCHAR(100) NULL,
  `direction` VARCHAR(100) NULL,
  `role` VARCHAR(30) NOT NULL,
  `email` VARCHAR(150) NULL,
  `mot_de_passe_hash` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_matricule` (`matricule`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: vehicle
-- -----------------------------
CREATE TABLE IF NOT EXISTS `vehicle` (
  `id` CHAR(36) NOT NULL,
  `matricule` VARCHAR(50) NOT NULL,
  `modele` VARCHAR(100) NULL,
  `service_rattache` VARCHAR(100) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vehicle_matricule` (`matricule`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: mission
-- -----------------------------
CREATE TABLE IF NOT EXISTS `mission` (
  `id` CHAR(36) NOT NULL,
  `requester_id` CHAR(36) NOT NULL,
  `vehicle_id` CHAR(36) NULL,
  `objet` TEXT NOT NULL,
  `destination` VARCHAR(255) NOT NULL,
  `date_depart` DATETIME NOT NULL,
  `date_retour` DATETIME NOT NULL,
  `moyen_transport` VARCHAR(50) NULL,
  `statut` VARCHAR(30) NOT NULL,
  `combinee` TINYINT(1) NOT NULL DEFAULT 0,
  `date_creation` DATETIME NOT NULL,
  `numero` INT NULL,
  `annee` INT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mission_annee_numero` (`annee`, `numero`),
  KEY `idx_mission_requester` (`requester_id`),
  KEY `idx_mission_vehicle` (`vehicle_id`),
  CONSTRAINT `fk_mission_requester`
    FOREIGN KEY (`requester_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `fk_mission_vehicle`
    FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: approval_step
-- -----------------------------
CREATE TABLE IF NOT EXISTS `approval_step` (
  `id` CHAR(36) NOT NULL,
  `mission_id` CHAR(36) NOT NULL,
  `approver_id` CHAR(36) NOT NULL,
  `ordre` INT NOT NULL,
  `statut` VARCHAR(30) NOT NULL,
  `commentaire` TEXT NULL,
  `date_action` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_approval_mission` (`mission_id`),
  KEY `idx_approval_approver` (`approver_id`),
  CONSTRAINT `fk_approval_mission`
    FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_approval_approver`
    FOREIGN KEY (`approver_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: mission_participant
-- -----------------------------
CREATE TABLE IF NOT EXISTS `mission_participant` (
  `id` CHAR(36) NOT NULL,
  `mission_id` CHAR(36) NOT NULL,
  `employee_id` CHAR(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_participant_mission` (`mission_id`),
  KEY `idx_participant_employee` (`employee_id`),
  CONSTRAINT `fk_participant_mission`
    FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_participant_employee`
    FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: system_config
-- -----------------------------
CREATE TABLE IF NOT EXISTS `system_config` (
  `config_key` VARCHAR(100) NOT NULL,
  `value` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255) NULL,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: service_approver
-- -----------------------------
CREATE TABLE IF NOT EXISTS `service_approver` (
  `id` CHAR(36) NOT NULL,
  `service_name` VARCHAR(100) NOT NULL,
  `service_key` VARCHAR(120) NOT NULL,
  `chef_employee_id` CHAR(36) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_approver_key` (`service_key`),
  KEY `idx_service_approver_chef` (`chef_employee_id`),
  CONSTRAINT `fk_service_approver_chef`
    FOREIGN KEY (`chef_employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
