
jQuery.extend(se.sparkwi.widget.util, 
{	
	get_name_for: function(wi)
	{
		var s = [];
		for (var p in wi.selector_values)
			if (wi.selector_display_values != null && wi.selector_display_values[p] != null)
				s.push( wi.selector_display_values[p] );
			else
				s.push( wi.selector_values[p] );
		var sb = "";
		for (var i=0; i<s.length; i++)
		{
			sb += s[i];
			if (i!=s.length-1)
				sb += ", ";
		}
		if (sb=="")
			sb += wi.connection.username;
		return sb;
	},
	
	google_fluff_account: function(w)
	{
		var account =	w.selector_display_values.account;
		if (account == null)
			return '';
		if(account.startsWith('www.'))
			return account.substring(4);
		return account;
	},
	
	empty: function(o)
	{
		if (o == null)
			return true;
		if (o instanceof String)
			return o == "";
		if (o instanceof Array)
			return o.length == 0;
		if (o instanceof Object)
		{
			var c = 0;
			for (var p in o)
				c++;
			return c == 0;
		}
		return false;
	},
	
	filter_nulls: function(list)
	{
		var list_without_nulls = [];
		for (var i=0; i<list.length; i++)
		{
			if (list[i]==null)
				continue;
			list_without_nulls.push(list[i])
		}
		return list_without_nulls;
	},
	
	find_min_max_for_widget: function(dvals)
	{
		/* get min, max data value */
		if (dvals==null || dvals.length==0)
			return {};
			
		var dvalmin = Number.MAX_VALUE;
		var dvalmax = -Number.MAX_VALUE;
		for(var i = 0;i < dvals.length;i++)
		{
			if (dvals[i].data==null)
				continue;
			var v = dvals[i].data.value;
			dvalmin = Math.min(dvalmin, v);
			dvalmax = Math.max(dvalmax, v);
		}
		var dvalrange = Math.abs(dvalmax - dvalmin);
	
		/* get the min max date */
		var datemin = Number.MAX_VALUE;
		var datemax = -Number.MAX_VALUE;
		for(var i = 0;i < dvals.length;i++)
		{
			datemin = Math.min(datemin, dvals[i].date_utc);
			datemax = Math.max(datemax, dvals[i].date_utc);
		}
		var daterange = datemax - datemin;
		
		return { 
			dvalmin: dvalmin, dvalmax: dvalmax, dvalrange: dvalrange, 
			datemin: datemin, datemax: datemax, daterange: daterange };
	},
	
	find_min_max_for_correlation: function(corr)
	{
		if (corr.length==0)
			return {};
		/* same for correlation data  */
		var dvalmin = Number.MAX_VALUE;
		var dvalmax = -Number.MAX_VALUE;
		var datemin = Number.MAX_VALUE;
		var datemax = -Number.MAX_VALUE;
		for(var k = 0;k < corr.length;k++)
		{
			var dvals = corr[k].values;
			for(var i = 0;i < dvals.length;i++)
			{
				if (dvals[i].data == null)
					dvals[i].data = { value: 0 };
				dvalmin = Math.min(dvalmin, dvals[i].data.value);
				dvalmax = Math.max(dvalmax, dvals[i].data.value);
				datemin = Math.min(datemin, dvals[i].date_utc);
				datemax = Math.max(datemax, dvals[i].date_utc);
			}
		}
		var dvalrange = Math.abs(dvalmax - dvalmin);
		var daterange = datemax - datemin;
		return { 
			dvalmin: dvalmin, dvalmax: dvalmax, dvalrange: dvalrange, 
			datemin: datemin, datemax: datemax, daterange: daterange };
	},
	

	add_commas: function (nStr)
	{
		nStr = String(nStr);
		var x = nStr.split('.');
		var x1 = x[0];
		var x2 = x.length > 1 ? '.' + x[1] : '';
		var rgx = /(\d+)(\d{3})/;
		while (rgx.test(x1))
			x1 = x1.replace(rgx, '$1' + ',' + '$2');
		return x1 + x2;
	},
	
	number_to_size: function(n)
	{
		var unit = 1000;
		n = Number(n);
		if (n<unit)
		{
			n = Math.round(n*10)/10;
			if (n>=100)
				n = Math.floor(n);
			return n;
		}
		var size_suffixes = ['k','M','G','T','P','E','Z','Y'];
		var i = parseInt(Math.floor(Math.log(n) / Math.log(unit)));
		if (i<1)
			return self.util.add_commas(n);
		var nn = (n / Math.pow(unit, i)).toFixed(1);
		if (nn>=100)
			nn = Math.floor(nn);
		return nn + '' + size_suffixes[i-1];
	},
	
	widget_state_to_str: function (s,widget)
	{
	    switch(s)
	    {
	    case WIDGET_STATE_INIT:
	      return "Init";
	    case WIDGET_STATE_REQUIRES_DATA_PROXY:
	      return "Requires Configuration";
	    case WIDGET_STATE_OK:
	      return "OK";
	    case WIDGET_STATE_REQUIRES_PROPS:
	    	return "Requires Properties";
	    case WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS:
	      	return "Requires Members";
	    default:
	      return "Unknown State - "+s;
	    }
	},
	
	proxy_state_to_str: function(state)
	{
	    switch(state)
	    {
	    case PROXY_STATE_REQUIRES_AUTH:
	    	return "Connect this Account";
	    default:
	      return "Unknown Proxy State - "+s;
	    }
	}

	
});


