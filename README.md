# access-pass-service

A backend for physical access control with QR passes. Staff issue passes to people (holders) under a policy that
says which gates they may use, at what time of day and how many times. The service returns a compact signed token
and a QR image of it. Turnstiles and door controllers (gate devices) post scanned tokens to a scan endpoint and get a
`GRANTED` or `DENIED` decision with a reason. Every scan, granted or denied, is written to an append-only audit log.

Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, PostgreSQL, Flyway, ZXing, Testcontainers.

## How a scan is decided

```
gate device --> POST /api/v1/scans  X-Device-Key: gdk_...  {"token": "AP1.k1.<payload>.<sig>"}
   |
   +- device key filter      SHA-256(key) looked up in gate_device; the gate comes from the device, not the request
   +- rate limiter           token bucket per device, 429 when empty
   +- token verification     version, key id, HMAC-SHA256 (constant-time compare), then expiry
   +- pass rules             claims match stored pass, not revoked, holder active, inside validity period,
   |                         gate allowed by policy, inside the daily window (site time zone), uses left
   +- consume one use        single conditional UPDATE ... WHERE use_count < max_uses
   +- audit row              same transaction as the consume, so a grant is never unaudited
```

Deny reasons: `MALFORMED_TOKEN`, `UNKNOWN_SIGNING_KEY`, `INVALID_SIGNATURE`, `EXPIRED`, `UNKNOWN_PASS`,
`TOKEN_MISMATCH`, `REVOKED`, `HOLDER_INACTIVE`, `NOT_YET_VALID`, `GATE_NOT_ALLOWED`, `OUTSIDE_TIME_WINDOW`,
`USES_EXHAUSTED`, `CONCURRENT_SCAN`.

## Pass token format

```
AP1.<keyId>.<base64url(passId|holderId|expiryEpochSeconds|nonce)>.<base64url(HMAC-SHA256)>
```

The MAC covers everything before the last dot, including the version and key id, so a token cannot be relabelled to
another key. The key id selects the verification key from a configured key ring, which is what makes rotation
possible: add a new key, make it active, keep the old one until the passes signed with it have expired, then remove
it. Signing is deterministic, so the QR endpoint can regenerate the image from stored pass data without storing the
token itself.

## Threat model

Defended:

- **Forged or altered passes.** Tokens are HMAC-SHA256 signed with a server-side key of at least 256 bits; the
  signature is compared with `MessageDigest.isEqual` and checked before the payload is parsed or the expiry is read.
  Non-canonical base64 is rejected so one signature has exactly one valid encoding.
- **Replay of single-use and limited passes.** Use counting is a single conditional `UPDATE` on the pass row. Under
  concurrent scans the row lock serialises the updates and the losing transactions match zero rows, so two
  simultaneous scans of a single-use pass cannot both be granted. A check constraint (`use_count <= max_uses`)
  backs this up in the database. Covered by concurrent tests on H2 and on real PostgreSQL.
- **Stolen or copied QR after revocation.** Revocation is checked on every scan; tokens carry no authority on their
  own beyond identifying the pass.
- **A device scanning for a gate it is not mounted at.** The gate is bound to the device credential server-side.
- **Leaked database.** Device API keys are stored as SHA-256 hashes (they are 256-bit random values, so a fast hash
  is sufficient and allows a unique-index lookup). Staff passwords use BCrypt. Pass tokens are not stored.
- **Credential confusion between staff and devices.** Two separate security filter chains: a device key is not
  accepted on staff endpoints and a staff JWT is not accepted on the scan endpoint.
- **Username enumeration through timing.** Unknown users still pay for a BCrypt comparison, and every login failure
  returns the same message.
- **Scan flooding from a compromised device.** Per-device token bucket rate limiting.
- **Secrets in logs.** Raw tokens and API keys are never logged; logs and the audit log carry a 64-bit SHA-256
  fingerprint of the token so repeated attempts with the same token can be correlated.
- **Audit tampering through the application.** The JPA entity is immutable and its repository exposes no update or
  delete. In PostgreSQL, triggers reject `UPDATE`, `DELETE` and `TRUNCATE` on `scan_audit`.

Out of scope:

- Screenshotting or photographing a valid QR and presenting it before the legitimate holder. Multi-use passes can be
  shared; single-use passes cannot be used twice, but whoever scans first wins. Defeating this needs holder-bound
  factors (rotating TOTP QR in a mobile app, NFC, a PIN or a guard).
