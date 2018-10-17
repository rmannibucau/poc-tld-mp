import React from 'react';
import PropTypes from 'prop-types';
import { IconsProvider } from '@talend/react-components';
import { Notification } from '@talend/react-containers';

import icons from './icons';

import theme from './App.scss';

export default function App(props) {
	return (
		<div className={theme.App}>
			<IconsProvider icons={icons} />
			<Notification />
			{props.children}
		</div>
	);
}

App.propTypes = {
	children: PropTypes.element,
};