//// TODO namespace

	
function get_widget_category(w)
{
	var self = this;
	var r = se.sparkwi.widget.collection;
	for (var i=0; i<r.categories.length; i++)
	{
		var cat = r.categories[i];
		 for (var j=0; j<cat.widgets.length; j++)
		 {
			 var catw = cat.widgets[j];
			 if (catw.id == w.id)
				 return cat;
		 }
	}
	return null;
}

function get_class_type_for_wi(wi)
{
	return (wi.type == WIDGET_TYPE_CORRELATION) ? 'sparkwise_correlation' : 
		get_class_name_for_widget(wi.proxy.widget);
}

function get_class_name_for_widget(wdef)
{
	try{
		var s = wdef.class_name.split(".");
		var n = s.pop();
		return n.toLowerCase();
	}catch(error)
	{
		/*for some reason the string "correlation" is being passed in here */ 
		return wdef;
	}
}

function get_css_class_for_cat(cat)
{
	return cat.name.toLowerCase().replace(' ','_');	
}

function get_css_class_for_proxy(proxy)
{
    var cat = null;
    try { 
    	cat = get_widget_category(proxy.widget);
    }catch(e){}
    if (cat==null)//old twitter widgets
    	return '';
    return cat.name.toLowerCase().replace(' ','_');	
}
function get_description_for_widget_instance(wi)
{
	if (wi.props.title!=null && wi.props.title!='')
		return wi.props.title;
	else
		return get_description_for_proxy(wi.proxy);
}
function get_description_for_proxy(proxy)
{
	if (proxy.props !=null && proxy.props.title)
		return proxy.props.title;
	var b = get_selector(proxy.selector_display_values, ['title','account','username','fan page','Search Query']);
	if (b==null)
	{
		b = '';
		for (var p in proxy.selector_display_values)
			b += proxy.selector_display_values[p]+" ";
	}
	b = proxy.widget.name+' '+b;
	var s = com_pagesociety_util_StringUtil.stripTags(b).split(' ');
	var d = '';
	for (var i=0; i<s.length; i++)
	{
		var si = s[i];
		if (si.length > 17)
			d += si.substring(0,14)+'... ';
		else
			d += si+' ';
	}
	return d;
}

function get_selector(vals,names)
{
	if (vals==null)
		return null;
	for (var i=0; i<names.length; i++)
	{
		if (vals[names[i]]!=null)
			return vals[names[i]];
	}
	return null;
}

function get_color_for_proxy(proxy)
{
    var cat = null;
    try { 
    	cat = get_widget_category(proxy.widget);
    }catch(e){}
    if (cat==null)//old twitter widgets
    	return '#CCCCCC';
    return ColorUtil.toHexString(se.sparkwi.colors[cat.name]);

}






