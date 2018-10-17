import cmf, { cmfConnect } from '@talend/react-cmf';

function mapStateToProps(state, ownProps) {
	const errorName = `${ownProps.collectionId}Error`;
	return {
		[ownProps.collectionId]: cmf.selectors.collections.toJS(state, ownProps.collectionId),
		[errorName]: cmf.selectors.collections.toJS(state, errorName),
		routeParams: ownProps.routeParams,
	};
}

export default function connected(target) {
	return cmfConnect({ mapStateToProps })(target);
}
