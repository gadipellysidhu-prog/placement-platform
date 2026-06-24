// Shared k6 configuration — imported by individual test scripts
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const THRESHOLDS = {
    // P95 must be under 500ms for all HTTP requests
    'http_req_duration': ['p(95)<500'],
    // P99 under 1 second
    'http_req_duration{expected_response:true}': ['p(99)<1000'],
    // Error rate under 1%
    'http_req_failed': ['rate<0.01'],
};

// Ramp-up → steady state → ramp-down
export const RAMP_STAGES = [
    { duration: '30s', target: 10 },   // warm up
    { duration: '1m',  target: 50 },   // ramp to 50 VUs
    { duration: '3m',  target: 50 },   // hold
    { duration: '30s', target: 0 },    // ramp down
];

// Spike test stages
export const SPIKE_STAGES = [
    { duration: '10s', target: 5 },
    { duration: '1m',  target: 100 },  // spike
    { duration: '10s', target: 5 },
    { duration: '30s', target: 0 },
];
