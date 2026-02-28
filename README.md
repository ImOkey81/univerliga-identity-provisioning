# univerliga-identity-provisioning

Production-grade Spring Boot 3.3 service for async identity provisioning in Keycloak from CRM events.

## Stack

- Java 21
- Spring Boot 3.3
- Spring MVC + Validation + Actuator
- Spring Security Resource Server (JWT)
- Spring Data JPA + PostgreSQL
- Flyway
- RabbitMQ (default broker)
- Spring Retry-style processing with retries + DLQ
- Spring RestClient for Keycloak Admin API
- OpenAPI/Swagger UI

## Run locally

```bash
docker compose up --build
```

`docker-compose.yml` starts service with `PROVISIONING_MODE=broker`, so `/api/v1/mock/events` publishes to RabbitMQ and is processed asynchronously by consumer.

Endpoints:

- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Keycloak: http://localhost:8081
- RabbitMQ UI: http://localhost:15672 (guest/guest)

## Keycloak realm and clients

`realm-export.json` auto-imports realm `univerliga` with:

- Roles: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`
- Client `univerliga-gateway` (confidential)
- Client `univerliga-provisioning` (confidential, service account enabled)

Default users for password grant:

- `admin.user` / `admin123` (`ROLE_ADMIN`)
- `manager.user` / `manager123` (`ROLE_MANAGER`)
- `employee.user` / `employee123` (`ROLE_EMPLOYEE`)

## Get JWT token

Password grant example (`ROLE_ADMIN`):

```bash
export TOKEN=$(curl -s -X POST 'http://localhost:8081/realms/univerliga/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=univerliga-gateway' \
  -d 'client_secret=gateway-secret' \
  -d 'grant_type=password' \
  -d 'username=admin.user' \
  -d 'password=admin123' | jq -r .access_token)
```

Password grant example (`ROLE_MANAGER`):

```bash
export MANAGER_TOKEN=$(curl -s -X POST 'http://localhost:8081/realms/univerliga/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=univerliga-gateway' \
  -d 'client_secret=gateway-secret' \
  -d 'grant_type=password' \
  -d 'username=manager.user' \
  -d 'password=manager123' | jq -r .access_token)
```

## API examples

Create user manually:

```bash
curl -i -X POST 'http://localhost:8080/api/v1/provisioning/users' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "personId":"p_123",
    "username":"user123",
    "email":"u123@example.com",
    "displayName":"User 123",
    "departmentId":"d_1",
    "teamId":"t_1",
    "roles":["ROLE_EMPLOYEE"],
    "enabled":true
  }'
```

Inject mock CRM event:

```bash
curl -i -X POST 'http://localhost:8080/api/v1/mock/events' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId":"11111111-1111-1111-1111-111111111111",
    "type":"PersonCreated",
    "occurredAt":"2026-02-28T12:00:00Z",
    "source":"crm-service",
    "payload":{
      "personId":"p_124",
      "username":"user124",
      "email":"u124@example.com",
      "displayName":"User 124",
      "roles":["ROLE_EMPLOYEE"],
      "enabled":true
    }
  }'
```

Publish CRM event directly to RabbitMQ Management API:

```bash
curl -u guest:guest -X POST \
  'http://localhost:15672/api/exchanges/%2F/univerliga.crm.events/publish' \
  -H 'content-type: application/json' \
  -d '{
    "properties": {},
    "routing_key": "crm.person.created",
    "payload_encoding": "string",
    "payload": "{\"eventId\":\"22222222-2222-2222-2222-222222222222\",\"type\":\"PersonCreated\",\"occurredAt\":\"2026-02-28T12:00:00Z\",\"source\":\"crm-service\",\"payload\":{\"personId\":\"p_125\",\"username\":\"user125\",\"email\":\"u125@example.com\",\"displayName\":\"User 125\",\"roles\":[\"ROLE_EMPLOYEE\"],\"enabled\":true}}"
  }'
```

Get user status:

```bash
curl -s 'http://localhost:8080/api/v1/provisioning/users/p_123' \
  -H "Authorization: Bearer $TOKEN"
```

## Idempotency and DLQ

- Inbox/idempotency table: `processed_events(event_id, event_type, status, attempt_count, fail_count, error, processed_at)`
- If event already `PROCESSED`, repeated delivery is `IGNORED`
- Failed events are retried with backoff: 1s, 2s, 5s, 10s, 30s
- After retries exhausted, event payload is sent to `univerliga.provisioning.dlq`
- Mapping table `identity_links` stores `personId <-> keycloakUserId`, status, last error
- If `fail_count` reaches `CRM_MAX_FAIL_COUNT` (default 10), the same `eventId` is ignored

DLQ check:

1. Open `http://localhost:15672` (`guest/guest`).
2. Go to `Queues and Streams`.
3. Open `univerliga.provisioning.dlq` and inspect `Ready` messages / `Get messages`.

## Modes

- `app.provisioning.mode=mock-crm`: process injected events directly in service
- `app.provisioning.mode=broker`: consume from RabbitMQ queue `univerliga.provisioning.inbox`

Configured via env `PROVISIONING_MODE`.
