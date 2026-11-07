# CARE Eats Database

CARE Eats uses PostgreSQL hosted on Supabase for shared storage. Spring JDBC connects the backend to the database, and Flyway manages schema migrations. H2 provides a separate local development database.

Application tables are stored in the `careeats` schema.

## Requirements

- Java 17 or later
- Maven or the included Maven wrapper
- Database credentials for Supabase access

The examples below use PowerShell.

## Run locally

From the repository root, in a fresh terminal:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'local'
mvn spring-boot:run
```

The backend runs at `http://localhost:8080`. Local data persists in `backend/data/careeats.mv.db`.

## Run with Supabase

From `backend`, configure the connection:

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

Credentials remain outside Git. These settings apply only to the current terminal. `.env.example` documents the variables; it is not loaded automatically.

In Supabase’s Table Editor, select the `careeats` schema.

## Migrations and sample data

Migration files are stored in `src/main/resources/db/migration`.

Before startup, set `CAREEATS_MIGRATE` to `'true'` to apply pending Supabase migrations. Set `CAREEATS_SEED_ENABLED` to `'true'` to populate an empty application database.

Sample data includes 46 accounts, 14 restaurants, 37 menu items and 84 orders. The loader records completion in `seed_runs` and skips subsequent runs.

Original demo credentials are listed in `src/main/resources/accounts.json`. Additional demo accounts use `DemoOnly123!`.

Keep both settings `'false'` for normal shared-database use.

## Tests

From `backend`:

```powershell
mvn test
```

Tests use isolated H2 and disposable local PostgreSQL databases, without connecting to Supabase.

If Maven is unavailable on PATH, use `./mvnw.cmd` on Windows or `./mvnw` on macOS/Linux. The Windows wrapper requires `powershell` on PATH.