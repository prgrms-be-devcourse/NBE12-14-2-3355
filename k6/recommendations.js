import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
if (!/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(baseUrl)) {
    throw new Error('이 스크립트는 테스트 계정을 만드는 로컬 전용입니다. BASE_URL은 localhost를 사용하세요.');
}
const mode = __ENV.MODE || 'all';
if (!['all', 'personalized', 'related'].includes(mode)) {
    throw new Error('MODE는 all, personalized, related 중 하나여야 합니다.');
}
const smoke = __ENV.SMOKE === 'true';
const stageDuration = __ENV.STAGE_DURATION || '15s';
const pause = Number(__ENV.SLEEP_SECONDS || 1);
if (!Number.isFinite(pause) || pause < 0) throw new Error('SLEEP_SECONDS는 0 이상이어야 합니다.');

// all: 세 시나리오를 합쳐 2 → 6 → 10 → 6 → 2 → 0 VU. 최대 10명.
const allocations = mode === 'all'
    ? { onboarding: [1, 2, 4], record: [1, 2, 3], related: [0, 2, 3] }
    : mode === 'personalized'
        ? { onboarding: [1, 3, 5], record: [1, 3, 5] }
        : { related: [2, 6, 10] };
const scenarios = {};
for (const [name, [start, middle, peak]] of Object.entries(allocations)) {
    scenarios[name] = smoke
        ? { executor: 'per-vu-iterations', vus: 1, iterations: 1, maxDuration: '1m', exec: name }
        : {
            executor: 'ramping-vus', exec: name, startVUs: start,
            stages: [start, middle, middle, peak, peak, middle, start, 0]
                .map((target) => ({ duration: stageDuration, target })),
            gracefulRampDown: '30s', gracefulStop: '30s',
        };
}
const thresholds = {};
for (const name of Object.keys(scenarios)) {
    thresholds[`http_req_failed{scenario:${name}}`] = ['rate<0.01'];
    thresholds[`checks{scenario:${name}}`] = ['rate>0.99'];
    // 시나리오별 submetric을 명시해 p95/p99를 별도 시계열로 전송한다.
    thresholds[`http_req_duration{scenario:${name}}`] = ['p(99)<60000'];
}
export const options = {
    scenarios, thresholds, setupTimeout: '3m',
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

function request(method, path, body, token, name) {
    const response = http.request(method, `${baseUrl}${path}`,
        body === null ? null : JSON.stringify(body), {
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { Authorization: `Bearer ${token}` } : {}),
            },
            tags: { name: name || 'fixture-setup' }, timeout: '60s',
        });
    return response;
}

function requireData(response, label) {
    let body;
    try { body = response.json(); } catch { /* 아래에서 민감한 응답을 출력하지 않고 실패 처리 */ }
    if (response.status < 200 || response.status >= 300 || body?.data === undefined) {
        throw new Error(`${label} 실패: HTTP ${response.status} (응답 본문/토큰은 출력하지 않습니다)`);
    }
    return body.data;
}

function requireRecommendations(path, token, label) {
    const data = requireData(request('GET', path, null, token), label);
    if (!Array.isArray(data) || data.length === 0) {
        throw new Error(`${label}: 추천 결과가 비었습니다. 다른 GAME_IDS를 지정하세요.`);
    }
}

export function setup() {
    let gameIds;
    if (__ENV.GAME_IDS) {
        gameIds = __ENV.GAME_IDS.split(',').map(Number);
        if (gameIds.length < 1 || gameIds.length > 3
            || gameIds.some((id) => !Number.isSafeInteger(id) || id < 1)) {
            throw new Error('GAME_IDS는 로컬 DB 게임 ID 1~3개를 쉼표로 구분하세요.');
        }
    } else {
        const page = requireData(request('GET', '/api/v1/games/page?page=0&size=3', null), '게임 선택');
        gameIds = page.content?.map((game) => game.id);
        if (!gameIds?.length) throw new Error('DB에 게임이 없습니다. 먼저 데이터를 수집하세요.');
    }
    const relatedGameId = Number(__ENV.RELATED_GAME_ID || gameIds[0]);
    if (!Number.isSafeInteger(relatedGameId) || relatedGameId < 1) {
        throw new Error('RELATED_GAME_ID는 양의 정수여야 합니다.');
    }
    const fixtures = { relatedGameId, gameIds };
    const runId = `${Date.now()}${Math.floor(Math.random() * 100000)}`;
    const users = {};
    for (const kind of Object.keys(scenarios).filter((name) => name !== 'related')) {
        const email = `k6-${kind}-${runId}@example.com`;
        const password = `K6!${runId}a`;
        const user = requireData(request('POST', '/api/v1/users/signup', {
            nickname: `k6${kind === 'record' ? 'r' : 'o'}${runId}`, email, password,
        }), `${kind} 테스트 회원가입`);
        const login = requireData(request('POST', '/api/v1/users/login', { email, password }), '로그인');
        if (!login.accessToken) throw new Error('로그인 응답에 Access Token이 없습니다.');
        fixtures[`${kind}Token`] = login.accessToken;
        users[kind] = user.id;
        if (kind === 'onboarding') {
            requireData(request('PUT', '/api/v1/users/me/preferred-games',
                { gameIds }, login.accessToken), '온보딩 선호 게임 설정');
            requireData(request('PATCH', '/api/v1/users/me/onboarding', null, login.accessToken), '온보딩 완료');
        } else {
            for (const gameId of gameIds) {
                requireData(request('POST', `/api/v1/library/games/${gameId}`, {
                    playStatus: 'COMPLETED', isPlaying: false, isBacklog: false,
                    isWishlist: false, isLiked: false, playTimeHours: 10,
                }, login.accessToken), '플레이 기록 설정');
            }
        }
        requireRecommendations('/api/v1/games/recommendations/personalized', login.accessToken, `${kind} 사전 확인`);
    }
    if (scenarios.related) {
        requireRecommendations(`/api/v1/games/${relatedGameId}/related`, null, '연관 추천 사전 확인');
    }
    console.log(`테스트 준비 완료: GAME_IDS=${gameIds.join(',')}, RELATED_GAME_ID=${relatedGameId}, 테스트 계정 ID=${JSON.stringify(users)}`);
    return fixtures;
}

function measure(path, token, name) {
    const response = request('GET', path, null, token, name);
    check(response, {
        'HTTP 200': (r) => r.status === 200,
        '추천 결과 존재': (r) => {
            try { return Array.isArray(r.json().data) && r.json().data.length > 0; }
            catch { return false; }
        },
    });
    sleep(pause);
}

export function onboarding(data) {
    measure('/api/v1/games/recommendations/personalized', data.onboardingToken, '사용자 맞춤 추천');
}
export function record(data) {
    measure('/api/v1/games/recommendations/personalized', data.recordToken, '사용자 맞춤 추천');
}
export function related(data) {
    measure(`/api/v1/games/${data.relatedGameId}/related`, null, '게임 상세 연관 추천');
}
