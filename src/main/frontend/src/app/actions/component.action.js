import { actions } from '@talend/react-cmf';
import {
	COMPONENTS_LOADING,
	COMPONENTS_LOADING_ERROR,
} from '../constants';

function loadPage(ignoredEvent, { options, pagination }) {
	const from = Math.max(0, (pagination.number - 1) * pagination.pageSize);
	const url = `/api/component/all?from=${from}&max=${pagination.pageSize}`;
	return actions.http.get(url, {
		onSend: COMPONENTS_LOADING,
		onError: COMPONENTS_LOADING_ERROR,
		cmf: {
			collectionId: 'components',
		},
		transform: data => {
			if (data.items && options.truncateLength) {
				return {
					...data,
					items: data.items.map(it => ({
						...it,
						description: it.description && it.description.length > options.truncateLength ?
							`${it.description.substring(0, options.truncateLength - 3)}...` : it.description,
					})),
				};
			}
			return data;
		},
	});
}

function loadSuggestions(ignoredEvent, { max, query }) {
	const url = `/api/component/suggestions?q=${encodeURIComponent(query || '')}&max=${max}`;
	return actions.http.get(url, {
		cmf: {
			collectionId: 'componentsSuggestions',
		},
		transform: data => {
			if (data.items) {
				return {
					...data,
					items: data.items.map(it => ({
						name: it.name,
						value: `${it.value}#${it.name}`,
					})),
				};
			}
			return data;
		},
	});
}

export default {
	'components:loadPage': loadPage,
	'components:loadSuggestions': loadSuggestions,
};
