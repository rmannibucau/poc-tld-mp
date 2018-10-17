import React from 'react';
import PropTypes from 'prop-types';
import { Datalist } from '@talend/react-components';
import connected from '../../lib/connected';
import SlugService from '../../lib/Slug.service';

class SearchBox extends React.Component {
	constructor(props) {
		super(props);
		this.onLiveChange = this.onLiveChange.bind(this);
		this.onChange = this.onChange.bind(this);
	}

	onChange(event, { value }) {
		const sep = value.indexOf('#');
		const id = sep > 0 ? value.substring(0, sep) : value;
		const name = sep > 0 ? value.substring(sep + 1) : '-';
		this.props.dispatchActionCreator('redirect', undefined, {
			action: {
				path: `/application/component/${SlugService.toSlug(name)}/${id}`,
			},
		});
	}

	onLiveChange(event, query) {
		this.props.dispatchActionCreator('components:loadSuggestions', undefined, {
			query,
			max: this.props.max,
		});
	}

	render() {
		return (<Datalist
			onLiveChange={this.onLiveChange}
			onChange={this.onChange}
			titleMap={(this.props.componentsSuggestions && this.props.componentsSuggestions.items) || []}
			multiSection={false}
		/>);
	}
}

SearchBox.displayName = 'SearchBox';
SearchBox.propTypes = {
	max: PropTypes.number,
	componentsSuggestions: PropTypes.object,
	dispatchActionCreator: PropTypes.function,
};


export default connected(SearchBox);
