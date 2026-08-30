# Setup Instructions - URL Shortener Service

This document provides step-by-step setup and deployment instructions for the URL Shortener service.

---

## Prerequisites

### System Requirements
- **OS:** Windows, macOS, or Linux
- **Docker:** Docker Desktop 4.0+ (with Docker Compose)
- **Java:** Java 17+ (for local development without Docker)
- **Maven:** Maven 3.8+ (for local builds)
- **Git:** Git 2.0+

### Verify Installation
```bash
docker --version          # Docker 20.10+
docker compose version    # Docker Compose v2+
java -version             # openjdk 17+
mvn --version             # Apache Maven 3.8+
```

### Start and Verify Docker Desktop (Windows)

Before running Docker Compose on Windows, open Docker Desktop from the Start menu and wait until it reports that Docker Desktop is running. Docker Desktop must use Linux containers with the WSL 2 based engine enabled.

Run the following command in Command Prompt to verify that both the Docker client and daemon are available:

```cmd
docker version
```

The output must contain both `Client` and `Server` sections. If the `Server` section is missing or the command reports an error for `dockerDesktopLinuxEngine`, Docker Desktop is not ready. Restart Docker Desktop, wait for it to finish starting, and run `docker version` again before continuing.

---

## Option 1: Run with Docker Compose (Recommended)

### Step 1: Clone the Repository
```cmd
git clone <repository-url>
cd url-shortener-ai-assisted-v2
```

### Step 2: Build and Start Services
```cmd
docker compose up --build
```

**What happens:**
- Builds the Spring Boot application JAR
- Starts PostgreSQL container (port 5432)
- Starts application container (port 8080)
- Flyway auto-migrates schema on startup

**Expected output:**
```
app_1  | ... INFO ... UrlShortenerApplication started
app_1  | ... INFO ... Tomcat started on port(s): 8080
app_1  | ... INFO ... Flyway has successfully validated this schema
db_1   | database system is ready to accept connections
```

### Step 3: Verify Services
```cmd
# Check if app is running
curl.exe -X GET "http://localhost:8080/actuator/health"

# Expected response:
# {"status":"UP"}
```

### Step 4: Stop Services
```cmd
docker compose down
```

---

## Option 2: Run Locally with External PostgreSQL

> **Verification note:** The external PostgreSQL configuration and commands below are included as the intended setup. They have not been run end-to-end against a local external PostgreSQL instance in this environment, so database connectivity and application startup remain to be verified. A local PostgreSQL process using port `5432` may require configuration changes before testing.

### Step 1: Install PostgreSQL
**On Windows (using Chocolatey):**
```bash
choco install postgresql
```

**On macOS (using Homebrew):**
```bash
brew install postgresql
```

**On Linux (Ubuntu/Debian):**
```bash
sudo apt-get install postgresql postgresql-contrib
```

### Step 2: Start PostgreSQL
```bash
# Windows
pg_ctl -D "C:\Program Files\PostgreSQL\data" start

# macOS
brew services start postgresql

# Linux
sudo systemctl start postgresql
```

### Step 3: Create Database
```bash
psql -U postgres -c "CREATE DATABASE url_shortener;"
```

### Step 4: Configure Application
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener
spring.datasource.username=postgres
spring.datasource.password=<your-password>
```

### Step 5: Build and Run
```bash
mvn clean package -DskipTests
java -jar target/url-shortener-ai-assisted-1.0.0.jar
```

**Expected output:**
```
INFO  ... UrlShortenerApplication : Started UrlShortenerApplication in 3.456 seconds
INFO  ... TomcatWebServer : Tomcat started on port(s): 8080
```

### Step 6: Verify Application
```cmd
curl.exe -X GET "http://localhost:8080/actuator/health"
```

---

## Option 3: Run Tests Locally

### Build without Docker
```bash
mvn clean package
```

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=UrlShorteningServiceTest
```

### Run Tests with Coverage
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Example Production Deployment (Azure)

This is a reference deployment approach and has not been deployed from this local environment.

1. Push the Docker image to Azure Container Registry (ACR).
2. Create an Azure Database for PostgreSQL Flexible Server instance.
3. Create an Azure Container Apps application that pulls the image from ACR and supplies the PostgreSQL connection settings as secrets.
4. Configure Container Apps ingress and HTTP health probes for the application.
5. Configure Container Apps scaling rules based on CPU and memory utilization.
6. Configure Azure Monitor and Application Insights alerts for error rates, failed requests, and resource utilization.

For a production deployment, keep database credentials in Azure Key Vault or Container Apps secrets, require encrypted PostgreSQL connections, and restrict database network access to the application environment.

---

## API Usage Examples

### Windows Command Prompt (`cmd`) Example

This exact command is compatible with Windows Command Prompt (`cmd.exe`) when run with `curl.exe`. It also works in VS Code's integrated terminal when the selected terminal profile is Command Prompt:

