function parse(dateString) {
	return new Date(dateString);
}

function format(dateString) {
	return parse(dateString).toLocaleString();
}

export default {
	parse,
	format,
};
