import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;

// 테스트할 게임 ID
const USER_ID = __ENV.USER_ID || '1';

export const options = {
    stages: [
        { duration: '10s', target: 5 },
        { duration: '30s', target: 10 },
        { duration: '30s', target: 20 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],

        'http_req_duration{api:library}': ['p(95)<1000'],
        'http_req_duration{api:profile}': ['p(95)<1000'],
        'http_req_duration{api:recent-games}': ['p(95)<1000'],
        'http_req_duration{api:public-games}': ['p(95)<1000'],
    },
};

const authHeaders = {
    headers: {
        Authorization: `Bearer ${ACCESS_TOKEN}`,
        'Content-Type': 'application/json',
    },
};

export default function () {
    // 1. 내 라이브러리 조회
    let res = http.get(
        `${BASE_URL}/api/v1/library/games?page=0&size=10`,
        {
            ...authHeaders,
            tags: { api: 'library' },
        }
    );

    check(res, {
        'library list - status 200': (r) => r.status === 200,
    });

    sleep(0.5);

    // 2. 내 프로필 조회
    res = http.get(
        `${BASE_URL}/api/v1/library/games/profile`,
        {
            ...authHeaders,
            tags: { api: 'profile' },
        }
    );

    check(res, {
        'my profile - status 200': (r) => r.status === 200,
    });

    sleep(0.5);

    // 3. 공개 프로필 게임 조회
    res = http.get(
        `${BASE_URL}/api/v1/library/games/profile/${USER_ID}/games?page=0&size=10`,
        {
            tags: { api: 'public-games' },
        }
    );

    check(res, {
        'public games - status 200': (r) => r.status === 200,
    });

    sleep(1);
}