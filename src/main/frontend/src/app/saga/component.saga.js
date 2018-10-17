import { takeEvery, put, call } from 'redux-saga/effects';
import { actions, sagas } from '@talend/react-cmf';
import { COMPONENT_DELETE } from '../constants';
import SecurityService from '../security/Security.service';

function* load({ routeParams }) {
	yield put(actions.collections.addOrReplace('componentError', undefined));
	const { response, data } = yield call(sagas.http.get, `/api/component/${routeParams.id}?relationships=true`);
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('componentError', {
			message: `Can't load component ${routeParams.id}.`,
		}));
	} else {
		yield put(actions.collections.addOrReplace('component', data));
	}
}

function* httpDelete({ downloadId, componentId }) {
	yield put(actions.collections.addOrReplace('componentError', undefined));
	const { response } = yield SecurityService.getClient()
		.delete(`/api/component/download/${componentId}/${downloadId}`);
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('componentError', {
			message: 'Can\'t delete the file.',
		}));
	} else {
		yield call(load, { routeParams: { id: componentId } });
	}
}

function* start({ routeParams }) {
	yield takeEvery(COMPONENT_DELETE, httpDelete);
	yield call(load, { routeParams });
}

export default {
	'Component::start': start,
};
