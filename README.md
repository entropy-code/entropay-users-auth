# entropay-users-auth

Identity service for the Entroteam platform. Manages authentication via AWS Cognito and issues JWTs consumed by all platform services.

- **Java 21**
- **Spring Boot 3.4.4**
- **Port: 8000**

## Tech Stack

- Spring Boot 3.4.4 (Web, Security, OAuth2 Client)
- Java 21
- PostgreSQL with Flyway-managed schema
- AWS Cognito as the upstream identity provider
- Maven for builds, Docker for containerization

## Development

```bash
# Start database
docker-compose up -d postgres pgweb

# Run application
./mvnw spring-boot:run

# Run tests
./mvnw test
```

Or using the Makefile:

```bash
make setup   # Start database and build
make run     # Run on http://localhost:8000
make test    # Run all tests
```

- App: http://localhost:8000
- pgweb (database UI): http://localhost:8081

## Configuration

Sensitive configuration (DB credentials, Cognito client ID/secret, redirect URIs) lives in environment variables or `local.env`. Never hardcode.

Key properties:
- `server.port` — default 8000
- `spring.datasource.url` — PostgreSQL JDBC URL
- `spring.security.oauth2.client.*` — Cognito OAuth2 client config

See the meta-repo's `COGNITO.md` for Cognito pool IDs and environment-specific values.
