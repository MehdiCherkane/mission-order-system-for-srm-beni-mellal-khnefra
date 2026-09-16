# Système de Gestion des Ordres de Mission
## Guide d'installation, d'exécution et de déploiement (FR)

**Projet :** `com.ordreDeMission:ordreDeMissison:0.0.1-SNAPSHOT`
**Stack :** Spring Boot 4.1.0 – Java 26 – Thymeleaf – Spring Security – Spring Data JPA – MySQL (H2 présent mais inutilisé) – JavaMail (non configuré par défaut)
**Port par défaut :** 8080 – **URL locale :** http://localhost:8080/login

---

### 1. Présentation

Application web de gestion des ordres de mission avec workflow de validation à 2 niveaux :
- **Employé :** créer une demande de mission (objet, destination, dates, moyen de transport, participants), suivre, imprimer si approuvée, profil.
- **Chef hiérarchique (`/chef`) :** valider / rejeter (ordre 1), affecter un véhicule.
- **Directeur (`/directeur`) :** validation finale (ordre 2).
- **Admin (`/admin`) :** tableau de bord, employés, missions, véhicules, paramètres (chef/directeur par défaut, mapping Service → Chef).
- Notifications e-mail asynchrones si SMTP configuré, sinon journalisées en avertissement.
- 22 pages Thymeleaf + CSP stricte, sessions 30 min, 1 session par utilisateur.

### 2. Prérequis

| Élément | Version requise |
|---|---|
| JDK | 26 (ex. Temurin 26.0.1+) – `java -version` doit afficher 26 |
| Maven | Inclus via `mvnw` / `mvnw.cmd` (Maven 3.9.16) |
| MySQL | 8.0+ sur `localhost:3306` |
| OS | Windows 10/11, Linux, macOS |

### 3. Contenu livré

```
ordreDeMissison/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/main/resources/application.properties
├── src/main/java/com/ordreDeMission/ordreDeMissison/
│   ├── OrdreDeMissisonApplication.java
│   ├── config/ (SecurityConfig, CustomUserDetailsService, DataInitializer)
│   ├── controller/ (Login, Employee, Chef, Directeur, Admin x4)
│   ├── model/ (Employee, Mission, ApprovalStep, MissionParticipant, Vehicle, SystemConfig, ServiceApprover)
│   ├── repository/ (7 JpaRepository)
│   └── service/ (MissionService, MissionNotificationService, EmployeeService, ...)
├── src/main/resources/templates/ (login, employee/, chef/, directeur/, admin/, error/, fragments/)
├── src/main/resources/static/ (css/style.css, logo.png)
├── data/ (vide – non utilisé)
└── target/ordreDeMissison-0.0.1-SNAPSHOT.jar (~58 Mo, daté du 31/07/2026)
```

### 4. Configuration

