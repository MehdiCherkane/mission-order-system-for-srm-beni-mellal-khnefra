# Système de Gestion des Ordres de Mission

Application web de gestion des ordres de mission avec workflow de validation à deux niveaux (chef hiérarchique puis directeur).

## Fonctionnalités

**Employé :**
- Création d'une demande de mission (objet, destination, dates, moyen de transport, participants)
- Suivi des demandes et impression si approuvée
- Consultation du profil

**Chef hiérarchique :**
- Validation ou rejet des demandes (première étape)
- Affectation d'un véhicule

**Directeur :**
- Validation finale (deuxième étape)

**Administrateur :**
- Tableau de bord, gestion des employés, des missions et des véhicules
- Paramètres : chef et directeur par défaut, mapping Service vers Chef
- Notifications e-mail asynchrones si SMTP configuré

## Stack technique

- Spring Boot 4.1.0 – Java 26
- Thymeleaf – Spring Security – Spring Data JPA
- MySQL / MariaDB
- Maven Wrapper inclus (`mvnw` / `mvnw.cmd`)

## Prérequis

- JDK 26
- MySQL 8.0+ sur `localhost:3306`

## Installation rapide (Windows)

```powershell
cd "ordreDeMissison"

# 1. Créer la base et les tables (sûr, sans DROP)
mysql -u root -p < "database\schema-install.sql"
# Le schéma est aussi créé automatiquement par Hibernate (ddl-auto=update).
# Les employés, véhicules et missions de démo sont créés par DataInitializer
# uniquement si la table employee est vide.

# 2. URL publique (optionnel)
$env:PORTAL_BASE_URL="http://localhost:8080"

# 3. Lancer l'application
.\mvnw.cmd spring-boot:run
```

Sous Linux ou macOS, utiliser `./mvnw` et `export PORTAL_BASE_URL="..."`.

Alternative : compiler puis exécuter le jar :

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\ordreDeMissison-0.0.1-SNAPSHOT.jar
```

Ouvrir ensuite http://localhost:8080/login

## Configuration

Fichier unique : `src/main/resources/application.properties` (sans secret en dur, voir `.env.example`).

| Paramètre | Variable d'environnement | Description |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | Base `mission_db` |
| `spring.datasource.username` | `DB_USER` | Utilisateur MySQL |
| `spring.datasource.password` | `DB_PASSWORD` (vide par défaut) | Mot de passe MySQL |
| `spring.jpa.hibernate.ddl-auto` | `JPA_DDL_AUTO` (`update`) | Passer à `validate` en production |
| `app.portal.base-url` | `PORTAL_BASE_URL` | URL publique dans les e-mails |
| `spring.mail.host/user/password` | `MAIL_HOST/USERNAME/PASSWORD` (vides) | SMTP optionnel |

SMTP vide par défaut : les e-mails sont journalisés et ignorés. Pour les activer, définir `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

## Base employés : aucune base SRM externe requise

Application autonome : les employés sont stockés dans la table locale `mission_db.employee`, gérés via `/admin/employees`. Aucune connexion LDAP, API RH ou seconde base. En production, supprimez les comptes démo et créez les vrais employés SRM via l'interface admin ou par import SQL.

## Comptes de démonstration

Créés automatiquement si la table `employee` est vide. Mot de passe commun : `password`. À changer ou supprimer en production.

| Matricule | Rôle | Accès |
|---|---|---|
| EMP001, EMP002, EMP003 | Employé | /employee/dashboard |
| CHEF001, CHEF002 | Chef hiérarchique | /chef/pending |
| DIR001 | Directeur | /directeur/pending |
| ADMIN001 | Administrateur | /admin/dashboard |

Des véhicules, missions de démo et paramètres par défaut sont également créés.

## Déploiement en production

1. Serveur avec JDK 26 et MySQL 8.
2. Créer la base `mission_db` et un utilisateur dédié (ne pas conserver `root`).
3. Recompiler : `.\mvnw.cmd clean package`, puis copier `target/ordreDeMissison-0.0.1-SNAPSHOT.jar` sur le serveur.
4. Lancer avec surcharges (sans modifier le jar) :
```bash
java -jar ordreDeMissison-0.0.1-SNAPSHOT.jar \
  -Dspring.datasource.url='jdbc:mysql://localhost:3306/mission_db?useSSL=true&serverTimezone=UTC' \
  -Dspring.datasource.username='mission_app' \
  -Dspring.datasource.password='<mot-de-passe-fort>' \
  -Dapp.portal.base-url='https://missions.votre-domaine.com' \
  -Dspring.jpa.hibernate.ddl-auto='validate' \
  -Dspring.thymeleaf.cache='true'
```
5. Exposer via reverse-proxy TLS, configurer `/admin/settings` avec les chefs et directeurs réels, changer les 7 mots de passe et supprimer les données de démo.

## Documentation détaillée

- `GUIDE_EXECUTION_DEPLOIEMENT_FR.md` : guide complet en français

## Remarques de sécurité

- Définir `DB_PASSWORD`, `MAIL_PASSWORD` et changer les 7 mots de passe `password` avant remise au client.
- Externaliser les accès base et SMTP, passer `ddl-auto` à `validate` et réactiver le cache Thymeleaf en production.
- Sauvegarder régulièrement la base MySQL.
