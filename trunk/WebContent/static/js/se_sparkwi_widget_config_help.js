se.sparkwi.widget.config.help = 
{
	add_help_to_select: function ($d, $select, sname)
	{
		var self = this;
		if (self.help==null)
			return;
		var help_data = self.help[sname];
		if (help_data == null)
			return;
		$select.css({'width':'175px','float':'left'});
		var $icon = $("<img src='static/image/help-rollover.png' class='icon-help' />");
		$icon.css({'margin-left':'5px'});
		$d.append($icon);
		$icon.click(function(){ self.show_dialog(self.help.help_title, help_data, $icon); });
	},
	
	add_help_to_input: function($d, $input, sname)
	{
		var self = this;
		if (self.help==null)
			return;
		var help_data = self.help[sname];
		if (help_data == null)
			return;
		$input.css({'width':'165px','float':'left'});
		var $icon = $("<img src='static/image/help-rollover.png' class='icon-help' />");
		$icon.css({'margin-left':'5px'});
		$d.append($icon);
		$icon.click(function(){ self.show_dialog(self.help.help_title, help_data, $icon); });
	},
	
	show_dialog: function(title, data, $icon)
	{
		var self = this;
		if (self.help_showing)
			self.close_help();
		self.help_showing = true;
		// remember where the cell was
		var $op = self.cell.element.parent();
		var off = self.cell.element.offset();
		var st = $(document.body).scrollTop();
		var pos = self.cell.element.position();
		var oz = self.cell.element.css('z-index');
		//
		//$(document.body).css({'overflow':'hidden'});//would, but then scrollbar disappears and page moves
		disable_scroll_wheel();
		
		// add gauze
		var $gauze = $$div($(document.body),'help-gauze').css({ 'background':'#333333', 'opacity': '.70'});
		$gauze.fadeTo(111,.6);
		// add help
		var $b = $$div($(document.body),'help-main');
		var $x = $$div($b,'help-close-button');
		var $k = $$div($b,'help-caret');
		var $c = $$div($b,'help-header').append(title);
		var $d = $$div($b,'help-body');
		// add the content
		var $ol = $$el($d, "OL");
		for (var i=0; i<data.length; i++)
		{
			(function(di){
				var $i = $$el($ol,"LI");
				$i.append(di);
				if (di.indexOf("<rollover")!=-1)
				{
					var s = di.replace(/.*<rollover (.*) (\d*) (\d*)>/g, '<img src=\"/static/help/$1\" style=\"border: 1px solid #ccc;\" width=$2 height=$3 />');
					var $r = $('<img src=\"/static/image/help-icon.png\"/>');
					$i.append($r);
					$r.mouseover(function(){ 
						graph_tooltip_show($icon,s);
					}).mouseout(function(){
						graph_tooltip_hide();
					});
				}
			})(data[i]);
		}
		// position and size
		var bt = off.top - st + $icon.position().top - 96;
		var bl = off.left + self.cell.element.width() + 10;
		var bh = Math.min($d.height()+50,$(window).height()-bt-70);
		$b.css({'position':'fixed','top':bt+'px','left':bl+'px', 'z-index': 20001});
		$d.css({'height': bh+'px'});
		if (bl+$b.width() > $(window).width())
		{
			bl = self.cell.element.offset().left - 310;
			$b.css({'left':bl+'px'});
			$k.addClass('right');
		}
		//add the custom scrollbar
		$d.jScrollPane("", true);
		
		// put cell above everything
		self.cell.element.appendTo($(document.body));
		self.cell.element.css({'position':'fixed','top':(off.top-st)+'px','left':off.left+'px', 'z-index': 22001});
		//save state to restore when help is closed
		self.help_ctx = { $op: $op, pos: pos, st: st, oz: oz, $gauze: $gauze, $b: $b }
		//connect all the close functions
		var do_close = function(){ self.close_help(); };
		$gauze.click(do_close);
		$x.click(do_close);
		$(window).bind('resize', function()
		{
			$(window).unbind('resize',do_close);
			do_close();
		});
		
	},
	
	close_help: function()
	{
		var self = this;
		if (!self.help_showing)
			return;
		//$(document.body).css({'overflow':'auto'});
		enable_scroll_wheel();
		$(document.body).scrollTop(self.help_ctx.st);
		self.cell.element.appendTo(self.help_ctx.$op);
		self.cell.element.css({'position':'absolute','top':self.help_ctx.pos.top+'px','left':self.help_ctx.pos.left+'px', 'z-index': self.help_ctx.oz });
		self.help_ctx.$gauze.remove();
		self.help_ctx.$b.remove();
		self.help_showing = false;
		self.help_ctx = null;
	}

};