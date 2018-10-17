import React from 'react';
import PropTypes from 'prop-types';
import Form from '@talend/react-containers/lib/Form';
import Loading from '../Loading/Loading.component';
import { CRUDFORM_SUBMIT } from '../../constants';
import sagas from '../../saga/crudform.saga';
import connected from '../../lib/connected';

import theme from './CrudForm.scss';

function ErrorMessage({ crudFormError }) {
	if (!crudFormError) {
		return undefined;
	}
	return (<div className="alert alert-danger">
		<strong>Error: </strong>{crudFormError.message || JSON.stringify(crudFormError)}
	</div>);
}

class CrudForm extends React.Component {
	constructor(props) {
		super(props);
		this.getTitle = this.getTitle.bind(this);
		this.onSubmit = this.onSubmit.bind(this);
	}

	onSubmit(properties) {
		this.props.dispatch({
			type: CRUDFORM_SUBMIT,
			properties,
			id: this.context.router.params.id,
		});
	}

	getTitle() {
		return this.context.router.params.id ?
			`Edit ${this.props.crudForm.properties.name} ${this.context.router.params.entity}` :
			`Register a ${this.context.router.params.entity}`;
	}

	render() {
		if (!this.props.crudForm && !this.props.crudFormError) {
			return (<Loading />);
		}
		return (<div className={theme.CrudForm}>
			<div className="col-sm-offset-2 col-sm-8">
				<h1>{this.getTitle()}</h1>
				{this.props.crudFormError &&
					<ErrorMessage crudFormError={this.props.crudFormError} />
				}
				{this.props.crudForm &&
					<Form
						formId="crud-form"
						uiSchema={this.props.crudForm.uiSchema}
						jsonSchema={this.props.crudForm.jsonSchema}
						data={this.props.crudForm.properties}
						onSubmit={this.onSubmit}
					/>
				}
			</div>
		</div>);
	}
}

CrudForm.displayName = 'CrudForm';
CrudForm.sagas = sagas;
CrudForm.propTypes = {
	crudForm: PropTypes.object,
	crudFormError: PropTypes.object,
	dispatch: PropTypes.function,
};
CrudForm.contextTypes = {
	router: PropTypes.object,
};
ErrorMessage.propTypes = {
	crudFormError: PropTypes.object,
};

export default connected(CrudForm);
