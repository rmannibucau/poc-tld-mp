import { takeLatest, put, call } from 'redux-saga/effects';
import { actions, sagas } from '@talend/react-cmf';
import { SECURITY_REFRESH } from '../constants';
import SecurityService from '../security/Security.service';

function* doRefresh({ collection, grantRequest }) {
	const { data, response } = yield call(sagas.http.post, '/api/security/token', grantRequest, {});
	if (!response.ok) {
		yield put(actions.collections.addOrReplace(collection, { loaded: true, validated: false }));
		return;
	}
	SecurityService.onLogin(data);
	yield put(actions.collections.addOrReplace(collection, { loaded: true, validated: true }));
}

function* refresh() {
	yield takeLatest(SECURITY_REFRESH, doRefresh);
}

export default {
	'Permissions::refresh': refresh,
};
