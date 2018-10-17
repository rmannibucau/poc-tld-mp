import App from './App';
import ComponentsList from './ComponentsList';
import components from './Component';
import crud from './Crud';
import Login from './Login';
import Logout from './Logout';
import Upload from './Upload';
import ApiDoc from './ApiDoc';

export default {
	App,
	...components,
	ComponentsList,
	...crud,
	Login,
	Logout,
	Upload,
	ApiDoc,
};
