import '@talend/bootstrap-theme/src/theme/theme.scss';
import { browserHistory as history } from 'react-router';
import cmf from '@talend/react-cmf';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import actionCreators from './actions';
import components from './components';
import Permissions from './security/Permissions.component';
import expressions from './expressions';

registerAllContainers();

cmf.expression.registerMany(expressions);

cmf.bootstrap({
	components: {
		...components,
		Permissions,
	},
	actionCreators,
	history,
});
