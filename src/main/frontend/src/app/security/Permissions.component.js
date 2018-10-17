import React from 'react';
import PropTypes from 'prop-types';
import { Inject } from '@talend/react-components';
import { actions } from '@talend/react-cmf';
import Loading from '../components/Loading/Loading.component';
import connected from '../lib/connected';
import SecurityService from './Security.service';
import { SECURITY_REFRESH } from '../constants';
import sagas from './permissions.saga.js';

import theme from './Permissions.scss';

class Permissions extends React.Component {
	constructor(props) {
		super(props);
		this.doCheck = this.doCheck.bind(this);
		this.componentDidMount = this.componentDidMount.bind(this);
		this.componentWillReceiveProps = this.componentWillReceiveProps.bind(this);
	}

	componentDidMount() {
		this.doCheck(this.props);
	}

	componentWillReceiveProps(props) {
		this.doCheck(props);
	}

	doCheck(props) {
		const holder = props[props.collectionId];
		if (!holder) { // reset and load the state
			const securityState = SecurityService.toState();
			if (!securityState.willExpire) {
				this.props.dispatch(actions.collections.addOrReplace(props.collectionId, {
					loaded: true,
					validated: securityState.valid,
				}));
			} else {
				this.props.dispatch(actions.collections.addOrReplace(props.collectionId, {
					loaded: false,
				}));
				this.props.dispatch({
					type: SECURITY_REFRESH,
					collection: props.collectionId,
					grantRequest: {
						grant_type: 'refresh_token',
						refresh_token: SecurityService.getRefreshToken(),
					},
				});
			}
		}
	}

	render() {
		const holder = this.props[this.props.collectionId];
		if (!holder || !holder.loaded) {
			return (<Loading />);
		}
		if (!holder.validated) {
			return (<span className={theme.Permissions}>
				<div className={theme.NotAllowed}>You are not allowed to access this page.</div>
			</span>);
		}
		const child = Inject.getReactElement(this.props.getComponent, {
			...this.props,
			...this.props.child,
		});
		return (<React.Fragment>{child}</React.Fragment>);
	}
}

Permissions.displayName = 'Permissions';
Permissions.sagas = sagas;
Permissions.propTypes = {
	collectionId: PropTypes.string.isRequired,
	child: PropTypes.object.isRequired,
	dispatch: PropTypes.function,
	getComponent: PropTypes.function,
};

export default connected(Permissions);
