import React from 'react';
import PropTypes from 'prop-types';
import { List as Grid } from '@talend/react-containers';
import { cmfConnect } from '@talend/react-cmf';
import Immutable from 'immutable';
import sagas from '../../saga/crudlist.saga';
import Loading from '../Loading/Loading.component';
import {
	CRUDLIST_PAGINATION_CHANGE,
	CRUDLIST_ACTION_EDIT,
	CRUDLIST_ACTION_DELETE,
} from '../../constants';

import theme from './CrudList.scss';

// TODO: make it support server pagination otherwise it is poorly usable
class List extends React.Component {
	componentDidUpdate(oldProps) {
		if (oldProps.routeParams.entity !== this.props.routeParams.entity) {
			this.props.dispatch({
				type: CRUDLIST_PAGINATION_CHANGE,
				pagination: { // for now list doesn't support server pagination so hardcode it
					number: 1,
					pageSize: 1000,
				},
				routeParams: this.props.routeParams,
			});
		}
	}

	render() {
		if (!this.props.items && !this.props.crudListError) {
			return (<Loading />);
		}
		return (
			<div className={theme.CrudList}>
				<h1>{this.props.routeParams.entity}</h1>
				<Grid
					items={this.props.items}
					list={{
						columns: this.props.columns,
						titleProps: {
							key: 'name',
						},
					}}
					toolbar={{
						sort: {
							options: [{ id: 'id', name: 'Id' }, { id: 'name', name: 'Name' }],
							field: 'name',
						},
						display: {
							displayModes: ['large', 'table'],
						},
						pagination: {
							totalResults: this.props.total,
							/* TODO: the component doesn't support yet server side pagination
							onChange: (from, pageSize) => this.props.dispatch({
								type: CRUDLIST_PAGINATION_CHANGE,
								pagination: {
									number: from / pageSize,
									pageSize,
								},
								routeParams: this.props.routeParams,
							}),
							*/
						},
					}}
					actions={{
						items: [
							{
								label: 'Edit',
								icon: 'talend-burger',
								payload: {
									type: CRUDLIST_ACTION_EDIT,
									routeParams: this.props.routeParams,
								},
							},
							{
								label: 'Delete',
								icon: 'talend-cross',
								payload: {
									type: CRUDLIST_ACTION_DELETE,
									routeParams: this.props.routeParams,
								},
							},
						],
						left: [`${this.props.routeParams.entity}:add`],
					}}
				/>
			</div>
		);
	}
}

List.displayName = 'CrudList';
List.sagas = sagas;
List.propTypes = {
	crudListError: PropTypes.any,
	total: PropTypes.number.isRequired,
	items: PropTypes.any.isRequired,
	columns: PropTypes.any.isRequired,
	routeParams: PropTypes.object,
	dispatch: PropTypes.function,
};

// yes this is highly consistent,columns must be an object and items a
function mapStateToProps(state, ownProps) {
	const columns = state.cmf.collections.getIn(['crudList', 'columns'], new Immutable.List()).toJS();
	const items = state.cmf.collections.getIn(['crudList', 'items'], new Immutable.List());
	return {
		columns,
		items,
		total: state.cmf.collections.getIn(['crudList', 'total']),
		crudListError: state.cmf.collections.get('crudListError'),
		routeParams: ownProps.routeParams,
	};
}

export default cmfConnect({ mapStateToProps })(List);
