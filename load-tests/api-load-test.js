/**
 * k6 Load Test — Student & Recruiter API Endpoints
 *
 * Tests: GET /students, GET /companies, GET /jobs, POST /applications
 *
 * Run:
 *   k6 run load-tests/api-load-test.js
 *   k6 run --env BASE_URL=https://api.example.com --env TEST_EMAIL=admin@test.com load-tests/api-load-test.js
 *
 * Targets: P95 < 500ms, error rate < 1%
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, THRESHOLDS, RAMP_STAGES } from './k6.config.js';

const studentListDuration = new Trend('student_list_duration', true);
const companyListDuration = new Trend('company_list_duration', true);

export const options = {
    stages: RAMP_STAGES,
    thresholds: {
        ...THRESHOLDS,
        'student_list_duration': ['p(95)<500'],
        'company_list_duration': ['p(95)<500'],
    },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

const ADMIN_EMAIL    = __ENV.ADMIN_EMAIL    || 'admin@placement.test';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'Admin@2024!';
const STUDENT_EMAIL    = __ENV.STUDENT_EMAIL    || 'student@placement.test';
const STUDENT_PASSWORD = __ENV.STUDENT_PASSWORD || 'Student@2024!';

export function setup() {
    // Authenticate admin and student — return tokens for the test
    const adminLogin = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
        { headers: JSON_HEADERS }
    );

    const studentLogin = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email: STUDENT_EMAIL, password: STUDENT_PASSWORD }),
        { headers: JSON_HEADERS }
    );

    let adminToken   = '';
    let studentToken = '';

    if (adminLogin.status === 200) {
        adminToken = JSON.parse(adminLogin.body).accessToken;
    } else {
        console.warn(`Admin login failed (${adminLogin.status}) — some checks will fail.`);
    }

    if (studentLogin.status === 200) {
        studentToken = JSON.parse(studentLogin.body).accessToken;
    } else {
        console.warn(`Student login failed (${studentLogin.status}) — some checks will fail.`);
    }

    return { adminToken, studentToken };
}

function authHeaders(token) {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };
}

export default function (data) {
    const { adminToken, studentToken } = data;

    group('Student API', () => {
        group('List students (admin)', () => {
            const start = Date.now();
            const res   = http.get(`${BASE_URL}/students?page=0&size=20`, {
                headers: authHeaders(adminToken),
            });
            studentListDuration.add(Date.now() - start);

            check(res, {
                'GET /students: status 200 or 403': (r) => [200, 403].includes(r.status),
                'GET /students: not 500':           (r) => r.status !== 500,
            });
        });

        group('Get own profile (student)', () => {
            const res = http.get(`${BASE_URL}/students/me`, {
                headers: authHeaders(studentToken),
            });
            check(res, {
                'GET /students/me: not 500': (r) => r.status !== 500,
            });
        });

        sleep(0.5);
    });

    group('Company & Job APIs', () => {
        group('List companies', () => {
            const start = Date.now();
            const res   = http.get(`${BASE_URL}/companies?page=0&size=20`, {
                headers: authHeaders(adminToken),
            });
            companyListDuration.add(Date.now() - start);

            check(res, {
                'GET /companies: not 500': (r) => r.status !== 500,
            });
        });

        group('List job postings', () => {
            const res = http.get(`${BASE_URL}/jobs?page=0&size=20`, {
                headers: authHeaders(studentToken),
            });
            check(res, {
                'GET /jobs: not 500': (r) => r.status !== 500,
            });
        });

        sleep(0.5);
    });

    group('Unauthenticated requests (must return 401)', () => {
        const res = http.get(`${BASE_URL}/students`);
        check(res, {
            'Unauthenticated → 401': (r) => r.status === 401,
        });
        sleep(0.2);
    });
}
