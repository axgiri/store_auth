export {
	usersEmailAvailabilityScenario,
	usersLoginScenario,
	usersRefreshScenario,
} from './scenarios/user-controller.js';

export const options = {
	thresholds: {
		http_req_duration: ['p(95)<500'],
		http_req_failed: ['rate<0.01'],
	},
	scenarios: {
		usersEmailAvailability: {
			executor: 'constant-vus',
			exec: 'usersEmailAvailabilityScenario',
			vus: 3300,
			duration: '30s',
		},
		usersLogin: {
			executor: 'constant-vus',
			exec: 'usersLoginScenario',
			vus: 200,
			duration: '30s',
		},
		usersRefresh: {
			executor: 'constant-vus',
			exec: 'usersRefreshScenario',
			vus: 200,
			duration: '30s',
		},
	},
};