- Physical attacks on the gate device or tailgating.
- A database superuser disabling the audit triggers.
- Brute-force protection on the staff login endpoint (expected to sit behind a gateway or WAF that throttles it).
- Distributed rate limiting. The token bucket is in-memory and per instance; it sits behind the `RateLimiter`
  interface so a Redis-backed implementation can replace it.
- Signing key storage in a KMS or HSM. Keys are read from configuration (environment variables).
- Offline operation of gate devices.

## Running locally

With Docker:

```bash
docker compose up --build
```

This starts PostgreSQL and the service on `http://localhost:8080` with development-only secrets and a bootstrap admin
(`admin` / `local-dev-admin-password`). Swagger UI is at `http://localhost:8080/swagger-ui.html`.

Without Docker, start a PostgreSQL 16 database and run:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/access_pass
export ACCESSPASS_TOKENS_ACTIVEKEYID=k1
export ACCESSPASS_TOKENS_KEYS_K1=$(openssl rand -base64 32)
export ACCESSPASS_JWT_SECRET=$(openssl rand -base64 32)
export ACCESSPASS_BOOTSTRAPADMIN_USERNAME=admin
export ACCESSPASS_BOOTSTRAPADMIN_PASSWORD='a-long-local-password'
./mvnw spring-boot:run
```

The service refuses to start without a signing key ring and a JWT secret, and rejects keys shorter than 32 bytes.

### A full round trip

```bash
TOKEN=$(curl -s localhost:8080/api/v1/auth/token -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"local-dev-admin-password"}' | jq -r .accessToken)
AUTH="Authorization: Bearer $TOKEN"
JSON='Content-Type: application/json'

curl -s localhost:8080/api/v1/sites -H "$AUTH" -H "$JSON" -d '{"name":"Main campus","timeZone":"Asia/Baku"}'
curl -s localhost:8080/api/v1/sites/1/gates -H "$AUTH" -H "$JSON" -d '{"code":"NORTH-1","name":"North entrance"}'
curl -s localhost:8080/api/v1/sites/1/policies -H "$AUTH" -H "$JSON" \
  -d '{"name":"Visitor","gateIds":[1],"windowStart":"08:00","windowEnd":"18:00","maxUses":1}'
curl -s localhost:8080/api/v1/holders -H "$AUTH" -H "$JSON" -d '{"fullName":"Aysel Mammadova"}'

DEVICE_KEY=$(curl -s localhost:8080/api/v1/gates/1/devices -H "$AUTH" -H "$JSON" \
  -d '{"name":"Turnstile A"}' | jq -r .apiKey)
PASS=$(curl -s localhost:8080/api/v1/passes -H "$AUTH" -H "$JSON" \
  -d "{\"holderId\":1,\"policyId\":1,\"validUntil\":\"$(date -u -d '+1 day' +%FT%TZ)\"}")

curl -s localhost:8080/api/v1/passes/$(echo "$PASS" | jq -r .passId)/qr -H "$AUTH" -o pass.png
curl -s localhost:8080/api/v1/scans -H "X-Device-Key: $DEVICE_KEY" -H "$JSON" \
  -d "{\"token\":\"$(echo "$PASS" | jq -r .token)\"}"
