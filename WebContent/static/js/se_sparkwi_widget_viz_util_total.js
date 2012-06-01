se.sparkwi.widget.viz.util.total = function(self)
{
	self.viz_util.title_and_icon(self);
	$(self.element).addClass('small');
	var c = -1;
	if (self.data.values.length!=0)
		c = Number(self.data.values[0].data.value);
	else
		c = Number(self.props['custom-count']);
	if (c>1E20)
		self.props['display-total-as'] = 'abbreviation';

	var cs = String(Math.round(c));
	var cs = self.util.add_commas(cs);
	
	switch(self.props['display-total-as'])
	{
		case 'default':
			break;
		case 'rounded0':
			cs = c.toFixed(2);
			break;
		case 'currency':
			cs = "$"+c.toFixed(2);
			break;
		case 'percent':
			cs = c+"%";
			break;
		case 'percent0':
			cs = Math.round(c)+"%";
			break;
		case 'abbreviation':
			cs = self.util.number_to_size(c);
			break;
	}
		
	var ds;
	var unit = self.unit || "" ;
	switch(self.display_type)
	{
		case se.sparkwi.DISPLAY_ABSOLUTE_TOTAL:
			ds = "Total "+unit+" as of";
			break;
		case se.sparkwi.DISPLAY_PER_DAY_TOTAL:
			ds = "Total "+unit+" on";
			break;
		case se.sparkwi.DISPLAY_PER_DAY_PERCENT:
		case se.sparkwi.DISPLAY_ABSOLUTE_PERCENT:
			ds =	"On";
			cs += "<span class='widget-percent-symbol'>%</span>";
			break;
	}
	var $text = $('<span class="widget-value"></span>');
	var $twrap = $('<div class="widget-value-layout"></div>');
	$text.html(cs);
	$twrap.append($text);
	$(self.element).append($twrap);
	
	var info = self.viz_util.size_type($text, self.width, 12, 88);
	if (info.too_small)
	{
		cs = self.util.number_to_size(c);
		$text.html(cs);
	}
	var top = info.baseline*25 + 45;
	$twrap.css({'top':top+'px'});
		
	// for more perfect baseline alignment, but less perfect character rendering...
	//		var t = self.viz_util.print(self.paper,0,113, cs, 'Akzidenz-Grotesk Std', fs);
	//		t.attr({'fill':'#444'});

	if (self.data.values.length!=0 && self.data.values[0].date_utc != null)
	{
		var d = sparkwise_local_date(self.data.values[0].date_utc);
		$(self.element).append('<div class="small widget-info-layout">'+ds+' '+d+'</div>');
	}

}