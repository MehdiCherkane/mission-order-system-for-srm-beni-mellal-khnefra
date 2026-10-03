# Ordre de Mission System — Explained Like You Have To Teach It

> Read this once, and you can explain the project to anyone: a new developer, a boss, or in an interview.
> Language kept simple on purpose. No buzzwords.

---

## 1. What is this project in one sentence?

It is a **web app for asking permission to go on a work trip**.

An employee says: "I need to go to Beni Mellal on July 30 to fix a water leak."
His boss (chef) says yes or no.
If yes, the big boss (director) says yes or no.
If both say yes, the employee can print the paper and go.

That paper is called an **"Ordre de Mission"**.

Real example from the demo data (`config/DataInitializer.java:84-89`):

- Ahmed Benali (EMP001, Service Eau) wants to repair a leak in Beni Mellal.
- Karim Idrissi (CHEF001) approves first.
- Nadia El Amrani (DIR001) approves second.
- Status becomes `approuvee` = printable.

---

## 2. Who uses it? The 4 roles

| Role | Login example | What they see | What they can do |
|------|---------------|---------------|------------------|
| Employee | EMP001 / password | `/employee/dashboard` | Create request, follow status, print if approved, see profile |
| Chef hiérarchique | CHEF001 / password | `/chef/pending` | Approve / reject step 1, assign a vehicle |
| Directeur | DIR001 / password | `/directeur/pending` | Final approve / reject step 2 |
| Admin | ADMIN001 / password | `/admin/dashboard` | See stats, manage employees, missions, vehicles, settings |

All demo passwords are `password`. You must change them in production.

Think of it like school:

- Employee = student who asks for a day off
- Chef = teacher who signs first
- Directeur = principal who signs last
- Admin = secretary who manages lists and rules

---

## 3. Big picture: how the pieces fit

```
Browser (Thymeleaf pages)
   |
   | login with matricule + password
   v
Spring Security (checks who you are, where you can go)
   |
   v
Controller (EmployeeController, ChefController, DirecteurController, Admin*Controller)
   |
   v
Service (MissionService = the brain, + Notification, Config, Approver services)
   |
   v
Repository (Spring Data JPA - simple save/find, no SQL written by hand)
   |
   v
MySQL database `mission_db` (7 tables)
```

There is **no frontend framework**. Pages are generated on the server with Thymeleaf (`src/main/resources/templates/` - 22 pages). CSS is one file (`static/css/style.css`). This is normal for an internal admin tool: simple, fast to build, no API needed.

To run it:

1. Create DB: `mysql -u root -p < database/schema-install.sql`
2. Run: `.\mvnw.cmd spring-boot:run`
3. Open: `http://localhost:8080/login`

Config lives in one file: `src/main/resources/application.properties`. Secrets are NOT in the file, they come from environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `MAIL_HOST`, etc. See `.env.example`).

---

## 4. Tech stack — what and why

| Tech | Version | Why this choice? Simple reason |
|------|---------|-------------------------------|
| Java | 26 | Company / school standard. Long-term, stable. |
| Spring Boot | 4.1.0 | Does login, web pages, DB connection for you. You write business rules only. |
| Spring Data JPA + Hibernate | included | You work with Java objects (`Mission`, `Employee`), it writes SQL for you. Fewer mistakes. |
| Spring Security | included | Login, passwords, "chef cannot open /admin" rules. Hard to do safely by hand. |
| Thymeleaf | included | Server pages. Good for forms + print page. No need for React for this use case. |
| MySQL 8 | 8.0+ | Free, well known, keeps data even after restart, handles many users at once, enforces links between tables. See section 5. |
| H2 | in `pom.xml` but unused | H2 is a toy in-memory DB for tests. It is declared but the app really uses MySQL (`spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver`). |
| Spring Mail | included | Sends emails. Optional - app works without it. |
| Maven Wrapper `mvnw` | 3.9.16 | Everyone builds with the same Maven version, no install needed. |

---

## 5. The database — deep dive

