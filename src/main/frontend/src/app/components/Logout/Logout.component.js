import React from 'react';
import PropTypes from 'prop-types';
import { cmfConnect } from '@talend/react-cmf';
import SecurityService from '../../security/Security.service';

function Logout(props) {
	SecurityService.onLogout();
	props.dispatch({
		type: '@@router/CALL_HISTORY_METHOD',
		payload: {
			method: 'push',
			args: ['/'],
		},
	});
	return <div>Redirecting to the home page</div>;
}

export default cmfConnect({})(Logout);
Logout.displayName = 'Logout';
Logout.propTypes = {
	dispatch: PropTypes.function,
};
