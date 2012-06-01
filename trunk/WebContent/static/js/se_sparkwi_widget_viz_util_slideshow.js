se.sparkwi.widget.viz.util.slideshow = function(self, images)
{
	self.viz_util.title_and_icon(self);
	var to = self.title_height;
	var h = self.height - to;
	var $f = $$div(self.element,'slideshow').css({'position':'relative','height':h+'px', 'margin-top':to+'px'});
	se.sparkwi.widget.viz.util.$slideshow($f, images, 
		{ width: self.width, height: h, scale: self.props.scale }, 
		self.props.speed ? self.props.speed : 3);
}

se.sparkwi.widget.viz.util.$slideshow = function($p, images, options, speed)
{
	var at = 0;
	var $slides = $$div($p,'slides');
	var $controls = $$div($p,'controls');
	var $buttons = $$div($controls,'buttons');
	var $back = $$div($buttons,'back');
	var $pp = $$div($buttons,'play');
	var $next = $$div($buttons,'next');
	var do_load = function(img_idx)
	{
		if (img_idx>at+2)
			return;
		var o = jQuery.extend(
			{
				onload: function()
				{ 
					update_ui();
					do_load(img_idx+1);
				}
			}, options);
		var $i = $$image($slides, images[img_idx], o)
			.css({'position':'absolute','top':'0','left':'0'})
			.hide()
			.data('idx',img_idx);
		var c = $slides.children();
		if (c.length>5)
			$(c[0]).remove();
	}
	var update_ui = function()
	{
		var c = $slides.children();
		var found = false;
		for (var i=0; i<c.length; i++)
		{
			var $ci = $(c[i]);
			if ($ci.data('idx')==at)
			{
				found = true;
				$ci.stop().fadeTo(500,1);
			}
			else
				$ci.stop().fadeTo(500,0);
		}
		return found;
	}
	var show_next = function()
	{
		if($p.closest('body').length == 0)
		{
			stop_ss();
			return;
		}
		at++;
		if (at>=images.length)
			at = 0;
		if (!update_ui()) 
			do_load(at);
	}
	var show_prev = function()
	{
		if (at==0)
			return;
		at--;
		if (!update_ui()) 
			do_load(at);
	}
	var over = function(){ $controls.stop().fadeTo(500,1); };
	var out = function(){ $controls.stop().fadeTo(500,0); };
	$slides.hover(over,out);
	$controls.hover(over,out);
	$controls.hide();
	do_load(0);
	var ssid = -1;
	var start_ss = function()
	{
		if (ssid==-1)
		{
			ssid = setInterval(show_next, parseInt(speed*1000));
			$pp.removeClass('pause');
		}
	}
	var stop_ss = function()
	{
		if (ssid!=-1)
		{
			clearInterval(ssid)
			ssid = -1;
			$pp.addClass('pause');
		}
	}
	var toggless = function()
	{
		if (ssid==-1)
			start_ss();
		else
			stop_ss();
	}
	$next.click(function(){ stop_ss(); show_next(); });
	$back.click(function(){ stop_ss(); show_prev(); });
	$pp.click(toggless);
	start_ss();
}