module.exports = {
	output: {
		filename: '[name]-[hash].js',
	},
	module: {
		rules: [{
			test: /\.svg$/,
			use: [
				{
					loader: 'babel-loader',
				},
				{
					loader: 'react-svg-loader',
					options: {
						jsx: true,
					},
				},
			],
		}],
	},
};
