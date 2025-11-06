# Orders Service

Coordinates orders across users and catalog services. Performs cross-service validation with WebClient, handles monetary calculations, and emits scheduled metrics.

## Commands

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

## Environment

- `SPRING_DATASOURCE_URL`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`
- `SERVICES_USERS_BASE_URL`
- `SERVICES_CATALOG_BASE_URL`
