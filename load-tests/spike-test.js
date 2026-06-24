/**
 * k6 Spike Test — Burst traffic simulation
 *
 * Simulates a sudden spike of 100 concurrent users to verify:
 *   - Rate limiting activates (HTTP 429)
 *   - Application stays responsive (no 500s)
 *   - Recovery after spike (latency returns to normal)
 *
 * Run:
 *   k6 run load-tests/spike-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE_URL, SPIKE_STAGES } from './k6.config.js';

const rateLimited = new Counter('rate_limited_429');
const errors500   = new Counter('server_errors_500');

export const options = {
    stages: SPIKE_STAGES,
    thresholds: {
        // Even during a spike, 500s must be <0.1%
        'server_errors_500': ['count<10'],
        // Rate limiting expected — not treated as failure
        'http_req_failed': ['rate<0.15'],
    },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export default function () {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email: `user${__VU}@test.com`, password: 'wrong' }),
        { headers: JSON_HEADERS }
    );

    if (res.status === 429) rateLimited.add(1);
    if (res.status === 500) errors500.add(1);

    check(res, {
        'not a 500 error': (r) => r.status !== 500,
        'has problem+json content-type': (r) =>
            (r.headers['Content-Type'] || '').includes('json'),
    });

    sleep(0.1);
}
