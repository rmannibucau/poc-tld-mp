import React from 'react';
import { Skeleton } from '@talend/react-components';

import theme from './Loading.scss';

export default function Loading() {
	return (<Skeleton type="icon" name="talend-table" className={theme.Loading} />);
}