```

## API overview

All errors are RFC 7807 `application/problem+json`.

| Method | Path | Auth |
|---|---|---|
| POST | `/api/v1/auth/token` | none (username and password) |
| POST, GET | `/api/v1/staff`, `POST /api/v1/staff/{id}/disable` | ADMIN |
| POST, GET | `/api/v1/sites`, `/api/v1/sites/{id}` | write ADMIN, read ADMIN or OPERATOR |
| POST, GET | `/api/v1/sites/{id}/gates` | write ADMIN, read ADMIN or OPERATOR |
| POST, GET | `/api/v1/sites/{id}/policies`, `GET /api/v1/policies/{id}` | write ADMIN, read ADMIN or OPERATOR |
| POST, GET | `/api/v1/gates/{id}/devices` | write ADMIN, read ADMIN or OPERATOR |
| POST | `/api/v1/devices/{id}/rotate-key`, `/api/v1/devices/{id}/deactivate` | ADMIN |
| POST, GET | `/api/v1/holders`, `/api/v1/holders/{id}`, `/{id}/activate`, `/{id}/deactivate` | ADMIN or OPERATOR |
| POST | `/api/v1/passes` | ADMIN or OPERATOR |
| GET | `/api/v1/passes?holderId=`, `/api/v1/passes/{id}` | ADMIN or OPERATOR |
| POST | `/api/v1/passes/{id}/revoke` | ADMIN or OPERATOR |
| GET | `/api/v1/passes/{id}/qr?size=320` | ADMIN or OPERATOR |
| GET | `/api/v1/audit/scans?gateId=&deviceId=&passId=&holderId=&outcome=&reason=&from=&to=&page=&size=` | ADMIN or OPERATOR |
| POST | `/api/v1/scans` | gate device (`X-Device-Key`) |

Responses that contain a credential (issued pass token, device API key, staff JWT) are sent with
`Cache-Control: no-store`. Device API keys are shown once, at registration or rotation.

## Configuration

| Property | Environment variable | Default |
|---|---|---|
| `access-pass.tokens.active-key-id` | `ACCESSPASS_TOKENS_ACTIVEKEYID` | required |
| `access-pass.tokens.keys.<id>` | `ACCESSPASS_TOKENS_KEYS_<ID>` | required, base64, at least 32 bytes |
| `access-pass.jwt.secret` | `ACCESSPASS_JWT_SECRET` | required, base64, at least 32 bytes |
| `access-pass.jwt.ttl` | `ACCESSPASS_JWT_TTL` | `15m` |
| `access-pass.scan.rate-limit.capacity` | `ACCESSPASS_SCAN_RATELIMIT_CAPACITY` | `10` |
| `access-pass.scan.rate-limit.refill-per-second` | `ACCESSPASS_SCAN_RATELIMIT_REFILLPERSECOND` | `5` |
| `access-pass.bootstrap-admin.username` / `.password` | `ACCESSPASS_BOOTSTRAPADMIN_USERNAME` / `_PASSWORD` | unset |
| `spring.datasource.url` | `DATABASE_URL` | `jdbc:postgresql://localhost:5432/access_pass` |

The bootstrap admin is only created when no staff account exists yet.

## Design decisions

- **HMAC instead of JWT or asymmetric signatures for passes.** Only this service issues and verifies passes, so a
  shared secret is enough, and an HMAC token is far shorter than a JWT or an ECDSA signature, which keeps the QR
  code small and easy to scan. If gates ever need to verify offline, the token format has a version prefix so an
  Ed25519 variant can be introduced without breaking existing passes.
- **Conditional UPDATE over optimistic-lock retries.** The scan path needs one answer quickly, not a retry loop. The
  `UPDATE ... WHERE use_count < max_uses` is atomic in one round trip and relies on row locking that every
  relational database provides. The pass entity still carries a `@Version` column, bumped by the same update, so a
  concurrent admin edit cannot silently overwrite a scan. Any lock failure the database reports is turned into a
  `CONCURRENT_SCAN` denial and audited in a fresh transaction.
- **Denials are 200 responses.** A denied scan is a normal business outcome for the device, which needs the reason
  to show it. HTTP errors are kept for problems with the request itself (401, 400, 429).
- **Audit pass id has no foreign key.** A correctly signed token for a pass that does not exist points to a leaked
  signing key, and that is exactly the case worth recording, so `pass_id` and `holder_id` are stored as given.
- **Package by feature.** `site`, `holder`, `policy`, `pass`, `token`, `device`, `staff`, `scan`, `ratelimit`,
  `security`, `common`. Entities keep their invariants; services own transactions; controllers map DTO records.
- **Flyway per vendor.** Shared migrations live in `db/migration/common`; PostgreSQL-only objects (the audit
  triggers) live in `db/migration/postgresql`. Fast tests run the shared migrations on H2 in PostgreSQL mode.

## Tests

```bash
./mvnw verify
```

- Unit tests: token signing and verification (tampering, wrong or retired key id, relabelled key id, expiry,
  rotation, malformed input), every scan deny reason against a mocked repository, daily windows across midnight,
  the token bucket with a fake clock.
- Integration tests on H2: both authentication mechanisms and role rules, the scan API end to end including QR
  decoding, audit queries, rate limiting, and concurrent scans of single-use and multi-use passes.
- Testcontainers tests on PostgreSQL 16 (`*IT`, run by Failsafe): the concurrency guarantees under real row locking
  and the append-only audit triggers. They are skipped automatically when Docker is not available.
