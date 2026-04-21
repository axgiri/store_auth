import { check, fail } from 'k6';
import http from 'k6/http';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/+$/, '');
const AUTH_EMAIL = __ENV.AUTH_EMAIL;
const AUTH_USERS = (__ENV.AUTH_USERS || '')
	.split(',')
	.map((value) => value.trim())
	.filter(Boolean);
const AUTH_USERS_COUNT = Number(__ENV.AUTH_USERS_COUNT || '0');
const AUTH_USER_EMAIL_TEMPLATE = __ENV.AUTH_USER_EMAIL_TEMPLATE || '';
const AUTH_PASSWORD = __ENV.AUTH_PASSWORD;

// Cache tokens per VU so each virtual user logs in only once.
const vuAuthCache = new Map();

function requireEnv(value, name) {
	if (!value) {
		fail(`${name} is required`);
	}
	return value;
}

export function getBaseUrl() {
	return requireEnv(BASE_URL, 'BASE_URL');
}

function resolveVuEmail() {
	if (AUTH_USERS.length > 0) {
		const index = (__VU - 1) % AUTH_USERS.length;
		return AUTH_USERS[index];
	}

	if (AUTH_USERS_COUNT > 0) {
		const template = requireEnv(
			AUTH_USER_EMAIL_TEMPLATE,
			'AUTH_USER_EMAIL_TEMPLATE'
		);
		const index = ((__VU - 1) % AUTH_USERS_COUNT) + 1;
		if (!template.includes('%d')) {
			fail('AUTH_USER_EMAIL_TEMPLATE must include %d placeholder');
		}
		return template.replace('%d', String(index));
	}

	return requireEnv(
		AUTH_EMAIL,
		'AUTH_EMAIL (or AUTH_USERS / AUTH_USERS_COUNT + AUTH_USER_EMAIL_TEMPLATE)'
	);
}

function parseJsonSafe(response) {
	try {
		return response.json();
	} catch {
		return null;
	}
}

function extractAuthTokens(body) {
	if (!body || typeof body !== 'object') {
		return null;
	}

	return {
		accessToken: body.access_token,
		refreshToken: body.refresh_token
	};
}

export function getVuAuthContext() {
	const vuKey = String(__VU);
	if (vuAuthCache.has(vuKey)) {
		return vuAuthCache.get(vuKey);
	}

	const baseUrl = getBaseUrl();
	const email = resolveVuEmail();
	const password = requireEnv(AUTH_PASSWORD, 'AUTH_PASSWORD');

	const response = http.post(
		`${baseUrl}/api/v1/users/login`,
		JSON.stringify({ email, password }),
		{
			headers: { 'Content-Type': 'application/json' },
			tags: { endpoint: 'users-login' },
		}
	);

	const body = parseJsonSafe(response);
	const tokens = extractAuthTokens(body);
	const ok = check(response, {
		'login status is 200': (r) => r.status === 200,
		'login returns access token': () => !!tokens?.accessToken,
		'login returns refresh token': () => !!tokens?.refreshToken,
	});

	if (!ok) {
		fail(`Login failed with status ${response.status}`);
	}

	const authContext = {
		accessToken: tokens.accessToken,
		refreshToken: tokens.refreshToken,
	};
	vuAuthCache.set(vuKey, authContext);
	return authContext;
}

export function refreshVuAuthContext() {
	const baseUrl = getBaseUrl();
	const authContext = getVuAuthContext();

	const response = http.put(
		`${baseUrl}/api/v1/users/refresh`,
		JSON.stringify({
			refresh_token: authContext.refreshToken,
		}),
		{
			headers: { 'Content-Type': 'application/json' },
			tags: { endpoint: 'users-refresh' },
		}
	);

	const body = parseJsonSafe(response);
	const tokens = extractAuthTokens(body);
	const ok = check(response, {
		'refresh status is 200': (r) => r.status === 200,
		'refresh returns access token': () => !!tokens?.accessToken,
		'refresh returns refresh token': () => !!tokens?.refreshToken,
	});

	if (!ok) {
		fail(`Refresh failed with status ${response.status}`);
	}

	const updated = {
		accessToken: tokens.accessToken,
		refreshToken: tokens.refreshToken,
	};
	vuAuthCache.set(String(__VU), updated);
	return updated;
}

export function authHeaders(accessToken) {
	return {
		Authorization: `Bearer ${accessToken}`,
	};
}

export function grafanaCookieHeader(accessToken) {
	return {
		Cookie: `grafana_session=${accessToken}`,
	};
}