# Orders Service

## Purpose
Orchestrates checkout operations for the portfolio, validating user identities and catalog availability before persisting orders. The service proves how business processes can remain consistent across microservices by coordinating state changes and financial logic centrally.

## Technology Focus
- Spring Boot 3.2 with Data JPA, transactional boundaries, and schedulers to manage order lifecycles and periodic status audits.
- Cross-service validation implemented with WebClient to call Users and Catalog services, showcasing resilient synchronous integrations.
- Dedicated MySQL schema for order aggregates plus Kafka hooks for downstream analytics, highlighting event-driven readiness.
- Keycloak-issued JWT enforcement through OAuth2 Resource Server to guarantee only authorized actors confirm or inspect orders.
