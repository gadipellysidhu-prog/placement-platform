/**
 * k6 Load Test — Authentication Endpoints
 *
 * Tests: POST /auth/login, POST /auth/register, POST /auth/refresh
 *
 * Run:
 *   k6 run load-tests/auth-load-test.js
 *   k6 run --env BASE_URL=https://api.example.com load-tests/auth-load-test.js
 *   k6 run --out json=results/auth.json load-tests/auth-load-test.js
 *
 * Targets:
 *   P95 < 500ms
 *   Error rate < 1%
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL, THRESHOLDS, RAMP_STAGES } from './k6.config.js';

// Custom metrics
const loginSuccess    = new Counter('login_success');
const loginFailed     = new Counter('login_failed');
const registerSuccess = new Counter('register_success');
const loginDuration   = new Trend('login_duration', true);

export const options = {
    stages: RAMP_STAGES,
    thresholds: {
        ...THRESHOLDS,
        'login_duration': ['p(95)<500'],
    },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Pre-registered test account — seed before running
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'loadtest@placement.test';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'LoadTest@2024!';

export function setup() {
    // Register the test user once before the test
    const payload = JSON.stringify({
        fullName: 'Load Test User',
        email: TEST_EMAIL,
        password: TEST_PASSWORD,
        role: 'STUDENT',
    });
    const res = http.post(`${BASE_URL}/auth/register`, payload, { headers: JSON_HEADERS });
    // 201 = created, 409 = already exists — both acceptable for setup
    if (res.status !== 201 && res.status !== 409) {
        console.warn(`Setup registration returned ${res.status}: ${res.body}`);
    }
    return { email: TEST_EMAIL, password: TEST_PASSWORD };
}

export default function (data) {
    group('Login', () => {
        const payload = JSON.stringify({ email: data.email, password: data.password });
        const start   = Date.now();
        const res     = http.post(`${BASE_URL}/auth/login`, payload, { headers: JSON_HEADERS });
        const elapsed = Date.now() - start;

        loginDuration.add(elapsed);

        const ok = check(res, {
            'login: status 200':             (r) => r.status === 200,
            'login: has accessToken':        (r) => JSON.parse(r.body || '{}').accessToken !== undefined,
            'login: has refreshToken':       (r) => JSON.parse(r.body || '{}').refreshToken !== undefined,
            'login: response time < 500ms':  () => elapsed < 500,
        });

        if (ok) loginSuccess.add(1);
        else loginFailed.add(1);

        // Use the refresh token for a follow-up call
        if (res.status === 200) {
            const tokens = JSON.parse(res.body);
            group('Token Refresh', () => {
                const refreshPayload = JSON.stringify({ refreshToken: tokens.refreshToken });
                const refreshRes = http.post(`${BASE_URL}/auth/refresh`, refreshPayload, { headers: JSON_HEADERS });
                check(refreshRes, {
                    'refresh: status 200':          (r) => r.status === 200,
                    'refresh: has new accessToken': (r) => JSON.parse(r.body || '{}').accessToken !== undefined,
                });
            });
        }

        sleep(1);
    });

    group('Invalid Login (bad password)', () => {
        const payload = JSON.stringify({ email: data.email, password: 'wrong-password' });
        const res     = http.post(`${BASE_URL}/auth/login`, payload, { headers: JSON_HEADERS });
        check(res, {
            'bad login: status 401': (r) => r.status === 401,
        });
        sleep(0.5);
    });
}

export function teardown(data) {
    console.log(`Login success: ${loginSuccess.name}, failed: ${loginFailed.name}`);
}
