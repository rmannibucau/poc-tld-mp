import React from 'react';
import PropTypes from 'prop-types';
import { Badge, Action, Icon } from '@talend/react-components';
import { cmfConnect } from '@talend/react-cmf';
import Markdown from 'markdown-to-jsx';
import sagas from '../../saga/component.saga';
import Loading from '../Loading/Loading.component';
import DateService from '../../lib/Date.service';
import SlugService from '../../lib/Slug.service';
import SecurityService from '../../security/Security.service';
import { COMPONENT_DELETE } from '../../constants';
import connected from '../../lib/connected';

import theme from './Component.scss';

const linkProps = {
	link: true,
	target: '_blank',
};

function hasComponentPermissions(vendorId) {
	const securityState = SecurityService.toState();
	return securityState.hasPermission(vendorId);
}

function Meta({ component }) {
	return (
		<div className={theme.meta}>
			<span className={theme.updated}>
				<Icon name="talend-refresh" />
				<span>{DateService.format(component.updated)}</span>
			</span>
			{component.products.length > 0 && <div>
				{component.products.map((product, index) => (
					<Badge key={index} label={product.name} />
				))}
				</div>
			}
		</div>
	);
}

function ComponentLink(props) {
	const merged = {
		...linkProps,
		...props,
	};
	return <li><Action {...merged} /></li>;
}

function ComponentLinks({ component }) {
	return (
		<div>
			<ul>
				<ComponentLink
					label={component.completeVendor.name}
					icon="talend-hand-pointer"
					// not done:href={`/application/vendor/${this.props.component.completeVendor.id}`}
					onClick={e => e.preventDefault()}
				/>
				{hasComponentPermissions(component.completeVendor.id) &&
					<ComponentLink
						label="Edit"
						icon="talend-burger"
						href={`/application/admin/component/${SlugService.toSlug(component.name)}/${component.id}`}
						target="_self"
					/>
				}
				{component.sources &&
					<ComponentLink label="Source Code" icon="app-code" href={component.sources} />
				}
				{component.license &&
					<ComponentLink label="License" icon="talend-license" href={component.license} />
				}
				{component.bugtracker &&
					<ComponentLink label="Bugtracker" icon="app-bug" href={component.bugtracker} />
				}
			</ul>
		</div>
	);
}

function Download({ componentId, download, canDelete, dispatch }) {
	return (
		<div className={`row ${theme.Download}`}>
			<div className="col-sm-4">{download.componentVersion}</div>
			<div className="col-sm-4">{DateService.format(download.updated)}</div>
			<div className="col-sm-4">
				<Action label="Download" icon="talend-download" href={`/api/component/download/${download.id}`} {...linkProps} />
				{canDelete &&
					<Action
						label="Delete"
						icon="talend-cross"
						onClick={() => {
							dispatch({
								type: COMPONENT_DELETE,
								componentId,
								downloadId: download.id,
							});
						}}
						{...linkProps}
					/>
				}
			</div>
		</div>
	);
}
const ConnectedDownload = cmfConnect({})(Download);

function Downloads({ componentId, downloads, vendorId }) {
	const canDelete = hasComponentPermissions(vendorId);
	return (
		<div className={`row ${theme.Downloads}`}>
			<div className="col-sm-9">
				<h2>Downloads</h2>
				<div className={`row ${theme.DownloadsHeader}`}>
					<div className="col-sm-4">Version</div>
					<div className="col-sm-4">Update Date</div>
				</div>
				{downloads.map((download, index) =>
					<ConnectedDownload
						key={index}
						download={download}
						canDelete={canDelete}
						componentId={componentId}
					/>)}
			</div>
		</div>
	);
}

function Component(props) {
	if (!props.component) {
		return (<Loading />);
	}
	return (
		<div className={[theme.Component, 'container'].join(' ')}>
			<h1>{props.component.name}</h1>
			<div>{props.componentError && props.componentError.message}</div>
			<div className="col-sm-9">

				<Meta component={props.component} />

				<Markdown>{props.component.description}</Markdown>

				{props.component.changelog && (
					<div>
						<h2>Changelog</h2>
						<Markdown>{props.component.changelog}</Markdown>
					</div>
				)}
			</div>
			<div className={[theme.ComponentLinks, 'col-sm-3'].join(' ')}>
				<ComponentLinks component={props.component} />
			</div>
			{props.component.downloads.length > 0 &&
				<Downloads
					componentId={props.component.id}
					downloads={props.component.downloads}
					vendorId={props.component.completeVendor.id}
				/>
			}
		</div>
	);
}


Component.displayName = 'Component';
Component.sagas = sagas;
Component.propTypes = {
	componentError: PropTypes.any,
	component: PropTypes.object.isRequired,
};
ComponentLinks.propTypes = {
	component: PropTypes.object.isRequired,
};
Meta.propTypes = {
	component: PropTypes.object.isRequired,
};
Download.propTypes = {
	componentId: PropTypes.string.isRequired,
	download: PropTypes.object.isRequired,
	canDelete: PropTypes.bool,
	dispatch: PropTypes.function,
};
Downloads.propTypes = {
	componentId: PropTypes.string.isRequired,
	downloads: PropTypes.array.isRequired,
	vendorId: PropTypes.string.isRequired,
};

export default connected(Component);
