$_footer_init = function()
{
	
	$('#mnu-privacy-policy').click(function() {
		$$dialog('PRIVACY POLICY','',$('<iframe src="/tool/privacy.fhtml" width="100%" height="350" allowtransparency="true" frameborder="0"/>'),[{name:'OK'}], {width:'790'});
		return false;
	});
	
	$('#mnu-terms').click(function() {
		$$dialog('TERMS AND CONDITIONS','',$('<iframe src="/tool/terms.fhtml" width="100%" height="350" allowtransparency="true" frameborder="0"/>'),[{name:'OK'}], {width:'790'});
		return false;
	});
};