### 5.1 Why MySQL and not something else?

Simple answer:

- You need **permanent shared data**. If the server restarts, missions must still be there. So file-only or in-memory (H2 alone, SQLite) is not enough for production.
- You need **relations**: a mission belongs to an employee, has 2 approval steps, can have participants and a vehicle. Relational DB enforces this with foreign keys. A junior cannot create an approval for a mission that does not exist - MySQL blocks it.
- You need **many people at once**: chef + directeur + employees working together. MySQL + InnoDB handles locks and transactions well.
- Team knows it: cheap hosting, phpMyAdmin, backups with `mysqldump`.

Why not PostgreSQL or MongoDB?

- Postgres would also work fine. MySQL was chosen because the client / server already has MySQL and the team knows it. No technical blocker.
- MongoDB (NoSQL) would be a bad fit: you want strict rules like "every mission must have a requester" and "status can only be X". Mongo lets you store anything, so bad data slips in.

In short: **MySQL = safe, shared, permanent, strict.**

Config proof (`application.properties:8-11`):

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/mission_db?...}
spring.datasource.username=${DB_USER:root}
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
```

- `ddl-auto=update` in dev = Hibernate creates/updates tables automatically. Easy for dev.
- In production you switch to `validate` = Hibernate only checks tables exist, never changes them. Safer. The SQL file `database/schema-install.sql` is the truth in prod.

### 5.2 The 7 tables — what each one is for

Think of 7 Excel sheets linked together.

**1. `employee` — who works here**

```sql
id CHAR(36), matricule VARCHAR(50) UNIQUE, nom, prenom,
fonction, service, direction, role, email, mot_de_passe_hash
```

Example row:

| matricule | nom | prenom | service | role |
|-----------|-----|--------|---------|------|
| EMP001 | Benali | Ahmed | Service Eau | EMPLOYEE |
| CHEF001 | Idrissi | Karim | Service Technique | CHEF_HIERARCHIQUE |
| DIR001 | El Amrani | Nadia | Direction Provinciale | DIRECTEUR |

- `matricule` is the login name, not email. Unique.
- `role` is plain text: `EMPLOYEE`, `CHEF_HIERARCHIQUE`, `DIRECTEUR`, `ADMIN`. Security checks this string.
- Password is never stored clear, only hash (`mot_de_passe_hash`).

Java: `model/Employee.java`

**2. `vehicle` — company cars**

```sql
id, matricule UNIQUE, modele, service_rattache
```

Example: `1234-A-5678 - Dacia Logan - Service Technique`

Simple list, managed by admin.

**3. `mission` — the request itself**

```sql
id, requester_id -> employee.id, vehicle_id -> vehicle.id (nullable),
objet TEXT, destination, date_depart, date_retour,
moyen_transport, statut, combinee, date_creation
```

`statut` values for mission:

- `en_attente_chef` = waiting for chef
- `en_attente_directeur` = chef said yes, waiting for director
- `approuvee` = both said yes, printable
- `rejetee` = someone said no, stopped
- `soumise` = just created, before routing (short moment)

`moyen_transport` = `vehicule_de_service` or `vehicule_personnel`
`combinee` = true/false = "is this a group mission with other participants?"

Java: `model/Mission.java`

**4. `approval_step` — the two signatures**

```sql
id, mission_id -> mission.id ON DELETE CASCADE,
approver_id -> employee.id, ordre INT (1 or 2),
statut, commentaire, date_action
```

`statut` values for steps:

- `en_attente` = not yet decided
- `approuve` = yes
- `rejete` = no
- `annulee` = cancelled because earlier step said no

Example for Ahmed's mission m2 (fully approved):

| ordre | approver | statut |
|-------|----------|--------|
| 1 | CHEF001 | approuve |
| 2 | DIR001 | approuve |

If chef rejects, step 2 becomes `annulee` automatically. See workflow.

Java: `model/ApprovalStep.java`

**5. `mission_participant` — extra people on a group trip**

```sql
id, mission_id -> mission.id ON DELETE CASCADE, employee_id -> employee.id
```

Example: mission m4 to Azilal has participant Youssef (EMP003). Combined mission = true.

Java: `model/MissionParticipant.java`

**6. `system_config` — global settings (key-value)**

```sql
config_key PK, value, description
```

Only 2 rows that matter:

| key | value | meaning |
|-----|-------|---------|
| `default_chef_matricule` | CHEF001 | fallback chef if service has no mapping |
| `default_directeur_matricule` | DIR001 | who is the final boss |

Admin edits them at `/admin/settings`. Code reads them in `SystemConfigService.java`.

**7. `service_approver` — which chef for which service?**

```sql
id, service_name, service_key UNIQUE, chef_employee_id -> employee.id
```

Example:

| service_name | service_key | chef |
|--------------|-------------|------|
| Service Eau | service eau | CHEF001 |
| Service Électricité | service électricité | CHEF002 |

`service_key` = lower-case trimmed name. This makes lookup case-insensitive: "Service EAU", "service eau" both work.

Java: `model/ServiceApprover.java`, logic: `service/ServiceApproverService.java`

### 5.3 How tables link — draw this on a board

```
employee 1 ----< mission (requester_id)
vehicle  1 ----< mission (vehicle_id, optional)
mission  1 ----< approval_step (2 rows per mission)
mission  1 ----< mission_participant (0..N rows)
employee 1 ----< approval_step (approver_id)
employee 1 ----< mission_participant (employee_id)
employee 1 ----< service_approver (chef_employee_id)
```

Rules enforced by MySQL:

- You cannot delete an employee if missions point to him (foreign key blocks, except approval cascade).
- If you delete a mission, its steps + participants auto-delete (`ON DELETE CASCADE`). No orphan rows.
- `matricule` unique, `service_key` unique: no duplicates.
- Indexes (`idx_mission_requester`, etc.) make "show my missions" fast.

IDs are UUIDs (`CHAR(36)` like `a3f1c...`). Why not 1,2,3?

- Harder to guess: `/mission/5` is easy to try, `/mission/a3f1c9d2-...` is not.
- Safe if you merge two databases later, no collision.

---

## 6. The workflow — step by step with a real story

### Story: Ahmed wants to go fix a leak

**Step 0 — Login**

Ahmed opens `/login`, types `EMP001` + `password`.

`CustomUserDetailsService.java:25` loads employee by matricule, gives authority = role (`EMPLOYEE`). `SecurityConfig.java:80-84` redirects by role: employee -> `/employee/dashboard`.

Session lasts 30 min (`server.servlet.session.timeout=30m`). Only 1 session per user: if Ahmed logs in on a second laptop, the first is kicked to `/login?expired`.

**Step 1 — Create request** (`EmployeeController.java:77-211` -> `MissionService.java:138-175`)

Ahmed fills: objet (min 10 chars), destination (min 3), depart, retour, transport, combined? + participants.

System checks:

- depart not in past, retour after depart
- if `combinee=true`, at least 1 participant required
- malformed participant UUIDs are skipped (anti-tampering)

Then `MissionService.create()`:

1. Load requester.
2. Find chef: first look in `service_approver` for "Service Eau" -> CHEF001. If not found, use `default_chef_matricule`. If still nothing -> throw `ApprovalRoutingException` ("No chef configured").
3. Find directeur from `default_directeur_matricule`. If missing -> throw.
4. Save mission with status `en_attente_chef`.
5. Create step 1 (ordre=1, chef, en_attente) + step 2 (ordre=2, directeur, en_attente).
6. Save participants.
7. Send notification to chef (async, see section 8).

All inside `@Transactional`: either everything saves, or nothing. No half-mission.

**Step 2 — Chef decides** (`ChefController.java:60-86` -> `MissionService.java:196-245`)

Karim sees the mission in `/chef/pending` because query finds missions where `s.approver.id = Karim AND s.ordre=1 AND s.statut=en_attente` (`MissionRepository.java:14-15`).

He opens `/chef/approval/{id}`. System checks `isAssignedApprover()`: is this really his mission? If not, redirect + toast "not assigned to you".

He picks Approve + optional vehicle, or Reject + required comment.

What code does:

- If already processed (`en_attente` check), do nothing = safe against double-click / F5.
- Set step 1 to `approuve` or `rejete` + date.
- If vehicle chosen AND transport is `vehicule_de_service`, attach vehicle. If transport is personal, ignore vehicle (you don't assign a company car to someone using his own car).
- If rejected: mark step 2 as `annulee`, mission = `rejetee`, notify requester.
- If approved: mission = `en_attente_directeur`, notify requester + directeur.

**Step 3 — Directeur decides** (`DirecteurController.java:56-80` -> `MissionService.java:248-275`)

Nadia sees it in `/directeur/pending`. Same guards + one extra:

- If step 1 is not `approuve`, refuse to act. This stops someone calling the URL directly to skip the chef.

If approve -> mission `approuvee`, notify requester "printable".
If reject (comment required) -> mission `rejetee`, notify with reason.

**Step 4 — Print**

Ahmed can open `/employee/mission/{id}/print` only if status is `approuvee` (`EmployeeController.java:228`). Otherwise redirect back. Print page is clean for paper.

End of story.

### Routing edge: who is my chef?

```
Is there a mapping for my exact service? (case-insensitive)
  YES and chef still has role CHEF_HIERARCHIQUE -> use him
  NO or chef demoted/removed -> use default_chef_matricule from settings
    If that is also empty/invalid -> error, cannot create mission
