import { takeLatest, put, call } from 'redux-saga/effects';
import { actions, sagas } from '@talend/react-cmf';
import { LOGIN_START } from '../constants';
import SecurityService from '../security/Security.service';

function* doLogin({ grantRequest }) {
	yield put(actions.collections.addOrReplace('LoginError', {}));
	const { data, response } = yield call(sagas.http.post, '/api/security/token', grantRequest, {});
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('LoginError', {
			message: 'Invalid username and/or password.',
		}));
		return;
	}
	SecurityService.onLogin(data);
	yield put(actions.collections.addOrReplace('LoginState', { logged: true }));
	yield put({
		type: '@@router/CALL_HISTORY_METHOD',
		payload: { method: 'push', args: ['/'] },
	});
}

function* loginFlow() {
	yield takeLatest(LOGIN_START, doLogin);
}

function* onHttpError({ error }) {
	if (error && error.stack && error.stack.status === 401) {
		yield put({
			type: '@@router/CALL_HISTORY_METHOD',
			payload: { method: 'push', args: ['/login'] },
		});
	}
}

function* redirectOn401() {
	yield takeLatest('@@HTTP/ERRORS', onHttpError);
}

export default {
	'Login::loginFlow': loginFlow,
	'Login::redirectOn401': redirectOn401,
};
