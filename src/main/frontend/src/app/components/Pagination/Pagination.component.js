import React from 'react';
import PropTypes from 'prop-types';

function shouldPaginate(page, pagination) {
	const pageNumber = pagination.number || 0;
	const nextPage = pageNumber * pagination.pageSize;
	return pageNumber > 0 || (page && ((page.total || 0) > nextPage));
}

export default function Pagination(props) {
	if (!shouldPaginate(props.page, props.pagination)) {
		return undefined;
	}
	const pageNumber = props.pagination.number || 0;
	const showPrevious = pageNumber > 1;
	const showNext = props.page &&
		(props.page.total || 0) > (pageNumber * props.pagination.pageSize);
	return (
		<nav aria-label="...">
			<ul className={'pager'}>
				{showPrevious && <li className={'previous'}>
					<a href onClick={props.onPrevious}><span aria-hidden="true">&larr;</span> Previous</a>
				</li>}
				{showNext && <li className={'next'}>
					<a href onClick={props.onNext}>Next <span aria-hidden="true">&rarr;</span></a>
				</li>}
			</ul>
		</nav>
	);
}

Pagination.propTypes = {
	page: PropTypes.object,
	pagination: PropTypes.object,
	onPrevious: PropTypes.function,
	onNext: PropTypes.function,
};
