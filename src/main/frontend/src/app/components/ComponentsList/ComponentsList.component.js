import React from 'react';
import PropTypes from 'prop-types';
import Pagination from '../Pagination/Pagination.component';
import Loading from '../Loading/Loading.component';
import ComponentItem from './ComponentItem.component';
import SearchBox from './SearchBox.component';
import connected from '../../lib/connected';

import theme from './ComponentsList.scss';

class ComponentsList extends React.Component {
	constructor(props) {
		super(props);

		this.onPrevious = this.onPrevious.bind(this);
		this.onNext = this.onNext.bind(this);
		this.reloadPage = this.reloadPage.bind(this);

		this.state = {
			pagination: props.pagination,
		};
	}

	onPrevious() {
		this.reloadPage(-1);
	}

	onNext() {
		this.reloadPage(+1);
	}

	reloadPage(offset) {
		const pagination = {
			...this.state.pagination,
			number: Math.max(1, this.state.pagination.number + offset),
		};
		this.setState({ pagination });
		this.props.dispatchActionCreator('components:loadPage', null, { pagination, options: this.props.options });
	}

	render() {
		if (!this.props.components) {
			return (<Loading />);
		}
		return (
			<div className={theme.ComponentsList}>
				<h1>Talend Components</h1>
				<div className={theme.typeahead}>
					<SearchBox />
				</div>
				<div className="container">
					{
						this.props.components && this.props.components.items &&
						this.props.components.items.length > 0 && (
							<div className={theme.componentsContainer}>
								{ this.props.components.items.map(
									item => (<ComponentItem key={item.id} item={item} />)) }
							</div>)
					}
					<Pagination
						page={this.props.components} pagination={this.state.pagination}
						onPrevious={this.onPrevious} onNext={this.onNext}
					/>
					{
						(!this.props.components || !this.props.components.items
							|| this.props.components.items.length === 0) &&
							<p><em>No Component Found.</em></p>
					}
				</div>
			</div>
		);
	}
}

export default connected(ComponentsList);

ComponentsList.displayName = 'ComponentsList';
ComponentsList.propTypes = {
	components: PropTypes.object,
	pagination: PropTypes.object,
	options: PropTypes.object,
	dispatchActionCreator: PropTypes.function,
};
