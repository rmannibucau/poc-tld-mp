import { takeLatest, put, call } from 'redux-saga/effects';
import { actions } from '@talend/react-cmf';
import { CRUDFORM_SUBMIT } from '../constants';
import SecurityService from '../security/Security.service';

function* load({ routeParams }) {
	yield put(actions.collections.addOrReplaceCollection('crudForm', undefined));
	yield put(actions.collections.addOrReplaceCollection('crudFormError', undefined));
	const basePath = `/api/${routeParams.entity}/ui`;
	const id = routeParams.id;
	const url = id ? `${basePath}/${id}` : basePath;
	const { response, data } = yield SecurityService.getClient().get(url);
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('crudFormError', {
			message: 'Can\'t load the page properly.',
		}));
	} else {
		yield put(actions.collections.addOrReplaceCollection('crudForm', data));
	}
}

function* doSubmit({ routeParams }, { properties }) {
	const basePath = `/api/${routeParams.entity}`;
	let submitResult;
	if (properties.id) {
		submitResult = yield SecurityService.getClient().put(`${basePath}/${properties.id}`, properties);
	} else {
		submitResult = yield SecurityService.getClient().post(basePath, properties);
	}
	const { response, data } = submitResult;
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('crudFormError', {
			message: `Can't save the form properly (${JSON.stringify(data, ' ', 2)}).`,
		}));
	} else {
		yield put({
			type: '@@router/CALL_HISTORY_METHOD',
			payload: { method: 'push', args: [`/application/admin/${routeParams.entity}/all`] },
		});
	}
}

function* start({ routeParams }) {
	yield takeLatest(CRUDFORM_SUBMIT, doSubmit, { routeParams });
	yield call(load, { routeParams });
}

export default {
	'CrudForm::start': start,
};
