
jQuery.extend(se.sparkwi.widget.viz.util,
{
	render: function(self)
	{
		$(self.element).empty();
		if (self.paper != null)
			self.paper.clear();
		if (self.display_type == null )
			self.display_type = se.sparkwi.DISPLAY_PER_DAY_TOTAL;
		if (self.data==null || self.data.values==null)
		{
			return;
		}
		var ct = get_widget_category(self.def).name;
		switch(self.props['display-style'])
		{
			case 'feed':
				self.viz_util.feed(self);
				break;
			case 'percent-change':
				self.viz_util.percent_change(self);
				break;
			case 'percent-complete':
				self.viz_util.percent_complete(self,se.sparkwi.colors[ct]);
				break;
			case 'trend':
				self.viz_util.trend(self, se.sparkwi.colors[ct]);
				break;
			case 'pie':
				if (self.data.values[0].data!=null)
					self.viz_util.pie(self, self.data.values[0].data.dict, se.sparkwi.colors[ct]);
				break;
			case 'map':
 				self.viz_util.map(self, [ self.data.values[0].data.geo_values ], 
 						[ self.title ], [ se.sparkwi.colors[ct] ]);
 				break;
			default:
				self.viz_util.total(self);
				break;
		}
	},
	
	
	size_type: function($text, width, font_size_min, font_size_max)
	{
		var fs = font_size_max;
		$text.css({'font-size':fs+'px'});
		var tw = $text.width();
		var get_baseline = function(fs){ return (1 - (fs-font_size_min)/(font_size_max-font_size_min)); };
		while (tw>width)
		{
			fs = Math.floor(fs-1);
			$text.css({'font-size':fs+'px'});
			tw = $text.width();
			if (fs<font_size_min)
			{
				$text.css({'font-size':font_size_max+'px'});
				return { size: font_size_max, baseline: get_baseline(fs), too_small: true, width: $text.width(), height: $text.height() };
			}
		}
		return { size: fs, baseline: get_baseline(fs), width: $text.width(), height: $text.height() };
	},

	
	trend: function(self,color)
	{
		self.viz_util.title_and_icon(self);
		if (self.data==null || self.data.values==null)
			return;
		
		self.props._line_color = color;
		var graph = new self.viz_util.graph(self);
		graph.init();
		graph.draw();

	},
	
	legend_label: function(paper,color,text)
	{
		var label = paper.set();
		var r = paper.rect(0,0,16,10,2);
		r.attr({'fill': color, 'stroke-width':0, 'stroke': 'none'});
		var t = paper.text(23,5,text);
		t.attr({'fill': '#666', 'font-family': 'Arial', 'font-size': '11px', 'text-anchor': 'start'});
		label.push(r,t);
		return label;
	},
	
	label: function(paper,text,family,size,color)
	{
		var label = paper.text(0,4,text);
		label.attr({'fill': color, 'font-family': family, 'font-size': size+'px', 'text-anchor': 'start'});
		return label;
	},

	title_and_icon: function(self)
	{
		var title = null;
		if (self.props.title)
			title = self.props.title;
		else if (self.title!=null)
			title = self.title;
		if (title)
		{
			self.$title = $('<div class="widget-label">'+title+'</div>');
			self.element.append(self.$title);
			self.element.append('<div class="widget-icons">&nbsp;</div>');
			self.title_height = self.$title.height() + 8;
		}
		else
		{
			self.$title = $('<div></div>');
			self.title_height = 0;
		}
	},
	
	wrap: function(self)
	{
		self.$wrap = $("<div></div>").css({ 
			'position':'absolute', 
			'top': self.title_height, 
			'width':'100%',
			'height': self.height-self.title_height,
			'overflow': 'hidden'});
		self.element.append(self.$wrap);
	},
	

	//unused raphael based svg glyph plotting
	print: function(paper, x, y, str, font, size, letter_spacing)
	{
		var letters		= str.split(''),
			font		= paper.getFont(font),
			isBaseLine	= true; 
		var out = paper.set();
		var scale = (size || 16) / font.face["units-per-em"];
		var shift = 0;
		var origin = "baseline";
		letter_spacing = Math.max(Math.min(letter_spacing || 0, 1), -1);
	
		var bb = font.face.bbox.split(/[, ]+/),
			top = +bb[0],
			height = +bb[1] + (origin == "baseline" ? bb[3] - bb[1] + (+font.face.descent) : (bb[3] - bb[1]) / 2);
		for (var i = 0, ii = letters.length; i < ii; i++) {
			var prev = i && font.glyphs[letters[i - 1]] || {},
				curr = font.glyphs[letters[i]];
			shift += i ? (prev.w || font.w) + (prev.k && prev.k[letters[i]] || 0) + (font.w * letter_spacing) : 0;
			curr && curr.d && out.push(paper.path(curr.d).attr({
				fill: "#000",
				stroke: "none",
				transform: [["t", shift * scale, 0]]
			}));
		}
		out.transform(["...s", scale, scale, top, height, "t", (x - top) / scale, (y - height) / scale]);
		var wrap = paper.set();
		wrap.push(out);
		return wrap;
		
	}

});