```

Same for directeur, but only global setting, no per-service mapping.

Admin configures this at `/admin/settings` (`AdminSettingsController.java`). Warnings show if a service has no chef and no fallback.

---

## 7. Security — how break-ins are blocked

1. **Passwords hashed** with BCrypt (`PasswordEncoder`). DB leak does not give passwords.
2. **Login by matricule**, not email. `usernameParameter("matricule")`, `passwordParameter("motDePasse")`.
3. **URL guards** (`SecurityConfig.java:30-37`):
   - `/chef/**` needs `CHEF_HIERARCHIQUE`
   - `/directeur/**` needs `DIRECTEUR`
   - `/admin/**` needs `ADMIN`
   - `/employee/**` needs any login
4. **Double check in controller**: even if you guess the URL `/chef/approval/{other-id}`, `requireAssignedApprover` throws and you get the 403 page (logged with your matricule).
5. **Object ownership**: `/employee/mission/{id}` and `/print` call `requireOwnedBy` — you only ever see your own missions. Unknown id redirects (no ID enumeration); someone else's id gives 403.
6. **Rate limiting**: 5 bad logins in 10 min (per IP + matricule) block for 15 min, rejected before BCrypt burns CPU; 10 missions/hour per employee. Counters are in-memory (reset on restart).
7. **CSRF active**: forms must come from the app's pages (token `_csrf`). Copy-paste POST from outside fails with 403.
8. **CSP + no iframe** (`default-src 'self'`, `frameOptions deny`): blocks injected scripts / clickjacking.
9. **Session**: 30 min timeout, 1 session max, error pages hide stacktraces (`include-stacktrace=never`).

---

## 8. Emails — smart design

File: `service/MissionNotificationService.java`

Idea: **email must never break a mission**.

How:

1. When mission is created/approved/rejected, code only builds a `MissionNotification` object (in memory) and publishes an event.
2. Real sending happens **after DB commit** (`@TransactionalEventListener(AFTER_COMMIT)`) in background (`@Async`).
3. If SMTP not configured or email invalid or send fails -> just log a warning, workflow continues.

If `MAIL_HOST` is empty (default), logs say "SMTP non configuré" and app keeps working. To enable, set `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

Email contains: requester, objet, destination, dates, reason if rejected, direct link (`PORTAL_BASE_URL + /chef/approval/...`).

---

## 9. Edge cases — the part that shows you really understand

This is what interviewers ask. Learn these 18:

| # | Edge case | What happens | Where |
|---|-----------|--------------|-------|
| 1 | Double-click Approve | Second click ignored (`if !"en_attente" return`). No duplicate. | `MissionService.java:203,260` |
| 2 | Chef opens directeur URL | Blocked by role + `isAssignedApprover(ordre=2)` fails. Redirect. | `DirecteurController.java:46` |
| 3 | Directeur tries before chef | Refused: `earlierStepNotApproved` check returns without change. | `MissionService.java:257-259` |
| 4 | Reject without comment | Form re-shown with error "Veuillez fournir un motif". Not saved. | `ChefController.java:72-80` |
| 5 | Depart in past | Error "ne peut pas être dans le passé". | `EmployeeController.java:117` |
| 6 | Retour before depart | Error "doit être postérieure". | `EmployeeController.java:135` |
| 7 | Objet too short (<10) | Error, ask to describe more. Prevents "test", "x". | `EmployeeController.java:95` |
| 8 | Combined mission, no participants | Error "Au moins un participant requis". | `EmployeeController.java:146` |
| 9 | Hacker sends bad participant UUID | `try UUID.fromString catch ignored` -> skipped, no crash. | `EmployeeController.java:183-187` |
| 10 | No chef for my service + no fallback | `ApprovalRoutingException`, form shows routing error, nothing saved. | `MissionService.java:142-144` |
| 11 | No directeur configured | Same, "Aucun directeur... configuré". | `MissionService.java:145-147` |
| 12 | Chef mapping points to someone demoted from chef | Filter `role==CHEF_HIERARCHIQUE` fails -> fallback to default chef. Stale mapping ignored. | `ServiceApproverService.java:54` |
| 13 | Assign vehicle but transport = personal | Vehicle ignored. Only `vehicule_de_service` gets a car. | `MissionService.java:214` |
| 14 | Print not-yet-approved mission | Redirect to detail, cannot print. | `EmployeeController.java:228` |
| 15 | SMTP down / no email | Warning logged, mission still saved. Email never blocks. | `MissionNotificationService.java:99-112` |
| 16 | Recipient has bad email | Regex check `^[^@]+@[^@]+\.[^@]+$`, skip + warn. | `MissionNotificationService.java:139-141` |
| 17 | Second login kicks first | `maximumSessions(1)` + `/login?expired`. Prevents account sharing confusion. | `SecurityConfig.java:51-55` |
| 18 | Mission deleted | Steps + participants auto-deleted by `ON DELETE CASCADE` + `orphanRemoval`. No orphans. | `schema-install.sql:84,100`, `Mission.java:49-53` |
| 19 | Lazy loading crash in page | `initialize()` touches collections inside transaction so Thymeleaf can read after session closes. | `MissionService.java:117-135` |
| 20 | Invalid action string | `if !"approuve" && !"rejete" return mission` -> ignore tampered POST. | `MissionService.java:198,250` |

Tests prove routing: `MissionServiceRoutingTests.java` has 6 tests (mapped chef used, fallback used, chef approve/reject notify, directeur approve/reject notify).

---

## 10. Admin + demo data

- Admin dashboard: counts, lists. Manage employees (`AdminEmployeesController`), all missions (`AdminMissionsController`), vehicles, settings.
- `DataInitializer.java:39-45` seeds only if `employee` table empty. So first run creates 7 users + 5 vehicles + 6 missions covering all states (pending, approved, rejected) + settings. Second run does nothing (safe re-runnable, like `schema-install.sql` with `IF NOT EXISTS`).
- `data/` folder is empty, unused. Ignore it.

---

## 11. How to explain it in 30 seconds, 2 minutes, 5 minutes

**30 seconds (to a non-tech boss):**
> "Employees request trips in the app. Their direct boss approves first, then the director approves. The app picks the right boss automatically by service, sends emails, and lets the employee print the final paper. Everything is stored in MySQL."

**2 minutes (to a new developer):**
> "Spring Boot + Thymeleaf + MySQL. Four roles with different pages. Core is Mission with two ApprovalSteps (ordre 1 chef, 2 directeur). MissionService.create resolves chef via service_approver mapping then fallback to system_config, directeur via system_config. Approve methods are idempotent, transactional, and fire async emails after commit. Security is matricule login + role URLs + assigned-approver check."

**5 minutes (technical):**
Add DB schema (7 tables, UUID, FK cascade), repository JPQL by approver+ordre, validation in EmployeeController, edge guards in MissionService, notification AFTER_COMMIT pattern, DataInitializer seeding, prod switch ddl-auto validate + env vars.

---

## 12. Q&A — practice these

**Q: Why MySQL?**
A: Need permanent, shared, strict relational data with transactions. MySQL is free, known by the team, enforces foreign keys, survives restarts. H2 alone would lose data. Mongo would allow bad data.

**Q: What are the tables?**
A: employee, vehicle, mission, approval_step, mission_participant, system_config, service_approver. Mission is center, linked to requester, optional vehicle, 2 steps, N participants.

**Q: How does the app know who my chef is?**
A: Look up my service in service_approver (case-insensitive key). If found and still a chef, use him. Else use default_chef from system_config. Same idea for directeur but only global.

**Q: Can directeur approve before chef?**
A: No. Code checks earlier steps are `approuve`, else does nothing.

**Q: What if chef rejects?**
A: Mission becomes `rejetee`, step 2 becomes `annulee`, requester gets email with reason. Workflow stops.

**Q: Can I approve twice?**
A: No. If step is not `en_attente`, method returns immediately.

**Q: Where is validation?**
A: In EmployeeController.saveMission: objet length, destination, dates, transport whitelist, combined needs participants. Reject needs comment in Chef/Directeur controllers.

**Q: What if email server is down?**
A: Nothing breaks. Notification is async after commit, failures only logged.

**Q: How is login done?**
A: Matricule + password, BCrypt hash, Spring Security, role-based URLs, 1 session, 30 min timeout.

**Q: How do you run in production safely?**
A: Create dedicated MySQL user (not root), set `JPA_DDL_AUTO=validate`, `THYMELEAF_CACHE=true`, set strong passwords, set `PORTAL_BASE_URL` to real domain, put behind HTTPS reverse-proxy, delete demo data.

**Q: What would you improve?**
A: Rename artifact `ordreDeMissison` typo, add real tests for controllers, add audit log, add file upload for proofs, add pagination for large lists, Docker file.

---

## 13. File map — where to look when lost

```
pom.xml = dependencies (web, thymeleaf, jpa, security, mail, mysql, h2)
src/main/resources/application.properties = all config via env vars
database/schema-install.sql = true SQL schema, safe re-runnable
model/ = Employee, Mission, ApprovalStep, MissionParticipant, Vehicle, SystemConfig, ServiceApprover
repository/ = 7 interfaces, custom JPQL in MissionRepository
service/MissionService.java = THE BRAIN (create, approveByChef, approveByDirecteur)
service/MissionNotificationService.java = async emails after commit
service/ServiceApproverService.java = service -> chef mapping
service/SystemConfigService.java = default chef/directeur
config/SecurityConfig.java = who can go where
config/CustomUserDetailsService.java = login by matricule
config/DataInitializer.java = demo data if empty
controller/EmployeeController.java = create + validation + print guard
controller/ChefController.java + DirecteurController.java = assigned-check + comment-required
controller/Admin* = dashboard, employees, missions, settings
templates/ = 22 pages (login, employee/, chef/, directeur/, admin/, error/)
```

---

## 14. One-linecheat sheet to memorize

> Employee creates -> system picks chef (service map else default) + directeur (default) -> chef approves (can set car) -> directeur approves -> printable. Any reject stops all. Emails never block. All guarded by role + assignment + idempotency.

Good luck explaining. You now know more than most people who just click through the app.

