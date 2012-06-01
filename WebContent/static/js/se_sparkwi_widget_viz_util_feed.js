se.sparkwi.widget.viz.util.feed = function(self)
{
	self.viz_util.title_and_icon(self);
	se.sparkwi.widget.viz.util.$feed(self.element, self.data.values[0].data.annotations, self.height, self.$title.height()+8);
}

se.sparkwi.widget.viz.util.$feed = function($p, annotations, h, ot)
{
	var $f = $("<div class='feed'></div>");
	$f.css({'position':'absolute','top':ot+'px', 'height': (h-ot)+'px'})
	var d = annotations;
	for (var i=0; i<d.length; i++)
	{
		var fd = sparkwise_local_date(d[i].date, {format_simple:true});
		var url = d[i].url;
		var short_url = url.substring(0,23);
		if (short_url!=url)
			short_url += "...";
		var title = d[i].title;
		var $fi = $("<div class='item'><strong class='date'>"+fd+"</strong> "+title+" "+
			"<a href='"+url+"' target='_blank'>"+short_url+"</a></div>");
		$f.append($fi);
	}
//	if (d.length==0)
//	{
//		// if the feeds date is today, the message should there is no data available right now, check back tomorrow.
//		$f.append("<div class='no-data'>There is no data available for this date.</div>");
//	}
	$p.append($f);
	$f.jScrollPane();
}