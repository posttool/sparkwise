se.sparkwi.widget.viz.util.pie = function(self, dict, basecolor)
	{
			
	self.viz_util.title_and_icon(self);

		// now start the pie
		// defaults
		if(self.pie_display_percent_of_total == null)
			self.pie_display_percent_of_total = false;
		if(self.pie_display_key_map == null)
			self.pie_display_key_map = {};

		// size info
		var is_one_cell_high = self.height<300,
			is_one_cell_wide = self.width<300;

		// convert dict results into sorted array
		var darray = [];
		for (var p in dict)
			if (p!='' && dict[p] != '')
				darray.push({ name: p, value: Number(dict[p]) });
		
		darray.sort(function(a,b){ return b.value - a.value; });
		//shorten lists greater than 
		if (darray.length > 8)
			darray = darray.slice(0, 8);
		// total
		var total = 0;
		for (var i=0; i<darray.length; i++)
			total += Number(darray[i].value);

		// create a list of colors from the basecolor
		var colors = [ ];
		if (basecolor)
		{
			var inc = 1/(darray.length-1);
			for (var i=0; i<darray.length; i++)
			{
				var f = (inc*i)*.6+.4;
				var c = ColorUtil.multiplyIntAsRGB(basecolor, f);
				colors.push(ColorUtil.toHexString(c));
			}
		}
		else
		{
			//colors = self.viz_util.get_comparison_colors(self.props, darray.length);
			for (var i=0; i<self.children.length; i++)
			{
				var proxy = self.children[i];
				var c = self.props['item_color_'+proxy.id];
				if (c!=null)
					colors.push(c);
				else
					colors.push(get_color_for_proxy(proxy));
			}
		}
		
		// draw legend and measure its dimensions 
		var legend_width = 0,
			legend_height = 0,
			legend_leading = 15,
			show_legend = !(is_one_cell_high && is_one_cell_wide) || self.always_show_legend;
		if (show_legend)
		{
			legend_height = darray.length*legend_leading + legend_leading + 10;
			var y = self.height - legend_height ;			
			for (var i = 0; i < darray.length; i++)
			{
				var lname = self.pie_display_key_map[darray[i].name] || darray[i].name;
				var label = self.viz_util.legend_label(self.paper,colors[i], lname);
				label.translate(0, y);
				legend_width = Math.max(legend_width, label.getBBox().width);// find max width
				y += legend_leading;
			}

			y += 6;
			var ds;
			switch(self.display_type)
			{
				case se.sparkwi.DISPLAY_ABSOLUTE_TOTAL:
					if(self.pie_display_percent_of_total)
						ds = "Totals and % as of";
					else
						ds = "Totals as of";		
					break;
				//case sparkwise.DISPLAY_PER_DAY_TOTAL:
				default:
					if(self.pie_display_percent_of_total)
						ds = "Totals and % on";
					else
						ds = "Totals on";		
					break;
			}

			var label_text = ds+" ";
			if (self.data.values!=null)
				label_text += sparkwise_local_date(self.data.values[0].date_utc);
			else
				label_text += sparkwise_local_date(self.data.corr[0].values[0].date_utc);
			var label = self.viz_util.label(self.paper,label_text, "Helvetica, Arial",11,"#666");
			label.translate(0, y);
			legend_width = Math.max(legend_width, label.getBBox().width);// max width?
		}
		
		// modify the dimensions & offset based on various factors
		var dh = !is_one_cell_high ? legend_height : 0;
		var dw = is_one_cell_high ? legend_width : 0;
		var cx = (self.width - dw) * .5;
		var dx = dw;
		var cy = (self.height - dh) * .5;
		var r = Math.min(cx, cy);
		r -= 10; // margin
		if (r>95)
		{
			r -= 10; // more margin
		}
		if (r>175)
		{
			r = 175; // max size
		}
		if (self.title_height>14) // multiline title
		{
			cy += 10;
			r -= 10;
		}

		// grey background / 4px border
		self.paper.circle(cx+dx,cy,r+4).attr({fill:"#d3d3d3",stroke:"none"});
		
		// sector drawing 
		var angle = 0,
			rad = Math.PI / 180,
			draw_sector = function(cx, cy, r, startAngle, endAngle)
			{
				var rad = Math.PI / 180;
				var x1 = cx + r * Math.cos(-startAngle * rad);
				var x2 = cx + r * Math.cos(-endAngle * rad);
				var y1 = cy + r * Math.sin(-startAngle * rad);
				var y2 = cy + r * Math.sin(-endAngle * rad);
				return self.paper.path(["M", cx, cy, "L", x1, y1, "A", r, r, 0, +(endAngle - startAngle > 180), 0, x2, y2, "z"]);
			},
			create_sector_ui = function(name, value, color)
			{
				var angleplus = 360 * value / total;
				var s = draw_sector(cx+dx, cy, r, angle, angle + angleplus).attr({ fill: color, stroke: color});
				s.mouseover(function (e) 
				{
					var valname = self.pie_display_key_map[name] || name;
					var valtext = valname+": "+value;
					if(self.pie_display_percent_of_total)
						valtext += " [ "+Math.round(value/total*100)+"% ]";
					graph_tooltip_show(null,valtext);
				});
				s.mouseout(function () 
				{
					graph_tooltip_hide();
				});
				angle += angleplus;
			};
			
		// for each value, make a sector
		for (var i = 0; i < darray.length; i++)
			create_sector_ui(darray[i].name, darray[i].value, colors[i] );	

	}
