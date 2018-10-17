import { sagas } from '@talend/react-cmf';

const TOKEN_NAME = 'talend-marketplace-token';
const EXPIRATION_MARGIN = 60;
const REFRESH_MARGIN = 5 * 60;

class SecurityService {
	constructor() {
		this.storage = localStorage;
	}

	getClient() {
		if (this.client) {
			return this.client;
		}

		const wrapper = this.getToken();
		this.client = sagas.http.create({
			headers: {
				Authorization: `${wrapper.token.token_type} ${wrapper.token.access_token}`,
			},
			onError: () => ({
				type: '@@router/CALL_HISTORY_METHOD',
				payload: {
					method: 'push',
					args: ['/login'],
				},
			}),
		});
		return this.client;
	}

	toState() {
		const wrapper = this.getToken();
		const checkExpiration = !!(wrapper &&
					wrapper.expirationMs &&
					wrapper.token &&
					wrapper.token.refresh_token);
		const nowSec = new Date().getTime() / 1000;
		const preComputedGroups = this.groups;
		return {
			// note: a better impl would be to start an app saga with delay() (with the exp+refresh)
			valid: checkExpiration && nowSec < (wrapper.expirationMs - REFRESH_MARGIN),
			willExpire: checkExpiration && nowSec > (wrapper.expirationMs - EXPIRATION_MARGIN),
			hasPermission: vendorId => {
				if (!wrapper) {
					return false;
				}
				const groups = preComputedGroups || JSON.parse(atob(wrapper.token.access_token.split('.')[1])).groups;
				return groups.indexOf('**') >= 0 || groups.indexOf(vendorId) >= 0;
			},
		};
	}

	getRefreshToken() {
		const token = this.getToken();
		return token.refresh_token;
	}

	getToken() {
		const token = this.storage.getItem(TOKEN_NAME);
		return token && JSON.parse(token);
	}

	onLogin(token) {
		this.onLogout();
		const payload = {
			token,
			expirationMs: token.expires_in + new Date().getTime() - 500, /* network margin */
		};
		this.groups = token.groups;
		this.storage.setItem(TOKEN_NAME, JSON.stringify(payload));
	}

	onLogout() {
		this.storage.removeItem(TOKEN_NAME);
		if (this.groups) {
			delete this.groups;
		}
		if (this.client) {
			delete this.client;
		}
	}
}

export default new SecurityService();
