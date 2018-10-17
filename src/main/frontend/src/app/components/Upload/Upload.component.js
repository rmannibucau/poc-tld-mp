import React from 'react';
import PropTypes from 'prop-types';
import Form from '@talend/react-containers/lib/Form';
import Loading from '../Loading/Loading.component';
import sagas from '../../saga/upload.saga';
import { UPLOAD_START } from '../../constants';
import connected from '../../lib/connected';

import theme from './Upload.scss';

class Upload extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			properties: {},
		};
		this.onSubmit = this.onSubmit.bind(this);
		this.onChange = this.onChange.bind(this);
	}

	onSubmit(properties) {
		const files = document.querySelector('input[type="file"]').files;
		if (!files || files.length !== 1) {
			return;
		}

		const form = new FormData();
		Object.keys(properties)
			.filter(k => k !== 'file')
			.forEach(key => form.append(key, properties[key]));
		form.append('file', files[0]);

		this.props.dispatch({
			type: UPLOAD_START,
			form,
			componentId: properties.componentId,
			componentVersion: properties.componentVersion,
			componentName: properties.$componentName,
		});
	}

	onChange({ schema, properties }) {
		if (!schema.key || schema.key.length !== 1) {
			return;
		}
		switch (schema.key[0]) {
			case 'file': {
				const files = document.querySelector('input[type="file"]').files;
				if (files && files.length === 1) {
					const name = files[0].name;
					const result = /^.*([0-9]+\.[0-9]+\.[0-9]+)\.(car|zip)$/.exec(name);
					if (result && result.length === 3) {
						const componentVersion = result[1];
						this.setState({
							properties: {
								...properties,
								componentVersion,
							},
						});
					}
				}
				break;
			}
			case 'componentId': {
				const filter = schema.titleMap.filter(it => it.value === properties.componentId);
				if (!filter || !filter.length) {
					this.setState({
						properties: {
							...properties,
							$componentName: undefined,
						},
					});
				} else {
					this.setState({
						properties: {
							...properties,
							$componentName: filter[0].name,
						},
					});
				}
				break;
			}
			default:
		}
	}

	render() {
		if (!this.props.uploadForm) {
			return <Loading />;
		}
		return (
			<div className={`col-sm-offset-2 col-sm-8 ${theme.Upload}`}>
				<h1>Add a component archive</h1>
				<Form
					formId="upload-form"
					uiSchema={this.props.uploadForm.uiSchema}
					jsonSchema={this.props.uploadForm.jsonSchema}
					data={this.state.properties}
					onSubmit={this.onSubmit}
					onChange={this.onChange}
				/>
			</div>
		);
	}
}

Upload.displayName = 'Upload';
Upload.sagas = sagas;
Upload.propTypes = {
	uploadForm: PropTypes.object,
	dispatch: PropTypes.function,
};
export default connected(Upload);
