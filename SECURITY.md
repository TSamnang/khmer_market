# Khmer Market - Application Security & ASVS Level 1 Compliance Report

## 1. Executive Summary & Security Posture
Khmer Market is designed following the **OWASP Application Security Verification Standard (ASVS) Level 1** benchmark and Defense-in-Depth principles. The marketplace implements strict access controls, cryptographic integrity checks, brute-force mitigation, tamper-proof state machines, and multi-tier rate limiting.

---

## 2. OWASP ASVS v4.0.3 Level 1 Verification Matrix

| ASVS Domain | Control Description | Implementation in Khmer Market | Status |
| :--- | :--- | :--- | :--- |
| **V1: Architecture** | Dual Security Filter Chains | Isolated filter chain for `/admin/**` requiring `ROLE_ADMIN` and separate storefront filter chain. | **VERIFIED** |
| **V2: Authentication** | Password Strength & Blacklist | Offline check of 55+ common/breached passwords via [PasswordPolicyValidator](file:///src/main/java/com/example/khmer_market/security/PasswordPolicyValidator.java). Minimum 12 characters. | **VERIFIED** |
| **V2: Authentication** | Password Storage | DelegatingPasswordEncoder using `BCrypt` with work factor 12. | **VERIFIED** |
| **V2: Authentication** | Lockout Defense | Account locked for 15 minutes after 5 consecutive failed login attempts via [LoginAttemptService](file:///src/main/java/com/example/khmer_market/security/LoginAttemptService.java). | **VERIFIED** |
| **V3: Session Management** | Session Fixation & Hijacking | `sessionFixation().migrateSession()`, `httpOnly=true`, `SameSite=Lax`, concurrent session cap of 3. | **VERIFIED** |
| **V4: Access Control** | Role-Based Authorization | Dual check via URL filter chains and `@PreAuthorize("hasRole('...')")`. Administrative privilege separation. | **VERIFIED** |
| **V5: Malicious Input** | SQL Injection & SSRF | Parameterized Spring Data JPA queries only; zero dynamic SQL string concatenations. | **VERIFIED** |
| **V5: Malicious Input** | Cross-Site Scripting (XSS) | Thymeleaf automatic HTML escaping (`th:text`); strict Content Security Policy (`script-src 'self'`). Zero inline scripts. | **VERIFIED** |
| **V7: Error Handling & Logging** | Information Disclosure | `server.error.include-stacktrace: never`; generic error pages for 400, 403, 404, 429, 500 without internal leaks. | **VERIFIED** |
| **V8: Data Protection** | CSRF Protection | Enabled across all state-changing endpoints with CSRF token validation and meta headers for AJAX. | **VERIFIED** |
| **V9: Communications** | Security Headers & CSP | Strict CSP directives (`frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`). | **VERIFIED** |
| **V10: Malicious Code** | Zero CDNs & Third-Party Dependencies | All CSS, JS (Bootstrap 5.3, Bootstrap Icons, Chart.js 4.4) bundled locally. Zero external CDN calls. | **VERIFIED** |
| **V11: Business Logic** | Inventory Concurrency Defense | Pessimistic write locking on `inventory` ordered by `package_id` to eliminate deadlocks and race conditions. | **VERIFIED** |
| **V11: Business Logic** | Checkout Replay & Double-Spend | Single-use checkout cryptographic tokens (`UUID`) invalidated immediately upon submission. | **VERIFIED** |
| **V12: File Uploads** | Magic Bytes & Traversal Defense | Apache Tika magic-bytes MIME detection (JPEG, PNG, WebP only). Cryptographic UUID filenames. Canonicalized path verification. | **VERIFIED** |
| **V13: API & Web Service** | Webhook HMAC Authentication | Payment webhook requests verified using `HmacSHA256` constant-time comparison against `PAYMENT_WEBHOOK_SECRET`. | **VERIFIED** |

---

## 3. Threat Modeling & Mitigation Matrix (STRIDE)

| Threat Category | Risk Scenario | Architectural Defense & Mitigation |
| :--- | :--- | :--- |
| **Spoofing** | Attacker impersonates payment gateway or another user | Webhook authenticated via HMAC-SHA256 signature; user identity bound to Spring Security `AuthenticationPrincipal`. |
| **Tampering** | Buyer alters price or inventory reserved in browser | Prices and totals computed exclusively on the server from the database; client-supplied prices are strictly ignored. |
| **Repudiation** | Admin denies performing critical moderation | Dedicated [AuditService](file:///src/main/java/com/example/khmer_market/service/AuditService.java) writing structured JSON/KV events with timestamp, admin ID, and client IP. |
| **Information Disclosure** | Error trace reveals database schema | Spring Boot production error profile suppresses stacktraces, exceptions, and schema details. |
| **Denial of Service** | Brute force or API flooding | In-memory Bucket4j rate limiter backed by Caffeine with per-route buckets and 429 Retry-After responses. |
| **Elevation of Privilege** | Buyer accessing admin console | URL security matcher strictly requires `ROLE_ADMIN`; dual SecurityFilterChain enforces isolated login realm. |

---

## 4. Payment Webhook Security & Verification Specification
* **Endpoint**: `POST /api/payments/khqr/webhook`
* **Signature Header**: `X-Signature`
* **Algorithm**: `HmacSHA256` computed over the raw request payload using shared secret `PAYMENT_WEBHOOK_SECRET`.
* **Verification Logic**: Uses `MessageDigest.isEqual` to prevent timing attacks.
* **Idempotency**: Repeat webhook hits for already-confirmed orders return `200 OK` without duplicate processing.

---

## 5. Vulnerability Disclosure Policy
Security concerns or vulnerability reports should be directed to `security@khmermarket.gov.kh`. Critical patches are triaged and applied within 24 hours.
