se.sparkwi.widget.viz.util.percent_change = function(self)
{
	self.viz_util.title_and_icon(self);
	
	var today = self.data.values[0].data.value;
	var old = self.data.values[self.data.values.length-1].data.value;
	var percent_change = 0;
	if(old != 0)
		percent_change = (today-old)/old;

	var c = today;
	var cs = String(Math.round(today));
	cs = self.util.add_commas(cs);

	var $text = $('<span class="widget-value"></span>');
	var $twrap = $('<div class="widget-value-layout"></div>');
	$text.html(cs);
	$twrap.append($text);
	$(self.element).append($twrap);
	
	var up_or_down = percent_change==0 ? "none" : percent_change<0 ? "down" : "up";
	var up_or_down_prefix = percent_change==0 ? "" : percent_change<0 ? "-" : "+";
	var $percent_wv = $('<div class="percent-widget-value"></div>');
	var $up_or_down = $('<div class="percent-'+up_or_down+'">'+up_or_down_prefix+Math.round(Math.abs(percent_change*100))+'%</div>');
	$percent_wv.append($up_or_down);
	$(self.element).append($percent_wv);
	
	var info = self.viz_util.size_type($text, self.width-40, 12, 88);
	if (info.too_small)
	{
		cs = self.util.number_to_size(c);
		$text.html(cs);
	}
	var top = info.baseline*25 + 45;
	$twrap.css({'top':top+'px'});
	//console.log(info);
	$percent_wv.css({'top':(info.baseline*55 + 50)+'px', 'left': info.width+'px'});
	
	var s = '';
	var ds = sparkwise_local_date(self.data.values[0].date_utc);
	var unit = self.unit || "" ;
	switch(self.display_type)
	{
		case se.sparkwi.DISPLAY_ABSOLUTE_TOTAL:
			s = "Total "+unit+" as of "+ds+"<br/>";
			break;
		case se.sparkwi.DISPLAY_PER_DAY_TOTAL:
			s = "Total "+unit+" on "+ds+"<br/>";
			break;
	}

	switch(parseInt(self.props['time-span']))
	{
		case 7:
			s += "% change in the last week";
			break;
		case 30:
			s += "% change in the last month";
			break;
		case 90:
			s += "% change in the last quarter";
			break;
		default:
			s += "% change in the last year";
			break;
	}
}