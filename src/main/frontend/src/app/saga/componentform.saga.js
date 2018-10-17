import { takeLatest, put, call } from 'redux-saga/effects';
import { actions } from '@talend/react-cmf';
import { COMPONENTFORM_SUBMIT } from '../constants';
import SecurityService from '../security/Security.service';
import SlugService from '../lib/Slug.service';

function* load({ routeParams }) {
	yield put(actions.collections.addOrReplaceCollection('componentForm', undefined));
	const basePath = '/api/component/ui';
	const id = routeParams.id;
	const url = id ? `${basePath}/${id}` : basePath;
	const { response, data } = yield SecurityService.getClient().get(url);
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('componentFormError', {
			message: 'Can\'t load the page properly.',
		}));
	} else {
		yield put(actions.collections.addOrReplaceCollection('componentForm', data));
	}
}

function* doSubmit({ properties }) {
	const basePath = '/api/component';
	let submitResult;
	if (properties.id) {
		submitResult = yield SecurityService.getClient().put(`${basePath}/${properties.id}`, properties);
	} else {
		submitResult = yield SecurityService.getClient().post(basePath, properties);
	}
	const { response, data } = submitResult;
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('componentFormError', {
			message: `Can't save the form properly (${JSON.stringify(data, ' ', 2)}).`,
		}));
	} else {
		yield put({
			type: '@@router/CALL_HISTORY_METHOD',
			payload: { method: 'push', args: [`/application/component/${SlugService.toSlug(data.name)}/${data.id}`] },
		});
	}
}

function* start({ routeParams }) {
	yield takeLatest(COMPONENTFORM_SUBMIT, doSubmit);
	yield call(load, { routeParams });
}

export default {
	'ComponentForm::start': start,
};
