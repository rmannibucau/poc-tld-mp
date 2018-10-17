import SecurityService from '../security/Security.service';

function isLogged() {
	return SecurityService.toState().valid;
}

function isAdmin() {
	return SecurityService.toState().hasPermission('**');
}

export default {
	'app:isAdmin': isAdmin,
	'app:isLogged': isLogged,
	'app:isNotLogged': () => !isLogged(),
};
