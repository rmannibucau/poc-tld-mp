/**
 * action creator
 * @param {Event} event which trigger this action
 * @param {Object} data {model,action} sub objects
 * @returns {Object} action
 */
export default function redirect(event, data) {
	const path = data.action.path;
	return {
		type: '@@router/CALL_HISTORY_METHOD',
		payload: {
			method: 'push',
			args: [path],
		},
	};
}
