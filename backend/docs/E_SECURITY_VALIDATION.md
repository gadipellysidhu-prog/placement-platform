# E. Security Validation Guide

How to validate JWT security, authorization, file scanning, and OWASP findings.

---

## 1. JWT Security Validation

### 1.1 Token generation and validation

Run the JWT validation test suite:
```powershell
cd backend
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=JwtValidationTest
```

This validates:
- ✅ Valid token accepted (200 OK)
- ✅ Expired token rejected (401)
- ✅ Token with wrong RSA key rejected (401) — signature verification
- ✅ Token with wrong issuer rejected (401) — issuer validation
- ✅ Token with wrong audience rejected (401) — audience validation
- ✅ Malformed token rejected (401)
- ✅ Empty string rejected (401)

### 1.2 Manual JWT testing

**Step 1:** Register and get a token:
```powershell
$body = '{"email":"test@example.com","password":"password123","role":"ROLE_STUDENT"}'
$r = Invoke-RestMethod http://localhost:8081/auth/register -Method POST -ContentType "application/json" -Body $body
$token = $r.accessToken
```

**Step 2:** Decode the token (paste at https://jwt.io):
- Verify `iss` (issuer) = `placement-platform`
- Verify `aud` (audience) = `placement-api`
- Verify `role` claim is present
- Verify `exp` is ~15 minutes in the future

**Step 3:** Test with tampered token:
```powershell
$tampered = $token.Substring(0, $token.Length - 5) + "XXXXX"
Invoke-RestMethod http://localhost:8081/api/users/me -Headers @{Authorization="Bearer $tampered"}
```
Expected: `401 Unauthorized`

**Step 4:** Test with expired token (set system clock forward, or use test):
```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=JwtValidationTest#expiredAccessToken_returns401
```

### 1.3 Refresh token rotation

```powershell
# Get initial tokens
$r = Invoke-RestMethod http://localhost:8081/auth/login -Method POST -ContentType "application/json" `
  -Body '{"email":"test@example.com","password":"password123"}'
$refresh1 = $r.refreshToken

# Rotate
$r2 = Invoke-RestMethod http://localhost:8081/auth/refresh -Method POST -ContentType "application/json" `
  -Body ('{"refreshToken":"' + $refresh1 + '"}')
$refresh2 = $r2.refreshToken

# Old token must be revoked
Invoke-RestMethod http://localhost:8081/auth/refresh -Method POST -ContentType "application/json" `
  -Body ('{"refreshToken":"' + $refresh1 + '"}')
# Expected: 401 Unauthorized
```

---

## 2. Authorization Validation

### 2.1 Run the ownership test suite
```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=AuthorizationOwnershipTest
```

Validates:
- ✅ Student cannot apply using another student's ID → 403
- ✅ Student cannot withdraw another student's application → 403
- ✅ Student cannot submit certificate for another student → 403
- ✅ Student can use own ID → 201/200

### 2.2 Run the security integration suite
```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=SecurityIntegrationTest
```

Validates:
- ✅ Security headers (X-Frame-Options, CSP, Referrer-Policy, Permissions-Policy)
- ✅ Student accessing admin endpoint → 403
- ✅ Admin accessing admin endpoint → 200
- ✅ Actuator metrics without auth → 401
- ✅ Actuator metrics with student token → 403
- ✅ Actuator metrics with admin token → 200
- ✅ Brute-force: 5 failed logins → account locked
- ✅ Locked account → 423 Locked

### 2.3 Vertical privilege escalation matrix

| Endpoint | STUDENT | PLACEMENT_OFFICER | ADMIN |
|----------|---------|-------------------|-------|
| `GET /api/students/me` | ✅ 200 | ✅ 200 (hierarchy) | ✅ 200 |
| `GET /api/students` | ❌ 403 | ✅ 200 | ✅ 200 |
| `POST /api/companies` | ❌ 403 | ✅ 201 | ✅ 201 |
| `POST /api/companies/{id}/blacklist` | ❌ 403 | ❌ 403 | ✅ 200 |
| `GET /actuator/metrics` | ❌ 403 | ❌ 403 | ✅ 200 |
| `POST /api/applications` | ✅ 201 (own) | ✅ 201 | ✅ 201 |

---

## 3. File Scanning Validation

### 3.1 Run file scanning tests
```powershell
.m2\apache-maven-3.9.6\bin\mvn.cmd test -Dtest=ClamAvVirusScanTest
```

Validates:
- ✅ INFECTED file → upload returns 422, file quarantined in DB
- ✅ Quarantined file download → 403
- ✅ FAILED scan (ClamAV down) → upload succeeds with status FAILED, not quarantined
- ✅ CLEAN scan → upload returns 201 with `scanStatus: CLEAN`
- ✅ EICAR payload simulation → detected and rejected

### 3.2 Manual ClamAV test (with running ClamAV)

Start dev stack with ClamAV:
```powershell
docker-compose -f docker-compose.dev.yml up -d
```

Upload EICAR test file:
```powershell
$eicar = "X5O!P%@AP[4\PZX54(P^)7CC)7}`$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!`$H+H*"
$bytes = [System.Text.Encoding]::ASCII.GetBytes($eicar)
[System.IO.File]::WriteAllBytes("eicar.com", $bytes)

# Override scan-enabled=true for this manual test
# (in test mode it's disabled — use dev mode)
```

Then upload via curl:
```powershell
curl -X POST http://localhost:8081/api/files/upload `
  -H "Authorization: Bearer $token" `
  -F "file=@eicar.com;type=application/pdf"
```
Expected: `422 Unprocessable Entity` with virus-detected message.

### 3.3 Verify quarantine in database
```powershell
# Connect to dev PostgreSQL
docker exec -it placement-postgres psql -U placement_user -d placement_dev `
  -c "SELECT id, filename, scan_status, quarantined FROM file_scan_records WHERE quarantined = true;"
```

---

## 4. OWASP Dependency Check

### 4.1 Run the scan
```powershell
cd backend
.m2\apache-maven-3.9.6\bin\mvn.cmd dependency-check:check
```

The build is configured to **fail on CVSS score ≥ 7** (High/Critical).

### 4.2 View HTML report
```powershell
Start-Process "target\dependency-check-report.html"
```

### 4.3 Review findings

For each finding:
1. Check CVE description at https://nvd.nist.gov/vuln/detail/CVE-XXXX-XXXXX
2. Check if the vulnerable code path is actually reachable in this application
3. Either upgrade the dependency or document it as an accepted risk

### 4.4 Accepted risk documentation format

Create `docs/OWASP_ACCEPTED_RISKS.md`:
```markdown
## CVE-XXXX-XXXXX
- **Severity:** Medium (CVSS 6.5)
- **Dependency:** some-lib-1.2.3
- **Why accepted:** The vulnerable endpoint is not exposed; we do not use X feature
- **Mitigation:** Blocked at Nginx layer; monitoring alert in place
- **Review date:** 2025-12-01
```

---

## 5. Security headers verification

```powershell
$response = Invoke-WebRequest http://localhost:8081/actuator/health
$response.Headers["X-Content-Type-Options"]   # nosniff
$response.Headers["X-Frame-Options"]           # DENY
$response.Headers["Referrer-Policy"]           # strict-origin-when-cross-origin
$response.Headers["Permissions-Policy"]        # camera=(), microphone=(), ...
$response.Headers["Content-Security-Policy"]   # default-src 'self'; frame-ancestors 'none'
```

---

## 6. Rate limiting validation (dev mode only)

The test profile disables rate limiting. To test in dev:

```powershell
# 6 rapid login attempts — 6th should be rate limited
for ($i = 0; $i -lt 6; $i++) {
  Invoke-RestMethod http://localhost:8081/auth/login -Method POST `
    -ContentType "application/json" `
    -Body '{"email":"x@x.com","password":"wrong"}'
}
```

Expected: first 5 return 401, 6th (or at configured limit) returns 429 Too Many Requests.
