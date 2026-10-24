# JobHire — Job Portal

A full-stack job portal starter for job seekers and employers, built with React, Spring Boot, Spring Security, Hibernate, and PostgreSQL.

## Features

- India-focused job search with keyword, city, work-mode, employment-type, experience, and CTC filters.
- Job seeker and employer registration with BCrypt password hashing and JWT authentication.
- Employer job posting, editing, opening, and closing.
- Employer role dashboard for managing open and closed roles.
- Job applications with duplicate protection, resume links, and status tracking.
- Job seekers can withdraw applications that are still submitted or under review; withdrawn records remain visible in application history.
- Optional email notifications confirm applications to job seekers, alert employers to new applications and withdrawals, and inform job seekers when employers update application statuses.
- Employer review of candidates and status changes: submitted, reviewing, interview, rejected, and hired; withdrawn applications remain in history and cannot be changed.
- Employer candidate applications can be searched by applicant name, email, or job title and filtered by application status.
- Authenticated server-sent events refresh application and interview data live, with automatic reconnects and a visible connection state.
- Employers can propose interview times with a duration, optional meeting link, and notes; job seekers can accept or decline pending invitations.
- Job-seeker profiles with a completion score, skills, education, bio, and a resume/portfolio link employers can open.
- Resume links are limited to valid HTTP/HTTPS URLs; no file upload or cloud-storage credentials are required.
- Administrator-only user listing. Admin accounts are deliberately not self-registerable.
- Responsive React frontend with login/register, job posting, apply, and application tracker flows.
- Eighteen India-focused sample job posts are added on startup when missing, including annual CTC in ₹ LPA and monthly internship stipends. Existing legacy demo listings are localized once. Demo data can be disabled with `SEED_DEMO_DATA=false`.
- Job search results are paginated from the API, with nine roles per page by default and pagination preserved for filters and saved roles.

## Requirements

- Java 17+
- Maven 3.8+
- Node.js 18+
- PostgreSQL 14+

## Database

Create the local database:

```sql
CREATE DATABASE job_portal;
```

The backend defaults to the supplied local connection:

```properties
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/job_portal
spring.datasource.username=postgres
spring.datasource.password=123
```

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables to override these development defaults. Flyway applies versioned PostgreSQL migrations at startup, and Hibernate updates the remaining local-development schema (`spring.jpa.hibernate.ddl-auto=update`).