```cmd
curl.exe -X POST "http://localhost:8080/api/shorten" -H "Content-Type: application/json" -d "{\"longUrl\":\"https://www.example.com/new-unique-page-123\",\"customAlias\":\"demo42\",\"expiresInSeconds\":86400}"
```

### 1. Shorten a URL

**Request:**
```cmd
curl.exe -X POST "http://localhost:8080/api/shorten" -H "Content-Type: application/json" -d "{\"longUrl\":\"https://www.example.com/very/long/path/to/resource\",\"customAlias\":null,\"expiresInSeconds\":null}"
```

**Response (201 Created):**
```json
{
  "shortCode": "aBc1234",
  "shortUrl": "/aBc1234",
  "longUrl": "https://www.example.com/very/long/path/to/resource"
}
```

### 2. Shorten with Custom Alias

**Request:**
```cmd
curl.exe -X POST "http://localhost:8080/api/shorten" -H "Content-Type: application/json" -d "{\"longUrl\":\"https://www.example.com/blog/post\",\"customAlias\":\"myblog\",\"expiresInSeconds\":86400}"
```

**Response (201 Created):**
```json
{
  "shortCode": "myblog",
  "shortUrl": "/myblog",
  "longUrl": "https://www.example.com/blog/post"
}
```

### 3. Redirect to Original URL

**Request:**
```cmd
curl.exe -i -X GET "http://localhost:8080/aBc1234"
```

**Response (302 Found):**
```
HTTP/1.1 302 Found
Location: https://www.example.com/very/long/path/to/resource
Cache-Control: no-cache
```

### 4. Get Analytics

**Request:**
```cmd
curl.exe -X GET "http://localhost:8080/api/stats/aBc1234"
```

**Response (200 OK):**
```json
{
  "shortCode": "aBc1234",
  "longUrl": "https://www.example.com/very/long/path/to/resource",
  "clickCount": 5,
  "createdAt": "2026-08-29T15:30:00Z",
  "lastAccessedAt": "2026-08-29T16:45:00Z"
}
```

---

## Configuration Reference

### Application Properties

**File:** `src/main/resources/application.properties`

```properties
# Server
server.port=8080
server.servlet.context-path=/

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# Logging
logging.level.root=INFO
logging.level.com.schwab.urlshortener=DEBUG
```

### Docker Compose Environment

**File:** `docker-compose.yml`

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/url_shortener
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      - db
    networks:
      - shortener-network

  db:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: url_shortener
    volumes:
      - db-data:/var/lib/postgresql/data
    networks:
      - shortener-network

volumes:
  db-data:

networks:
  shortener-network:
    driver: bridge
```

---

## Troubleshooting

### Issue: "Port 5432 already in use"
**Cause:** Local PostgreSQL service or another container is using the port

**Solution (Option A - Stop local service):**
```bash
# Windows
pg_ctl -D "C:\Program Files\PostgreSQL\data" stop

# macOS
brew services stop postgresql

# Linux
sudo systemctl stop postgresql
```

**Solution (Option B - Use different port):**
Edit `docker-compose.yml`:
```yaml
db:
  ports:
    - "5433:5432"  # Use 5433 instead of 5432
```

Edit `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/url_shortener
```

### Issue: "UnknownHostException: db"
**Cause:** Application container cannot resolve database hostname

**Solution:**
```bash
# Ensure containers are on same network
docker-compose down
docker-compose up --build
```

### Issue: "Compilation error: cannot find symbol"
**Cause:** Maven cache or stale generated files

**Solution:**
```bash
mvn clean
mvn compile
```

### Issue: "Connection refused: localhost:8080"
**Cause:** Application not yet started or using different port

**Solution:**
```bash
# Check if container is running
docker-compose ps

# View logs
docker-compose logs app

# Check which port application is listening on
netstat -tlnp | grep java  # Linux/macOS
netstat -ano | findstr :8080  # Windows
```

---

## Database Schema

### Auto-Migration with Flyway

Flyway automatically runs SQL migrations from `src/main/resources/db/migration/` on application startup.

**Migration files:**
- `V1__create_url_mapping.sql` - Initial schema
- `V2__add_expiry_column.sql` - Add expiresAt column (example)

**Verify migration:**
```bash
# Connect to database
psql -U postgres -d url_shortener

# List tables
\dt

# Describe url_mapping table
\d url_mapping
```

### Example Schema
```sql
CREATE TABLE url_mapping (
  id BIGSERIAL PRIMARY KEY,
  short_code VARCHAR(7) NOT NULL UNIQUE,
  long_url TEXT NOT NULL,
  click_count BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_accessed_at TIMESTAMP,
  expires_at TIMESTAMP
);

