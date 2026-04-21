import { check, sleep } from 'k6';
import http from 'k6/http';

import {
	getBaseUrl,
	getVuAuthContext,
	refreshVuAuthContext,
} from './auth.js';

const BASE_URL = getBaseUrl();

function getJson(url, tags) {
	const response = http.get(url, { tags });

	check(response, {
		[`${tags.endpoint} status is 2xx`]: (r) => r.status >= 200 && r.status < 300,
	});

	return response;
}

export function usersEmailAvailabilityScenario() {
	const email = `k6-user-${__VU}-${__ITER}@example.com`;
	getJson(
		`${BASE_URL}/api/v1/users/is-email-available?email=${encodeURIComponent(email)}`,
		{ endpoint: 'users-is-email-available' }
	);
	sleep(0.5);
}

export function usersLoginScenario() {
	try {
		getVuAuthContext();
	} finally {
		sleep(0.5);
	}
}

export function usersRefreshScenario() {
	try {
		refreshVuAuthContext();
	} finally {
		sleep(0.5);
	}
}
