# PFT Personal Finance Tracker

PFT is a Spring Boot backend for tracking personal income, expenses, budgets, reports, and user account security. It exposes REST APIs for client applications to register users, authenticate with JWT, manage categories and transactions, review dashboard summaries, generate financial reports, export PDFs, and email monthly reports.

The project is implemented as a layered backend application:

```text
HTTP request
  -> Spring Security JWT filter
  -> REST controller
  -> service layer
  -> Spring Data JPA repository
  -> MySQL database
```

## Table of Contents

- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Environment Configuration](#environment-configuration)
- [Database Setup and Flyway Migrations](#database-setup-and-flyway-migrations)
- [Installation and Local Startup](#installation-and-local-startup)
- [Authentication and Authorization](#authentication-and-authorization)
- [Core Business Workflows](#core-business-workflows)
- [API Reference](#api-reference)
- [Business Rules and Usage Notes](#business-rules-and-usage-notes)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Development Conventions](#development-conventions)
- [Known Limitations](#known-limitations)

## Key Features

- User registration with default `USER` role assignment.
- Password login with JWT issuance.
- Login failure tracking and temporary account lockout.
- Optional email-based two-factor authentication.
- JWT-protected APIs for categories, transactions, dashboard, budgets, reports, notifications, and profile management.
- User-owned categories and transactions.
- Monthly budget management for expense categories.
- Dashboard totals, pie chart data, and recent transaction feed.
- Category, monthly, and summary financial reports.
- Local PDF report export with optional charts and top-expense sections.
- Email delivery of summary reports with PDF attachments.
- Notification settings for daily reminders, tips, and budget alerts.

## Technology Stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.0 |
| Web | `spring-boot-starter-webmvc` |
| Security | Spring Security, method security |
| Persistence | Spring Data JPA, Hibernate managed by Spring Boot |
| Database | MySQL via `mysql-connector-j` runtime dependency |
| Migrations | Flyway via `spring-boot-starter-flyway` and `flyway-mysql` |
| Test database | H2, configured in `src/test/resources/application.properties` |
| JWT | JJWT `0.12.5` |
| PDF generation | OpenPDF `2.0.3` |
| Chart generation | JFreeChart `1.5.6` |
| Email | Spring Boot Mail, JavaMailSender |
| Boilerplate reduction | Lombok |
| Testing | JUnit 5, Mockito, Spring MVC test, Spring Security test |
| Coverage | JaCoCo Maven plugin `0.8.15` |
| Build tool | Maven Wrapper, Maven `3.9.16` |

Spring Boot manages versions for most Spring starters, Spring Security, Spring Data JPA, MySQL connector, H2, Flyway, Lombok, and test libraries.

## System Architecture

PFT uses a conventional Spring Boot package layout:

```text
src/main/java/org/example/pft
|-- controller    # REST API endpoints
|-- dto           # request and response payloads
|-- entity        # JPA entities
|-- enums         # CategoryType and ReportType
|-- exception     # API error model and exception handlers
|-- helper        # current-user and PDF helper utilities
|-- repository    # Spring Data JPA repositories and JPQL queries
|-- security      # JWT, user details, entry point, access denied handler
`-- service       # business interfaces, implementations, PDF renderers
```

Main responsibilities:

| Layer | Responsibility |
|---|---|
| Controller | Accepts HTTP requests, applies validation, delegates to services, returns DTO responses. |
| Security | Validates bearer tokens, loads authenticated users, enforces authentication and method-level role checks. |
| Service | Implements business rules, resource ownership checks, report calculations, PDF/email workflows. |
| Repository | Encapsulates JPA persistence and user-scoped financial queries. |
| DTO | Defines API request and response contracts plus Bean Validation rules. |
| Exception handling | Converts validation, business, authentication, authorization, and not-found errors into JSON responses. |
| PDF/report components | Render report data and optional charts into OpenPDF documents. |

## Project Structure

Important files and directories:

```text
pom.xml
mvnw / mvnw.cmd
src/main/resources/application.properties
src/main/resources/db/migration/
  V1__create_schema.sql
  V2__seed_roles.sql
  V3__seed_category_icons.sql
src/test/resources/application.properties
src/test/java/org/example/pft/
reports/
target/site/jacoco/
target/surefire-reports/
```

The `reports/` directory is used by the PDF export feature at runtime. Generated files are named with the current user ID, report type, month, and year.

## Prerequisites

- JDK 17.
- MySQL server with an empty database for a fresh local setup.
- Environment variables for database, JWT, and SMTP configuration.
- Internet access for Maven dependency resolution on first build.
- Gmail SMTP credentials or compatible SMTP settings if using OTP or email report features.

No Docker Compose, frontend application, Swagger UI, or OpenAPI specification is configured in this repository.

## Environment Configuration

Runtime configuration is defined in `src/main/resources/application.properties`.

| Variable | Purpose | Required | Example |
|---|---|---:|---|
| `DB_URL` | JDBC URL for the MySQL database. Defaults to local `pft` database if omitted. | No | `jdbc:mysql://localhost:3306/pft?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL username. Defaults to `root`. | No | `pft_user` |
| `DB_PASSWORD` | MySQL password. Defaults to empty string. | No | `change-me` |
| `SECRET_KEY` | Base64-encoded JWT signing key. Must decode to at least 32 bytes. | Yes | `base64-encoded-32-byte-secret` |
| `MAIL_USERNAME` | SMTP username. Used for OTP and report emails. | Yes for email features | `your-account@example.com` |
| `MAIL_PASSWORD` | SMTP password or app password. | Yes for email features | `app-specific-password` |

JWT defaults:

| Property | Value |
|---|---|
| `app.jwt.expiration-seconds` | `3600` |
| `app.jwt.issuer` | `pft` |

Generate a development JWT secret without committing it:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Then set it as an environment variable before starting the application.

```powershell
$env:SECRET_KEY = "<base64-encoded-secret>"
$env:DB_URL = "jdbc:mysql://localhost:3306/pft?useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "pft_user"
$env:DB_PASSWORD = "<database-password>"
$env:MAIL_USERNAME = "<smtp-username>"
$env:MAIL_PASSWORD = "<smtp-password>"
```

Do not commit real secrets to `application.properties`.

## Database Setup and Flyway Migrations

Flyway is enabled by default:

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate validates the schema at startup. Flyway is responsible for creating and seeding the database.

### Migration Responsibilities

| Migration | Responsibility |
|---|---|
| `V1__create_schema.sql` | Creates roles, category icons, users, categories, budgets, transactions, user roles, notification settings, and two-factor challenge tables. |
| `V2__seed_roles.sql` | Inserts default roles `ADMIN` and `USER` if missing. |
| `V3__seed_category_icons.sql` | Inserts default category icons if missing. |

### Default Roles

Flyway seeds:

- `ADMIN`
- `USER`

Newly registered users receive the `USER` role.

### Default Category Icons

Flyway seeds 12 category icon names. IDs are generated by MySQL auto-increment.

| Category icon | `icon_url` |
|---|---|
| Housing | `https://cdn.example.com/icons/housing.png` |
| Food | `https://cdn.example.com/icons/food.png` |
| Shopping | `https://cdn.example.com/icons/shopping.png` |
| Salary | `https://cdn.example.com/icons/salary.png` |
| Freelance | `https://cdn.example.com/icons/freelance.png` |
| Investments | `https://cdn.example.com/icons/investments.png` |
| Other | `https://cdn.example.com/icons/other.png` |
| Transportation | `https://cdn.example.com/icons/transportation.png` |
| Entertainment | `https://cdn.example.com/icons/entertainment.png` |
| Health | `https://cdn.example.com/icons/health.png` |
| Education | `https://cdn.example.com/icons/education.png` |
| Gifts | `https://cdn.example.com/icons/gifts.png` |

The `cdn.example.com` URLs are placeholder asset URLs in the seed data.

### Scenario A: Fresh Database

1. Create an empty MySQL database.

   ```sql
   CREATE DATABASE pft CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```

2. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SECRET_KEY`, `MAIL_USERNAME`, and `MAIL_PASSWORD`.

3. Start the application with the Maven Wrapper.

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Let Spring Boot run Flyway automatically.

5. Verify that tables such as `users`, `transactions`, `budgets`, and `two_factor_challenges` exist.

6. Verify that roles and category icons were inserted.

   ```sql
   SELECT name FROM roles ORDER BY name;
   SELECT category_name, icon_url FROM category_icons ORDER BY id;
   ```

Do not manually execute the migrations during normal startup. Flyway runs them automatically.

### Scenario B: Existing Database

An existing database may require a Flyway baseline before migrations can be enabled safely. The development database for this project was previously baselined at version 3 because its schema and seed data already matched `V1` through `V3`.

Do not treat baseline version 3 as a universal rule for arbitrary databases. An incorrect baseline can hide missing tables or seed data.

Recommended verification before baselining an existing database:

1. Compare the existing schema against `V1__create_schema.sql`.
2. Confirm `ADMIN` and `USER` roles exist.
3. Confirm all 12 category icons exist.
4. Confirm constraints and foreign keys match the migration files.
5. Create a database backup before changing Flyway metadata.
6. Apply an explicit baseline only after the database is proven equivalent to the skipped migrations.

The default application configuration should not silently baseline arbitrary existing databases. `baseline-on-migrate=true` is not enabled in this project.

## Installation and Local Startup

Clone the repository and move into the project directory:

```powershell
git clone <repository-url>
cd PFT
```

Set environment variables:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/pft?useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "pft_user"
$env:DB_PASSWORD = "<database-password>"
$env:SECRET_KEY = "<base64-encoded-secret>"
$env:MAIL_USERNAME = "<smtp-username>"
$env:MAIL_PASSWORD = "<smtp-password>"
```

Start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The application uses Spring Boot's default port unless overridden externally. Verify startup by calling a public endpoint such as registration or login, or by checking the startup logs for successful Flyway migration and Hibernate schema validation.

## Authentication and Authorization

### Security Model

`SecurityConfig` applies these rules:

| Path | Rule |
|---|---|
| `/api/auth/**` | Public |
| All other endpoints | Authenticated request required |
| `PUT /api/user/profile` | Authenticated request with `ROLE_USER` required |

JWT credentials must be passed as a bearer token:

```http
Authorization: Bearer <accessToken>
```

The JWT contains:

- issuer from `app.jwt.issuer`
- subject as the user's email
- issued-at and expiration timestamps
- `roles` claim from the user's assigned roles

The authentication filter ignores missing or invalid bearer tokens and lets Spring Security return the configured `401 Unauthorized` response for protected endpoints.

### Registration

`POST /api/auth/register` creates a user if the email is not already registered, assigns the default `USER` role, and creates notification settings for the user.

Registration rules:

- `email` is required and must be valid.
- `password` is required, at least 8 characters, and must include both letters and numbers.
- `fullName` is required.
- Duplicate email returns `409 Conflict`.
- Missing default `USER` role returns `404 Not Found`.

### Login

`POST /api/auth/login` validates email and password.

Login failure rules:

- Unknown email and wrong password return `422 Unprocessable Entity` with `Invalid email or password`.
- Failed login attempts are counted.
- On the fifth failed attempt, the account is locked for 30 minutes.
- While locked, login returns a message with the remaining minutes.
- A successful login after previous failures resets failed attempts and lock state.

JWT issuance:

- If two-factor authentication is disabled, login returns an access token immediately.
- If two-factor authentication is enabled, login returns `challengeId` and `data: null`; the client must verify the OTP before receiving a JWT.

### Two-Factor Authentication

2FA is controlled by the user's profile field `twoFactorEnabled`.

When enabled:

1. Login creates or reuses a two-factor challenge.
2. The system sends a 6-digit OTP by email.
3. The client verifies the code with `POST /api/auth/2fa/verify`.
4. A valid code marks the challenge as used and returns the JWT.

2FA rules:

| Rule | Value |
|---|---|
| OTP length | 6 digits |
| OTP expiration | 5 minutes |
| Max verification attempts | 5 |
| Resend cooldown | 60 seconds |

Failure cases:

- Unknown challenge ID returns `404 Not Found`.
- Used challenge returns `422 Unprocessable Entity`.
- Expired code returns `422 Unprocessable Entity`.
- Wrong code increments attempt count.
- Reaching 5 attempts returns `422 Unprocessable Entity`.
- Resend during cooldown returns a message with remaining seconds.

## Core Business Workflows

### Categories

Categories are user-owned and typed as `INCOME` or `EXPENSE`.

The category creation endpoint links a user category to one seeded `CategoryIcon`. If `emoji` is omitted or blank, the service looks up the icon by `name`. If `emoji` is supplied, the service looks up the icon by emoji. Duplicate category names are checked per user.

### Transactions

Transactions record an amount, note, category, date, and owner. Amounts are stored as positive values. The category type determines how totals are interpreted:

- `INCOME` contributes to income totals.
- `EXPENSE` contributes to expense totals.
- Dashboard recent transactions display expenses as negative values and income as positive values.

The transaction service verifies that the category belongs to the authenticated user before creating a transaction.

### Budgets

Budgets are monthly targets for expense categories only. The service rejects income categories with `Category type must be EXPENSE`.

Budget create is an upsert by `(user, category, month, year)`: if a budget already exists for the same user, category, month, and year, its amount is replaced.

Budget update and delete locate budgets with `findByIdAndUser`, so another user's budget is treated as not found.

### Dashboard

Dashboard data is computed for a month and year:

- income total
- expense total
- balance as `income - expense`
- pie chart data grouped by category and type
- up to 3 recent transactions for that period

### Reports

Reports are scoped to the authenticated user.

| Report | Behavior |
|---|---|
| Category report | Groups transaction totals by category for a selected `INCOME` or `EXPENSE` type and calculates category percentages. |
| Monthly report | Returns 12 months of income and expense chart data for a year plus the selected month summary. |
| Summary report | Returns income, expense, balance, and top 3 expense categories for the selected month. |

Empty totals are normalized to `0`.

### PDF Export

PDF export supports `SUMMARY`, `MONTHLY`, and `CATEGORY` report types. The default report type is `SUMMARY` when omitted or blank.

PDF files are written to `reports/` and named:

```text
<userId>_<reportType>_<month-short-name>_<year>.pdf
```

For example:

```text
2_summary_sep_2026.pdf
```

Optional flags:

- `includeChart`
- `includeTopExpenses`

### Email Report Delivery

Email export generates a summary PDF in memory and sends it as an attachment to the requested email address. It requires SMTP configuration and an authenticated user.

## API Reference

Base path examples assume the application is running locally on Spring Boot's configured port.

### Authentication

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new user. | Public |
| `POST` | `/api/auth/login` | Authenticate with email and password. | Public |
| `POST` | `/api/auth/2fa/verify` | Verify OTP and issue JWT. | Public |
| `POST` | `/api/auth/2fa/resend` | Resend OTP for an active challenge. | Public |

Register request:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "Example User"
}
```

Login success without 2FA:

```json
{
  "success": true,
  "message": "Login successful",
  "challengeId": null,
  "data": {
    "accessToken": "<jwt>",
    "expired": "2026-09-29T12:00:00"
  }
}
```

Login response with 2FA enabled:

```json
{
  "success": true,
  "message": "Two-factor verification required",
  "challengeId": "<challenge-id>",
  "data": null
}
```

Verify 2FA request:

```json
{
  "challengeId": "<challenge-id>",
  "code": "123456"
}
```

### User Profile

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `PUT` | `/api/user/profile` | Update full name, avatar, and 2FA setting. | `ROLE_USER` |

Request:

```json
{
  "fullName": "Updated User",
  "avatar": "avatar.png",
  "twoFactorEnabled": true
}
```

If `avatar` is omitted or blank, it is stored as `null`.

### Categories

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `GET` | `/api/categories` | List current user's categories grouped by type. | JWT |
| `GET` | `/api/categories?type=EXPENSE` | List current user's categories by type. | JWT |
| `POST` | `/api/categories` | Create a category from a seeded icon. | JWT |

Create category request:

```json
{
  "name": "Food",
  "type": "EXPENSE",
  "emoji": ""
}
```

### Transactions

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `POST` | `/api/transactions` | Create a transaction. | JWT |
| `GET` | `/api/transactions/history` | Query transaction history. | JWT |

Create transaction request:

```json
{
  "amount": 125000.50,
  "note": "Lunch",
  "categoryId": 20,
  "date": "2026-09-07"
}
```

History query:

```http
GET /api/transactions/history?startDate=2026-09-01&endDate=2026-09-30&type=EXPENSE&page=1&size=10
```

Optional query parameter:

- `categoryId`

### Budgets

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `POST` | `/api/budgets` | Create or replace a monthly budget for an expense category. | JWT |
| `GET` | `/api/budgets?month=9&year=2026` | List budgets for a month and year. | JWT |
| `PUT` | `/api/budgets/{id}` | Update a budget amount. | JWT |
| `DELETE` | `/api/budgets/{id}` | Delete a budget. | JWT |

Create budget request:

```json
{
  "categoryId": 20,
  "amount": 1000000,
  "month": 9,
  "year": 2026
}
```

### Dashboard

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `GET` | `/api/dashboard?month=9&year=2026` | Get income, expense, balance, pie chart data, and recent transactions. | JWT |

`month` and `year` default to the current month and year if omitted.

### Reports

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `GET` | `/api/reports/category?month=9&year=2026&type=EXPENSE` | Category breakdown report. | JWT |
| `GET` | `/api/reports/monthly?month=9&year=2026` | Monthly chart report for a year. | JWT |
| `GET` | `/api/reports/summary?month=9&year=2026` | Monthly summary report. | JWT |
| `POST` | `/api/reports/export/pdf` | Generate a local PDF file. | JWT |
| `POST` | `/api/reports/export/email` | Email a summary PDF report. | JWT |

PDF export request:

```json
{
  "month": 9,
  "year": 2026,
  "reportType": "SUMMARY",
  "includeChart": true,
  "includeTopExpenses": true
}
```

PDF export response:

```json
{
  "success": true,
  "message": "summary report PDF generated successfully",
  "data": "reports\\2_summary_sep_2026.pdf"
}
```

Email export request:

```json
{
  "month": 9,
  "year": 2026,
  "email": "recipient@example.com",
  "includeChart": true,
  "includeTopExpenses": true
}
```

### Notification Settings

| Method | URL | Purpose | Auth |
|---|---|---|---|
| `GET` | `/api/notifications/settings` | Read current user's notification settings. | JWT |
| `PUT` | `/api/notifications/settings` | Update notification settings. | JWT |

Update request:

```json
{
  "dailyReminder": true,
  "tipsEnabled": false,
  "budgetAlert": true
}
```

## Business Rules and Usage Notes

- Supported category types are `INCOME` and `EXPENSE`.
- Amounts are stored as positive values; category type controls income or expense interpretation.
- Balance is calculated as income minus expenses.
- Resource ownership is enforced through the current authenticated user.
- Cross-user category or budget access is normally hidden as `404 Not Found`.
- Transaction history requires `startDate`, `endDate`, and `type`.
- Transaction history `startDate` must be before or equal to `endDate`.
- Transaction history defaults to page `1` and size `10`, with maximum size `20`.
- Reports default month/year to the current month/year where the DTO defines defaults.
- PDF and email export require month and year. Valid year range is `1900..9999`.
- Budget create requires an expense category. Income categories are rejected.
- Budget list defaults month/year to current values if omitted.
- Registration creates notification settings with all three flags set to `false`.
- The migration default for `notification_settings.budget_alert` is `1`, but application-created settings use `false`.

## Error Handling

Application business and validation errors generally use this structure:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ]
}
```

Authentication and authorization failures use this structure:

```json
{
  "timestamp": "2026-09-29T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized - Please login to access this resource",
  "path": "/api/budgets"
}
```

HTTP statuses implemented by the application:

| Status | Source |
|---|---|
| `200 OK` | Successful reads, updates, login, report export responses. |
| `201 Created` | Category, budget, and transaction creation. |
| `204 No Content` | Budget delete and 2FA resend success. |
| `401 Unauthorized` | Missing or invalid authentication for protected endpoints. |
| `403 Forbidden` | Authenticated user lacks required method-level role, currently used by `PUT /api/user/profile`. |
| `404 Not Found` | `ResourceNotFoundException`, including hidden ownership failures. |
| `409 Conflict` | Duplicate email/category conflicts and PDF file write conflicts. |
| `422 Unprocessable Entity` | Bean validation failures and business validation errors. |
| `500 Internal Server Error` | Email send failures mapped by `EmailSendException`. |

Invalid Boolean JSON values are reported as validation errors with `Must be true or false` for the affected field when the field can be resolved.

## Testing

The test configuration uses H2:

```properties
spring.datasource.url=jdbc:h2:mem:pft_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=MONTH,YEAR;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=create-drop
```

Automated tests should not depend on the developer's local MySQL database.

Run the full test suite:

```powershell
.\mvnw.cmd test
```

Run selected test classes:

```powershell
.\mvnw.cmd -Dtest=BudgetControllerSecurityTest,CategoryControllerSecurityTest,TransactionControllerSecurityTest test
```

Run Maven verification and generate the JaCoCo report:

```powershell
.\mvnw.cmd verify
```

On macOS or Linux, use `./mvnw` instead of `.\mvnw.cmd`.

Generated reports:

| Report | Location |
|---|---|
| Surefire test reports | `target/surefire-reports/` |
| JaCoCo HTML report | `target/site/jacoco/index.html` |
| JaCoCo XML/CSV | `target/site/jacoco/` |

Existing tests cover controller security slices, service logic, repository isolation, DTO validation, exception handling, JWT service behavior, current-user resolution, email, PDF rendering, and chart helpers.

## Troubleshooting

| Symptom | Likely cause | Suggested check |
|---|---|---|
| Application fails at startup with missing JWT secret | `SECRET_KEY` is not set or blank. | Set a Base64 secret that decodes to at least 32 bytes. |
| JWT secret error says it must be Base64 encoded | `SECRET_KEY` is not valid Base64. | Generate a new Base64 value and restart. |
| MySQL connection failure | Incorrect `DB_URL`, credentials, or database not created. | Verify MySQL is running and the database exists. |
| Hibernate schema validation failure | Database schema does not match JPA mappings. | Confirm Flyway migrations ran successfully. |
| Flyway migration failure | Existing database is not empty or migration SQL failed. | Inspect `flyway_schema_history`; do not baseline until schema and seed data are verified. |
| Default role not found during registration | `V2__seed_roles.sql` did not run or was skipped incorrectly. | Verify `roles` contains `USER`. |
| Category creation returns icon not found | Requested `name` or `emoji` does not match a seeded category icon. | Query `category_icons` and use one of the seeded values. |
| OTP or report email fails | SMTP credentials or provider policy is invalid. | Verify `MAIL_USERNAME`, `MAIL_PASSWORD`, and provider app-password settings. |
| PDF export conflict | Target path is not writable or the PDF is open in another application. | Close the file and verify write permissions for `reports/`. |
| Port already in use | Another process is using the configured server port. | Stop the other process or set `server.port` externally. |

Avoid dropping or recreating a database as a routine fix. Back up data first and verify the root cause.

## Development Conventions

Observed repository conventions:

- Main source code is under `src/main/java/org/example/pft`.
- Tests mirror production modules under `src/test/java/org/example/pft`.
- Controller tests use `@WebMvcTest`, MockMvc, Spring Security test support, and mocked services.
- Service tests use JUnit 5 and Mockito.
- Repository isolation tests use Spring Boot context and transactional database tests.
- Use Maven Wrapper commands for repeatable local execution.
- Use feature branches for focused changes. Recent branch names follow patterns such as `test/...`, `feat/...`, and `docs/...`.

No formal pull request target, protected branch policy, or required commit format is documented in the repository. Recent commit history uses conventional-style prefixes such as `feat:`, `test:`, `chore:`, and `merge(...)`.

## Known Limitations

- The repository contains no frontend application.
- No OpenAPI or Swagger configuration is present.
- No Dockerfile or Docker Compose configuration is present.
- No license file is present; no license is declared here.
- Category icon URLs in the seed migration point to `cdn.example.com` placeholder assets.
- OTP codes are stored in the `otp_code` column as generated values; the field name in the entity is `otpHash`, but no hashing is implemented.
- `CreateBudgetRequest.month` is not annotated with `@NotNull`, although the service expects it to be present.
- `UpdateBudgetRequest.amount` is not annotated with `@NotNull`, although the database column is non-null.
- `V3__seed_category_icons.sql` begins with a `SELECT` statement before the inserts. This may be harmless in some environments but should be reviewed if Flyway execution behaves unexpectedly.