Fichier unique : `src/main/resources/application.properties` (sans secret en dur, tout via variables d'environnement, voir `.env.example`) :

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/mission_db?...}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
app.portal.base-url=${PORTAL_BASE_URL:http://localhost:8080}
```

| Variable | Défaut | Usage |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | URL locale / `root` / vide | Accès MySQL `mission_db` |
| `PORTAL_BASE_URL` | `http://localhost:8080` | URL publique dans les e-mails |
| `JPA_DDL_AUTO` | `update` | Mettre `validate` en production |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | vide | SMTP optionnel – si vide, e-mails journalisés et ignorés |

### 4b. Base employés : aucune base SRM externe requise

L'application est autonome : les employés sont stockés dans la table locale `mission_db.employee`, gérés via `/admin/employees`. Aucune connexion LDAP, API RH ou seconde base n'existe dans le code. En production, supprimez les comptes démo et créez les vrais employés SRM via l'interface admin ou par import SQL.

### 5. Installation locale – pas à pas (Windows PowerShell)

```powershell
# 0. Vérifier Java 26
java -version

cd "C:\Users\Utilisateur\Desktop\Non Ready Projects\ordre De Missison system\ordreDeMissison"

# 1. Créer la base et les tables (sûr, sans DROP)
mysql -u root -p < "database\schema-install.sql"
# Le schéma est aussi créé automatiquement par Hibernate (ddl-auto=update).

# 2. Variables d'environnement (voir .env.example)
$env:DB_USER="root"
$env:DB_PASSWORD="<votre-mdp-mysql>"
$env:JPA_DDL_AUTO="update"
$env:PORTAL_BASE_URL="http://localhost:8080"

# 3a. Lancer en dev
.\mvnw.cmd spring-boot:run

# 3b. OU compiler + lancer le jar
.\mvnw.cmd clean package -DskipTests
java -jar target\ordreDeMissison-0.0.1-SNAPSHOT.jar

# 4. Ouvrir http://localhost:8080/login
```

Sous Linux/macOS : `./mvnw` au lieu de `.\mvnw.cmd`, `export PORTAL_BASE_URL=...`.

### 6. Comptes de démonstration (créés si table `employee` vide)

Mot de passe unique pour les 7 comptes : `password` – **à supprimer / changer en production.**

| Matricule | Mot de passe | Rôle | Accès |
|---|---|---|---|
| EMP001 / EMP002 / EMP003 | password | Employé | /employee/dashboard |
| CHEF001 / CHEF002 | password | CHEF_HIERARCHIQUE | /chef/pending |
| DIR001 | password | DIRECTEUR | /directeur/pending |
| ADMIN001 | password | ADMIN | /admin/dashboard |

5 véhicules + 6 missions de démo + config (`CHEF001`, `DIR001`, Service Eau → CHEF001, Service Électricité → CHEF002) sont aussi créés.

### 7. Déploiement en production

1. **Serveur :** JDK 26 + MySQL 8 + reverse-proxy (Nginx/Apache).
2. **Base :** `mysql -u root -p < database/schema-install.sql` + créez un utilisateur dédié (ne pas garder `root`) :
   ```sql
   CREATE USER 'mission_app'@'%' IDENTIFIED BY '<fort>';
   GRANT SELECT,INSERT,UPDATE,DELETE ON mission_db.* TO 'mission_app'@'%';
   ```
3. **Jar :** `.\mvnw.cmd clean package` → copiez `target/ordreDeMissison-0.0.1-SNAPSHOT.jar` sur le serveur (le jar livré date du 31/07, recompilez).
4. **Lancement prod (surcharges sans modifier le jar) :**
   ```bash
   java -jar ordreDeMissison-0.0.1-SNAPSHOT.jar \
     -Dspring.datasource.url='jdbc:mysql://localhost:3306/mission_db?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=false' \
     -Dspring.datasource.username='mission_app' \
     -Dspring.datasource.password='<fort>' \
     -Dapp.portal.base-url='https://missions.votre-domaine.com' \
     -Dspring.jpa.hibernate.ddl-auto='validate' \
     -Dspring.thymeleaf.cache='true'
   ```
   Ajoutez SMTP si e-mails voulus :
   ```
   -Dspring.mail.host='smtp.gmail.com' -Dspring.mail.port='587' \
   -Dspring.mail.username='***' -Dspring.mail.password='***' \
   -Dspring.mail.properties.mail.smtp.auth='true' -Dspring.mail.properties.mail.smtp.starttls.enable='true'
   ```
5. **Service systemd (exemple) :**
   ```ini
   [Unit]
   Description=Ordre de Mission
   After=mysql.service
   [Service]
   User=app
   WorkingDirectory=/opt/mission
   ExecStart=/usr/bin/java -jar /opt/mission/ordreDeMissison-0.0.1-SNAPSHOT.jar -Dspring.datasource.url=jdbc:mysql://localhost:3306/mission_db -Dspring.datasource.username=mission_app -Dspring.datasource.password=*** -Dapp.portal.base-url=https://missions.votre-domaine.com -Dspring.jpa.hibernate.ddl-auto=validate -Dspring.thymeleaf.cache=true
   Restart=always
   [Install]
   WantedBy=multi-user.target
   ```
6. **Post-déploiement :** connectez-vous en ADMIN001, changez les 7 mots de passe, supprimez les missions de démo, créez les vrais employés SRM via `/admin/employees` (aucune base externe requise), configurez `/admin/settings` (chef/directeur réels).

### 8. Dépannage

| Symptôme | Solution |
|---|---|
| `Unknown database mission_db` | `CREATE DATABASE mission_db ...` |
| `Access denied for user root` | Corrigez user/password ou passez `-Dspring.datasource.*` |
| Page `login?error` | Matricule (`EMP001`, pas e-mail) + `motDePasse` ; 1 session max – une 2ᵉ connexion expire la 1ʳᵉ (`/login?expired`) |
| `403` sur formulaire | CSRF actif – utilisez les formulaires Thymeleaf existants (token `_csrf` inclus) |
| Pas d'e-mails | Normal sans `spring.mail.*` – logs `SMTP non configuré`. Ajoutez les `-Dspring.mail.*` |
| Port 8080 occupé | Les 2 projets utilisent 8080 – lancez-en un seul à la fois ou `-Dserver.port=8081` |
| `UnsupportedClassVersionError` | Java < 26 – installez JDK 26 |

### 9. Actions recommandées avant remise client

- [ ] Définir `DB_PASSWORD`, `MAIL_PASSWORD` et changer les 7 mots de passe `password`, supprimer données démo.
- [ ] Variables d'env en prod (`DB_*`, `MAIL_*`, `JPA_DDL_AUTO=validate`, `THYMELEAF_CACHE=true`, logs INFO).
- [ ] Ajouter README, `.env.example`, Dockerfile si déploiement Docker souhaité.
- [ ] Renommer l'artefact `ordreDeMissison` → `ordreDeMission` (coquille) dans une version future.
- [ ] Recompiler le jar (celui livré est antérieur aux sources du 29/08).

*Document généré le 16/09/2026 – Spring Boot 4.1.0 / Java 26.*
