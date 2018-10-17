import React from 'react';
import PropTypes from 'prop-types';
import cmf, { cmfConnect } from '@talend/react-cmf';
import Form from '@talend/react-containers/lib/Form';
import Loading from '../Loading/Loading.component';
import { COMPONENTFORM_SUBMIT } from '../../constants';
import sagas from '../../saga/componentform.saga';

import theme from './ComponentForm.scss';

class ComponentForm extends React.Component {
	constructor(props) {
		super(props);
		this.getTitle = this.getTitle.bind(this);
		this.onSubmit = this.onSubmit.bind(this);
	}

	onSubmit(properties) {
		this.props.dispatch({
			type: COMPONENTFORM_SUBMIT,
			properties,
			id: this.context.router.params.id,
		});
	}

	getTitle() {
		return this.context.router.params.id ?
			`Edit ${this.props.componentForm.properties.name}` :
			'Register a component';
	}

	render() {
		if (!this.props.componentForm) {
			return (<Loading />);
		}
		return (<div className={theme.ComponentForm}>
			<div className="col-sm-offset-2 col-sm-8">
				<h1>{this.getTitle()}</h1>
				<Form
					formId="component-form"
					uiSchema={this.props.componentForm.uiSchema}
					jsonSchema={this.props.componentForm.jsonSchema}
					data={this.props.componentForm.properties}
					onSubmit={this.onSubmit}
				/>
			</div>
		</div>);
	}
}

ComponentForm.displayName = 'ComponentForm';
ComponentForm.sagas = sagas;
ComponentForm.propTypes = {
	componentForm: PropTypes.object,
	dispatch: PropTypes.function,
};
ComponentForm.contextTypes = {
	router: PropTypes.object,
};

function mapStateToProps(state, props) {
	const component = cmf.selectors.collections.toJS(state, props.collectionId);
	if (!component) {
		return state;
	}
	return {
		componentForm: component,
	};
}

export default cmfConnect({ mapStateToProps })(ComponentForm);
