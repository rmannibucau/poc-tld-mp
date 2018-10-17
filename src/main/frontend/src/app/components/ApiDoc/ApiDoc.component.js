import React from 'react';
import { cmfConnect } from '@talend/react-cmf';
import { default as swaggerUi, presets } from 'swagger-ui';
import 'swagger-ui/dist/swagger-ui.css';
import spec from '../../../generated/openapi.json';

function ApiDoc() {
	return (<div>
		<h1>API Documentation</h1>
		<div
			className="swagger-ui"
			ref={domNode => {
				swaggerUi({
					domNode,
					spec,
					presets: [presets.apis],
				});
			}}
		/>
	</div>);
}

export default cmfConnect({})(ApiDoc);
