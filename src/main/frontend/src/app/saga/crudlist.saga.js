import { put, takeLatest, call } from 'redux-saga/effects';
import { actions } from '@talend/react-cmf';
import SecurityService from '../security/Security.service';
import SlugService from '../lib/Slug.service';
import {
	CRUDLIST_PAGINATION_CHANGE,
	CRUDLIST_ACTION_EDIT,
	CRUDLIST_ACTION_DELETE,
} from '../constants';

const DEFAULT_PAGE_SIZE = 1000; // no server pagination yet in list container

function* load({ routeParams, pagination = { number: 1, pageSize: DEFAULT_PAGE_SIZE } }) {
	yield put(actions.collections.addOrReplace('crudListError', undefined));
	yield put(actions.collections.addOrReplace('crudList', undefined));
	const from = Math.max(0, (pagination.number - 1) * pagination.pageSize);
	const url = `/api/${routeParams.entity}/all?from=${from}&max=${pagination.pageSize}`;
	const { response, data } = yield SecurityService.getClient().get(url);
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('crudListError', {
			message: `Can't load the page: ${JSON.stringify(response.body || response.message)}`,
		}));
	} else {
		// ensure data has columns for the List.container
		if (data.items && !data.columns) {
			const columns = data.items
				.map(item => Object.keys(item))
				.reduce((a, keys) => ({
					...a,
					...keys.reduce((all, name) => ({
						...all,
						[name]: name,
					}), {}),
				}), {});
			const forcedOrderStartElts = ['id', 'name'];
			const forcedOrderEndElts = ['created', 'updated', 'version'];
			data.columns = Object.keys(columns)
				.map(key => ({
					key,
					label: key.charAt(0).toUpperCase() + key.slice(1),
				}))
				.sort((a, b) => {
					if (a === b || a.key === b.key) {
						return 0;
					}

					const aStartIndex = forcedOrderStartElts.indexOf(a.key);
					const bStartIndex = forcedOrderStartElts.indexOf(b.key);
					if (aStartIndex >= 0 && bStartIndex < 0) {
						return -1;
					}
					if (bStartIndex >= 0 && aStartIndex < 0) {
						return 1;
					}
					if (aStartIndex >= 0 && bStartIndex >= 0) {
						return aStartIndex - bStartIndex;
					}

					const aEndIndex = forcedOrderEndElts.indexOf(a.key);
					const bEndIndex = forcedOrderEndElts.indexOf(b.key);
					if (aEndIndex >= 0 && bEndIndex < 0) {
						return 1;
					}
					if (bEndIndex >= 0 && aEndIndex < 0) {
						return -1;
					}
					if (aEndIndex >= 0 && bEndIndex >= 0) {
						return aEndIndex - bEndIndex;
					}

					return a.key.localeCompare(b.key);
				});
			for (let i = 0; i < data.columns.length; i += 1) {
				data.columns[i].order = i;
			}
		}
		yield put(actions.collections.addOrReplace('crudList', data));
	}
}

function* doDelete({ model, routeParams }) {
	yield put(actions.collections.addOrReplace('crudListError', undefined));
	const { response } = yield SecurityService.getClient().delete(`/api/${routeParams.entity}/${model.id}`);
	if (!response.ok) {
		yield put(actions.collections.addOrReplace('crudListError', {
			message: `Can't delete the entity: ${JSON.stringify(response.body || response.message)}`,
		}));
	} else {
		yield call(load, { routeParams, pagination: { number: 1, pageSize: DEFAULT_PAGE_SIZE } });
		yield put({
			type: '@@router/CALL_HISTORY_METHOD',
			payload: {
				method: 'push',
				args: [`/application/admin/${routeParams.entity}/all`],
			},
		});
	}
}

function* doEdit({ model, routeParams }) {
	yield put({
		type: '@@router/CALL_HISTORY_METHOD',
		payload: {
			method: 'push',
			args: [`/application/admin/${routeParams.entity}/${SlugService.toSlug(model.name)}/${model.id}`],
		},
	});
}

function* start(args) {
	yield takeLatest(CRUDLIST_PAGINATION_CHANGE, load);
	yield takeLatest(CRUDLIST_ACTION_DELETE, doDelete);
	yield takeLatest(CRUDLIST_ACTION_EDIT, doEdit);
	yield call(load, args);
}

export default {
	'CrudList::start': start,
};
