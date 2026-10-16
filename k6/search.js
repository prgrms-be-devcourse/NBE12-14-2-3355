import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 30,
    duration: '1m',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
        checks: ['rate>0.99'],
    },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const keyword = __ENV.KEYWORD || 'Baldur';
const page = Number(__ENV.PAGE || 0);
const size = Number(__ENV.SIZE || 20);
if (!Number.isInteger(page) || page < 0 || !Number.isInteger(size) || size < 1 || size > 100) {
    throw new Error('PAGE >= 0, SIZE = 1..100인 정수를 지정하세요.');
}
const tags = { name: 'game-search', keyword, page: String(page), size: String(size) };

export default function () {
    const response = http.get(
        `${baseUrl}/api/v1/games/page`
        + `?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`,
        {
            tags,
        }
    );

    check(response, {
        '검색 응답이 200': (r) => r.status === 200,
        '검색 결과 구조가 정상': (r) => {
            try {
                return Array.isArray(r.json().data.content);
            } catch {
                return false;
            }
        },
    }, tags);

    sleep(1);
}