CREATE INDEX idx_short_code ON url_mapping(short_code);
CREATE INDEX idx_long_url ON url_mapping(long_url);
```

---

## Development Workflow

### Local Development (without Docker)
```bash
# Start PostgreSQL
pg_ctl start

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Stop PostgreSQL
pg_ctl stop
```

### With Docker Compose
```bash
# Start all services
docker-compose up --build

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Remove data volumes
docker-compose down -v
```

### IDE Setup (IntelliJ IDEA / VS Code)
1. Open project root as Maven project
2. Configure JDK: Java 17
3. Import Maven dependencies: Maven → Reload
4. Run configurations:
   - **Run:** UrlShortenerApplication
   - **Test:** JUnit 5
5. Set environment variables in Run → Edit Configurations:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/url_shortener
   SPRING_DATASOURCE_PASSWORD=postgres
   ```

---

## Performance Tuning

### Database Indexing
Ensure indexes on frequently queried columns:
```sql
-- Already created in V1__create_url_mapping.sql
CREATE INDEX idx_short_code ON url_mapping(short_code);  -- Redirect lookups
CREATE INDEX idx_long_url ON url_mapping(long_url);      -- Deduplication
```

### Connection Pooling
Configure HikariCP in `application.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
```

### Batch Operations
Enable Hibernate batch inserts:
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

---

## Security Considerations

### 1. Input Validation
All DTOs use Jakarta Bean Validation:
- Long URLs validated as HTTP/HTTPS
- Custom aliases restricted to alphanumeric
- Expiry duration positive only

### 2. SQL Injection Prevention
- All queries use JPA parameterized queries (no string concatenation)
- Repository methods are type-safe

### 3. Rate Limiting (Future)
- Currently: no rate limiting
- Recommended: add API key + rate limit middleware

### 4. HTTPS (Production)
- Use reverse proxy (nginx, HAProxy) with SSL/TLS
- Set `Strict-Transport-Security` header

### 5. Database Security
- Change default PostgreSQL password
- Use network isolation (Docker default) or VPC
- Enable SSL connections between app and DB (future)

---

## Deployment

### Production Checklist
- [ ] Configure environment-specific `application-prod.properties`
- [ ] Set strong PostgreSQL password (secret management)
- [ ] Enable HTTPS with valid certificate
- [ ] Configure logging to centralized sink (ELK, CloudWatch)
- [ ] Set up database backups (daily incremental)
- [ ] Configure monitoring and alerts (Prometheus, Grafana)
- [ ] Enable request tracing (OpenTelemetry)
- [ ] Load testing and capacity planning
- [ ] Disaster recovery plan (RPO/RTO)

---

## Appendix: Project Structure

```
url-shortener-ai-assisted-v2/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/schwab/urlshortener/
│   │   │       ├── UrlShortenerApplication.java
│   │   │       ├── controller/
│   │   │       │   ├── UrlCreateController.java
│   │   │       │   ├── UrlRedirectController.java
│   │   │       │   └── AnalyticsController.java
│   │   │       ├── service/
│   │   │       │   ├── ShorteningService.java (interface)
│   │   │       │   ├── UrlShorteningService.java
│   │   │       │   ├── RedirectService.java (interface)
│   │   │       │   ├── UrlRedirectService.java
│   │   │       │   ├── AnalyticsService.java (interface)
│   │   │       │   ├── UrlAnalyticsService.java
│   │   │       │   ├── UrlShortenerFacade.java
│   │   │       │   ├── AliasValidator.java
│   │   │       │   └── LinkExpiryPolicy.java
│   │   │       ├── util/
│   │   │       │   └── ShortCodeGenerator.java
│   │   │       ├── model/
│   │   │       │   └── UrlMapping.java
│   │   │       ├── dto/
│   │   │       │   ├── ShortenRequest.java
│   │   │       │   ├── ShortenResponse.java
│   │   │       │   └── StatsResponse.java
│   │   │       ├── repository/
│   │   │       │   └── UrlMappingRepository.java
│   │   │       └── exception/
│   │   │           ├── ShortCodeNotFoundException.java
│   │   │           ├── UrlExpiredException.java
│   │   │           ├── DuplicateAliasException.java
│   │   │           └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           ├── V1__init_schema.sql
│   │           └── V2__add_expiry_to_url_mapping.sql
│   └── test/
│       ├── java/
│       │   └── com/schwab/urlshortener/
│       │       ├── ShortUrlServiceTest.java
│       │       └── ShortCodeGeneratorTest.java
│       └── resources/
│           ├── application.properties
│           └── application-test.properties
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
├── docs/
│   ├── AI_USAGE_TRACKER.md
│   ├── ANALYTICS_DECISION.md
│   ├── ARCHITECTURE.md
│   ├── ENGINEERING_SUMMARY.md
│   ├── SCENARIOS.md
│   ├── SETUP.md
│   └── TESTING.md
└── .gitignore
```

---

## Next Steps

1. Complete setup using Option 1 or 2 above
2. Test API endpoints using provided examples
3. Review [TESTING.md](TESTING.md) for test execution
4. Review [ARCHITECTURE.md](ARCHITECTURE.md) for system design
5. Deploy to production environment following deployment checklist
