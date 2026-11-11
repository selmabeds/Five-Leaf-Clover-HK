# CARE Eats Database

CARE Eats uses PostgreSQL hosted on Supabase for shared storage and a file-based H2 database for local testing. Spring JDBC connects the backend to the database, and Flyway applies schema migrations. Application tables are stored in the `careeats` schema.

## Requirements

- Java 17 or later
- Maven or the included Maven wrapper
- Node.js and npm for the frontend
- Supabase database credentials only when using the shared database

The commands below use PowerShell.

## Run locally

Local H2 requires no Supabase account or database password. The demo accounts below let you test the app against a populated local database.

From the repository root, start the backend in a fresh terminal:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:CAREEATS_SEED_ENABLED = 'true'
mvn spring-boot:run
```

Flyway applies pending migrations at startup. The backend runs on port `8080`, and local data persists in `backend/data/careeats.mv.db`.

In a second terminal, from the repository root:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open CARE Eats (http://localhost:5173/) and sign in with a demo account.

| Role | Email | Demo password |
|---|---|---|
| Customer (Amira) | `customer1@example.test` | `DemoOnly123!` |
| Restaurant 1 (CARE Eats Test Restaurant) | `restaurant@careeats.com` | `Restaurant123!` |
| Courier | `courier@careeats.com` | `Courier123!` |
| Operator | `operator@careeats.com` | `Operator123!` |



## Run with Supabase

From `backend`, configure the shared connection:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'supabase'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require'
$env:SPRING_DATASOURCE_USERNAME = 'postgres.ykvwxfhnpocicvyevghs'

$dbSecret = Read-Host 'Database password' -AsSecureString
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', $dbSecret).Password
Remove-Variable dbSecret

$env:CAREEATS_MIGRATE = 'false'
$env:CAREEATS_SEED_ENABLED = 'false'
mvn spring-boot:run
```

Keep database passwords outside Git. These variables apply to the current terminal. `.env.example` lists the variables for reference. Set them in the terminal before starting the backend.

In Supabase’s Table Editor, select the `careeats` schema.

## Migrations and sample data

SQL migrations are in `src/main/resources/db/migration`. Java migrations are in `src/main/java/db/migration`. Flyway records applied versions in `careeats.flyway_schema_history`.

For an agreed shared database update, set `CAREEATS_MIGRATE` to `'true'` before starting the backend. Keep it at `'false'` for normal shared use.

Set `CAREEATS_SEED_ENABLED` to `'true'` to populate an empty application database. The sample data includes 46 accounts, 14 restaurants, 37 menu items, and 84 orders. The loader records completion in `seed_runs` and skips seeding on later startups. Keep seeding disabled for normal shared use.

The original demo accounts are listed in `src/main/resources/accounts.json`. Additional demo accounts use `DemoOnly123!`.

## Tests

From `backend`:

```powershell
mvn test
```

Tests use isolated H2 and disposable local PostgreSQL databases, without connecting to Supabase.

If Maven is unavailable on PATH, use `.\mvnw.cmd` on Windows or `./mvnw` on macOS/Linux in place of `mvn`. The Windows wrapper requires `powershell` on PATH.