Email notifications are disabled by default. To enable them, configure an SMTP provider and set `EMAIL_NOTIFICATIONS_ENABLED=true`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`. SMTP authentication and STARTTLS default to enabled and can be changed with `MAIL_SMTP_AUTH` and `MAIL_SMTP_STARTTLS` when required by your provider. Keep SMTP credentials in environment variables or a secret manager, never in source control. Notification sending is asynchronous; mail delivery failures are written to the backend log and do not undo an application or status change.

For local development, demo employers are also created for the sample postings:

| Email | Password | Company |
| --- | --- | --- |
| `hiring@northstar.example` | `DemoPass123!` | Northstar Technologies |
| `careers@greener.example` | `DemoPass123!` | Greener Goods India |

These are public demo credentials. Do not use the demo accounts or enable demo data in a deployed environment. Set `SEED_DEMO_DATA=false` to disable the seeder.

## Run locally

Start the API from the repository root:

```powershell
mvn spring-boot:run
```

Start the frontend in another terminal from the repository root:

```powershell
npm install
npm run dev
```

The root `package.json` configures `frontend` as an npm workspace, so install and run the UI from the project root. Alternatively, run `npm install` and `npm run dev` from `frontend`. Open <http://localhost:5173>. The API is available at <http://localhost:8080/api>. Set `VITE_API_URL` if the API is hosted elsewhere.

If the page shows a fetch error, start the backend in a second terminal with `mvn spring-boot:run`, then open <http://localhost:8080/api/jobs> and confirm it returns JSON. Use <http://localhost:5173> for the frontend (or <http://127.0.0.1:5173>, also allowed by default). Do not mix `localhost` and `127.0.0.1` between the frontend and API. If you change `VITE_API_URL` or `CORS_ALLOWED_ORIGINS`, restart the relevant development server.

## Authentication and roles

Register at `POST /api/auth/register` with a `role` of `JOB_SEEKER` or `EMPLOYER`. Employer registration requires `companyName`. Sign in at `POST /api/auth/login`; send the returned token as `Authorization: Bearer <token>`.

An administrator must be provisioned through a trusted administrative process, not public registration. Assign an existing user the `ADMIN` role directly in the database or through a future protected admin provisioning workflow. Configure a strong Base64-encoded `JWT_SECRET` in non-local environments; the bundled value is for development only.

## Main API routes

| Method | Route | Access |
| --- | --- | --- |
| POST | `/api/auth/register`, `/api/auth/login` | Public |
| GET | `/api/jobs`, `/api/jobs/{id}` | Public |
| GET | `/api/jobs/saved?ids=1&ids=2` | Public; saved job IDs supplied by the client |
| GET | `/api/jobs/mine` | Employer / Admin |
| POST | `/api/jobs` | Employer / Admin |
| PUT | `/api/jobs/{id}` | Owning Employer / Admin |
| PATCH | `/api/jobs/{id}/status?status=OPEN` | Owning Employer / Admin |
| DELETE | `/api/jobs/{id}` (soft close) | Owning Employer / Admin |
| GET, PUT | `/api/profile/me` | Authenticated |
| POST | `/api/jobs/{jobId}/applications` | Job seeker |
| GET | `/api/applications/me` | Job seeker |
| GET | `/api/jobs/{jobId}/applications` | Owning Employer / Admin |
| PATCH | `/api/applications/{applicationId}/status` | Owning Employer / Admin |
| PATCH | `/api/applications/{applicationId}/withdraw` | Owning Job Seeker; submitted/reviewing only |
| GET | `/api/events/stream` | Authenticated; SSE live updates |
| GET | `/api/interviews/me` | Job seeker / Employer |
| POST | `/api/applications/{applicationId}/interviews` | Owning Employer / Admin |
| PATCH | `/api/interviews/{interviewId}/response` | Owning Job Seeker; `ACCEPTED` or `DECLINED` |
| GET | `/api/admin/users` | Admin |

Interview proposals require a future `proposedAt` timestamp, a duration between 15 and 240 minutes, and an optional HTTP(S) meeting URL. Only one unanswered proposal can be active for an application. Proposing an interview advances the application to `INTERVIEW`; the invitation remains pending until the job seeker accepts or declines. Interview records are created through Hibernate schema update for existing local databases.

The live event stream sends only update notifications (not application or interview details); the frontend refetches the user's authorized REST data after each event. It authenticates with the bearer token in the request header, reconnects after interruptions, and receives a heartbeat every 20 seconds. The in-memory SSE registry is local to one backend process; multi-instance deployments need a shared event broker such as Redis or PostgreSQL-based messaging.

`PUT /api/profile/me` accepts `fullName`, `bio`, `resumeUrl`, `skills`, `education`, and `companyName` as applicable to the signed-in role. For job seekers, `skills` and `education` are arrays of strings. Resume URLs must use HTTP or HTTPS. The profile modal shows a completion score and lets the user preview the saved link. Hibernate's `ddl-auto=update` adds the profile fields to existing local databases on backend startup.

For regular roles, job create/update requests accept optional `salaryMinLpa` and `salaryMaxLpa` decimal fields. Values represent annual CTC in lakhs per annum (LPA); for example, `8.5` means ₹8.5 lakh per year. For internships, use `stipendMinMonthly` and `stipendMaxMonthly`, which represent monthly amounts in rupees. Do not submit both annual CTC and monthly stipend values for one posting; each range's minimum cannot exceed its maximum. CTC is the employer's annual cost-to-company figure, not a promise of monthly take-home pay.

Public job search accepts optional `keyword`, `location`, `workMode` (`ONSITE`, `HYBRID`, or `REMOTE`), `employmentType`, `experienceYears`, CTC (`minSalaryLpa`, `maxSalaryLpa`) or internship stipend (`minStipendMonthly`, `maxStipendMonthly`) range parameters, and zero-based `page` / `size` pagination parameters (default page 0, size 9; maximum size 24). The response uses Spring's page format with `content`, `totalElements`, `totalPages`, `number`, and `size`. Saved-job pagination accepts repeated `ids` parameters. Job posts accept `workMode`, `minExperienceYears`, and `maxExperienceYears`; older postings infer work mode from their location until edited.
