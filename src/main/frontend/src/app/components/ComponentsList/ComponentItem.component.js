import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';
import { Action } from '@talend/react-components';
import { cmfConnect } from '@talend/react-cmf';
import Markdown from 'markdown-to-jsx';
import SlugService from '../../lib/Slug.service';

import theme from './ComponentsItem.scss';

function ComponentItem(props) {
	const path = `/application/component/${SlugService.toSlug(props.item.name)}/${props.item.id}`;
	const action = {
		link: true,
		href: path,
		action: {
			path,
		},
	};
	return (
		<div className={theme.ComponentsItem}>
			<h2>
				<Link to={path}>{props.item.name}</Link>
			</h2>
			<div className={theme.description}>
				<Markdown>{props.item.description.replace(/^#[^\n]*\n?$/m, '').trim()}</Markdown>
				<Action {...action} label="More" />
			</div>
			<div className={theme.meta}>
				<span><i className="fa fa-clock" /> {props.item.updated}</span>
			</div>
		</div>
	);
}

export default cmfConnect({})(ComponentItem);

ComponentItem.displayName = 'ComponentItem';
ComponentItem.propTypes = {
	item: PropTypes.object,
};
