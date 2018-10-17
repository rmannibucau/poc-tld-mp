import React from 'react';
import { shallow } from 'enzyme';
import mock from '@talend/react-cmf/lib/mock';

import App from './App.container';

describe('App container', () => {
	it('should render', () => {
		// given
		const context = mock.context();

		// when
		const wrapper = shallow(
			<App />
		, { context });

		// then
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
