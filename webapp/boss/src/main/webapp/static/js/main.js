
function openPage(page, basePath) {
	var mainTabs = $('#main_tabs');
	if (mainTabs.length > 0) {
		if ($('#main_tabs').tabs('exists', page.title)){
			$('#main_tabs').tabs('select', page.title);
		} else {
			$('#main_tabs').tabs('add',{
				title: page.title,
				href: basePath + page.path,
				closable: true
			});
		}
	} else {
		window.open(u);
	}
}

