import React from 'react';
import PropTypes from 'prop-types';
import { cmfConnect } from '@talend/react-cmf';
import UIForm from '@talend/react-forms';
import { Icon } from '@talend/react-components';
import sagas from '../../saga/login.saga';
import { LOGIN_START } from '../../constants';

import theme from './Login.scss';

class Login extends React.Component {
	constructor(props) {
		super(props);
		this.onSubmit = this.onSubmit.bind(this);
	}

	onSubmit(event, properties) {
		this.props.dispatch({
			type: LOGIN_START,
			grantRequest: {
				grant_type: 'password',
				...properties,
			},
		});
	}

	render() {
		return (<div className={theme.Login}>
			<div className={theme.wrapper}>
				<div className={theme.form}>
					<Icon name="talend-logo-colored" className={theme.logo} />
					<h1>Login</h1>
					<div>Welcome on Talend Components Marketplace</div>
					<div className={theme.error}>{this.props.errorMessage}</div>
					<UIForm data={this.props.form} onSubmit={this.onSubmit} />
				</div>
			</div>
		</div>);
	}
}

function mapStateToProps(state) {
	return {
		errorMessage: state.cmf.collections.getIn(['LoginError', 'message']),
	};
}

const component = cmfConnect({ mapStateToProps })(Login);
component.sagas = sagas;
export default component;

Login.displayName = 'Login';
Login.propTypes = {
	form: PropTypes.object,
	errorMessage: PropTypes.string,
	dispatch: PropTypes.function,
};
