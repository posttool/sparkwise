/* date/time */
/* daylight savings rules are for usa right now */
MONTHS = [ 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
function sparkwise_local_date(l,options)
{
	 
	var local,d,m,day,y,hh,mm;
	local = false || (options && options.local);
	
	if(local)
	{
		d 	 = new Date(l);
		m    = d.getMonth();
		day  = d.getDate();
		y    = d.getFullYear();
		hh   = d.getHours();
		mm   = d.getMinutes();
	}
	else
	{
		d 	 = new Date(l);
		m    = d.getUTCMonth();
		day  = d.getUTCDate();
		y    = d.getUTCFullYear();
		hh   = d.getUTCHours();
		mm   = d.getUTCMinutes();
	}
	if(options && options.format_simple)
	{
		return (m+1)+"/"+day+"/"+y;
	}
	
	if(options && options.show_time)
	{
		if (mm < 10)
			mm = '0'+mm;
		
		var ap = "AM";
		if (hh   > 11) { ap = "PM";        }
		if (hh   > 12) { hh = hh - 12; }
		if (hh   == 0) { hh = 12;        }

		 return MONTHS[m]+' '+day+' at '+hh+':'+mm+' '+ap;
	}

	return MONTHS[m]+' '+day;
}






/** date formatting utils **/
var format_date_ago = function(time){
	var date = new Date(time);

	var diff = (new Date().getTime() - date.getTime()) / 1000;
	var day_diff = Math.floor(diff / 86400);
	if ( isNaN(day_diff) || day_diff < 0 || day_diff >= 31 )
	{
	    //console.log('day diff err is '+day_diff);
		return null;
	}		
	return day_diff == 0 && (
			diff < 60 && "just now" ||
			diff < 120 && "1 minute ago" ||
			diff < 3600 && Math.floor( diff / 60 ) + " minutes ago" ||
			diff < 7200 && "1 hour ago" ||
			diff < 86400 && Math.floor( diff / 3600 ) + " hours ago") ||
		day_diff == 1 && "Yesterday" ||
		day_diff < 7 && day_diff + " days ago" ||
		day_diff < 31 && Math.ceil( day_diff / 7 ) + " weeks ago";
}
var format_date = function(time)
{
	var d = new Date(time);
	return d.getMonth()+"/"+d.getDate()+"/"+d.getFullYear();
}





