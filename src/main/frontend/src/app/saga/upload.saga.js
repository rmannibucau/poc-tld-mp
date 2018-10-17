import { takeLatest, put, call } from 'redux-saga/effects';
import { actions } from '@talend/react-cmf';
import { UPLOAD_START } from '../constants';
import SecurityService from '../security/Security.service';
import SlugService from '../lib/Slug.service';

function* load() {
	yield put(actions.collections.addOrReplace('uploadForm', undefined));
	const { response, data } = yield SecurityService.getClient().get('/api/component/ui/download');
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('uploadFormError', {
			message: 'Can\'t load the form.',
		}));
	} else {
		// small
		yield put(actions.collections.addOrReplace('uploadForm', data));
	}
}

function* doUpload({ componentName, componentId, componentVersion, form }) {
	yield put(actions.collections.addOrReplace('uploadFormError', {}));
	const { response } = yield SecurityService.getClient()
		.post(`/api/component/download/${componentId}?componentVersion=${encodeURIComponent(componentVersion)}`, form);
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('componentFormError', {
			message: 'Can\'t upload the page properly.',
		}));
	} else {
		yield put({
			type: '@@router/CALL_HISTORY_METHOD',
			payload: {
				method: 'push',
				args: [
					`/application/component/${SlugService.toSlug(componentName)}/${componentId}`,
				],
			},
		});
	}
}

function* start() {
	yield takeLatest(UPLOAD_START, doUpload);
	yield call(load);
}

export default {
	'Upload::start': start,
};
