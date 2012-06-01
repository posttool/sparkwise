
se.sparkwi.widget.viz.util.graph = function(widget)
	{
		var self = this; 
		$.extend(self, widget); //mix in widget props
		
		var legend_leading = 15;
		var debug = false;
		
		var ts = self.props['time-span'];
		if (isNaN(ts))
			ts = 1;
		else
			ts = parseInt(ts);

		var date_from = null;
		var date_to = null;
		if (ts == 0)
		{
			date_to = self.props['time-span-to'];
			date_from = self.props['time-span-from'];
		}
		else
		{
			var now = new Date().getTime();
			date_from = now - ts * se.sparkwi.DAY_IN_MS;
			date_to = now;
		}

		self.init = function()
		{
			self.is_correlation = self.data.corr != null;
			if (self.is_correlation)
			{
				self.vals = [];
				for (var i=0; i<self.data.corr.length; i++)
					self.vals.push( self.util.filter_nulls(self.data.corr[i].values ) );
				self.info = self.util.find_min_max_for_correlation(self.data.corr);
			}
			else
			{
				self.vals = [ self.util.filter_nulls(self.data.values) ];
				self.info = self.util.find_min_max_for_widget(self.data.values);
			}
			
			var l = 0;
			for (var i=0; i<self.vals.length; i++)
				l += self.vals[i].length;
			self.has_data = l!=0;
			// adjust values
			//self.info.dvalmin -= dvalrange*.1;
			//self.info.dvalmax += dvalrange*.1;
			//self.info.dvalrange += dvalrange*.2;
			//adjust dates
			//self.info.datemax = floor_time(new Date().getTime()) + se.sparkwi.DAY_IN_MS;
			//self.info.datemin = self.info.datemax - self.props['time-span']*se.sparkwi.DAY_IN_MS - se.sparkwi.DAY_IN_MS ;
			//self.info.daterange = self.info.datemax - self.info.datemin;
			self.info.datemax += se.sparkwi.DAY_IN_MS;
			self.info.datemin = floor_time(date_from);//floor_time(self.info.datemin);
			self.info.datemax = floor_time(date_to);//floor_time(self.info.datemax) + se.sparkwi.DAY_IN_MS;
			self.info.daterange = self.info.datemax - self.info.datemin;
			//
			self.show_x_label = self.height>210;
			self.show_y_label = self.width>220;
			self.show_legend = self.height>210;
			self.show_events = self.props['show-events'];
			//
			self.yo = self.$title.height(); //from top
			self.xo = self.show_y_label ?	35 : 6; //from left
			self.margin =	5; // from left * right
			self.w = self.width - self.margin - self.xo;
			self.w0 = self.info.daterange==0 ? self.w : self.w / self.info.daterange;
			self.bo = self.show_legend ? self.vals.length * legend_leading + 25 : 15; // from bottom
			if (self.show_legend && self.show_events)
				self.bo += legend_leading;
			self.h = self.height - self.yo - self.bo - (self.show_x_label ? 10 : 0);
		};
		
		self.draw = function()
		{
			draw_axes();
			for (var i=0; i<self.vals.length; i++)
				draw_graph(i);
			if (self.show_events)
				draw_events();
			if (self.show_legend)
				draw_legend();
			draw_rollovers();
			if (self.props['show-trend-goal'])
				draw_goals();
		};
		
		var draw_line = function(x,x1,y,y1)
		{
			var path = "M "+x+" "+y+" L "+x1+" "+y1;
			var p = self.paper.path(path);
			p[0].style['shape-rendering'] = 'crispEdges';
			return p;
		};

		var draw_axes = function()
		{
			var days = self.info.daterange / se.sparkwi.DAY_IN_MS;
			var weeks = days/7;
			var tinc = days<33 ? days : weeks;
			
			var draw_x_labels = function()
			{
				var xtexts = self.paper.set();
				var inc = self.info.daterange/tinc;
				var y = self.yo + self.h + 10;
				var next_x = 0;
				for (var i=0; i<tinc+1; i++)
				{
					var d = self.info.datemin+inc*i;
					if (isNaN(d))
						continue;
					var x = get_x(d);
					if (x<next_x)
						continue;
					var ta = i == 0 ? 'start' : i == tinc ? 'end' : 'middle';
					var txo = i == 0 ? -1 : i ==tinc ? +1 : 0;
					var t = self.paper.text(x+txo,y,format_date(d))
						.attr({fill: '#666', "font-family": 'Arial', "font-size": "10px", 'text-anchor': ta});
					xtexts.push(t);
					next_x = x + t.getBBox().width + 10;
				}
			}
			if (!self.has_data)
			{
				var y = self.h + self.yo;
				var x = self.w + self.xo;
				for (var y0=self.yo; y0<=y; y0+=self.h/4)
					draw_line(self.xo, self.xo + self.w, y0, y0).attr({'stroke': '#c7c7c7','stroke-width': 1, 'stroke-dasharray': '.'});
				for (var y0=self.yo; y0<=y; y0+=self.h/20)
					draw_line(self.xo-6, self.xo-2, y0,y0).attr({'stroke': '#b4b4b4','stroke-width': 1});
				for (var x0=self.xo; x0<=x; x0+=self.w/20)
					draw_line(x0, x0, y+2,y+6).attr({'stroke': '#b4b4b4','stroke-width': 1});
				if (self.show_x_label)
					draw_x_labels();
				return;
			}
			
			var draw_hs = function(num_horz_lines, x0, x1)
			{
				var set = self.paper.set();
				var inc = self.info.dvalrange/num_horz_lines;
				for (var i=0; i<num_horz_lines+1; i++)
				{
					var y = get_y(self.info.dvalmin+inc*i);
					set.push(draw_line(x0, x1, y, y));
				}
				return set;
			};
			
			var draw_vs = function(num_lines, y, y1)
			{
				var set = self.paper.set();
				var inc = self.info.daterange/num_lines;
				for (var i=0; i<num_lines+1; i++)
				{
					var x = get_x(self.info.datemin+inc*i);
					set.push(draw_line(x, x, y, y1));
				}
				return set;
			};
			
			draw_hs(4,self.xo, self.xo + self.w ).attr({'stroke': '#c7c7c7','stroke-width': 1, 'stroke-dasharray': '.'});
			draw_hs(20,self.xo-6, self.xo-2).attr({'stroke': '#b4b4b4','stroke-width': 1});		
			
			draw_vs(tinc , self.yo+self.h+1, self.yo+self.h+5).attr({'stroke': '#b4b4b4','stroke-width': 1});
			//
			if (self.show_y_label)
			{
				var ytexts = self.paper.set();
				var inc = self.info.dvalrange/4;
				for (var i=0; i<5; i++)
				{
					var v = self.info.dvalmin+inc*i;
					var y = get_y(v);
					ytexts.push(self.paper.text(26,y,self.util.number_to_size(v))
							.attr({fill: '#666', "font-family": 'Arial', "font-size": "10px", 'text-anchor': 'end'}));
				}
			}
			if (self.show_x_label)
				draw_x_labels();
		};
		
		var draw_goals = function()
		{
			//trend limits
			var trend_goal = function(prop_name, rollover_text)
			{
				if (self.props[prop_name]==null)
					return;
				var tl = Number(self.props[prop_name]);
				if (tl>self.info.dvalmin && tl<self.info.dvalmax)
				{
					var y = get_y(tl);
					draw_line(self.xo, self.xo + self.w, y, y)
						.attr({'stroke': '#f00','stroke-width': 1, 'stroke-dasharray': '.'});
					draw_line(self.xo, self.xo + self.w, y, y)
						.attr({'stroke': '#f00','stroke-width': 4, 'opacity': 0})
						.mouseover(function(){
							graph_tooltip_show(null,rollover_text+' '+self.props[prop_name]); 
						})
						.mouseout(function(){
							graph_tooltip_hide();
						});
				}
			}
			trend_goal('trend-goal-low', 'Baseline');
			trend_goal('trend-goal-high', 'Goal');

		}
		
		var draw_graph = function(idx)
		{
			var dvals = self.vals[idx];
			var line_color = get_color(idx);
			
			/*	the line graph */
			var path = "";
			for(var i = 0; i < dvals.length; i++)
			{
				var val = dvals[i];
				if (val.data == null)
					continue;
				var x = get_x(val.date_utc, true); 
				if (x<self.xo)
					continue;
				var y = get_y(val.data.value);
				if (i==0)
					path += "M ";
				else
					path += "L ";
				path += x+" "+y+" ";
			}
			self.paper.path(path).attr({'stroke': line_color, 'stroke-width': 1.5});

			/* and the rollover circles */
			var on_mouse_over_circle = function(val)
			{
				var label = get_label(idx);
				if (val.data.label!=null)
					label = val.data.label;
				var txt = '<b>'+Math.floor(val.data.value)+' '+label+'</b><br/>on '+sparkwise_local_date(val.date_utc);
				graph_tooltip_show(null,txt); 
			};
			var draw_circle = function(val)
			{
				if (val.data == null)
					return;
				var x = get_x(val.date_utc, true);
				var y = get_y(val.data.value);
				var c = self.paper.circle(x,y,4)
					.attr({ fill: line_color, stroke: 'none' });
				var c0 = self.paper.circle(x,y,8)
					.attr({ fill: '#0f0', opacity: 0 })
					.mouseover(function() { 
						c.attr({stroke: line_color, 'stroke-width':2});
						on_mouse_over_circle(val); 
						on_mouseover_date(val.date_utc); })
					.mouseout(function() { 
						graph_tooltip_hide(); 
						on_mouseout_date(val.date_utc); });
				// show details
				if (val.data.annotations != null && val.data.annotations.length != 0)
				{
					c0.click(function(){ 
						graph_tooltip_hide(); 
						draw_details(val); 
					});
				}
				c.hide();
				c0.hide();
				var key = get_key_for_date(val.date_utc);
				set_put(key,c);
				set_put(key,c0);
				data_put(key,idx,val.data.value);
			};
			
			for(var i = 0;i < dvals.length;i++)
				draw_circle(dvals[i]);
		};
		
		var draw_events = function()
		{
			var events = g_get_events_between_dates(self.info.datemax,self.info.datemin) ;
			var event_map = {};
			for (var i=0; i<events.length; i++)
			{
				var e = events[i];
				if (event_map[e.date] == null)
				{
					event_map[e.date] = [e];
				}
				else
					event_map[e.date].push(e);
			}
			
			var event_y = self.yo + self.h + 5;
			var ecolor = '#555';
			var draw_event_circle = function(date,events)
			{
				var event_x = get_x(date, true);
				var c0 = self.paper.circle(event_x, event_y, 4).attr({ fill: ecolor, stroke: 'none', opacity: 1 });
				var c1 = self.paper.circle(event_x, event_y, 5).attr({ fill: ecolor, stroke: 'none', opacity: 0 });
				var event_title;
				if (events.length==1)
					event_title = '<b>'+events[0].name+'</b>';
				else
				{
					event_title = '<ul>';
					for (var i=0; i<events.length; i++)
						event_title += '<li>'+events[i].name+'</li>';
					event_title += '</ul>'
				}
				event_title += '<br/>on '+sparkwise_local_date(date)
				
				c1
					.mouseover(function (e) { 
						c0.attr({stroke: ecolor, 'stroke-width':1.5});
						graph_tooltip_show(p, event_title); 
						on_mouseover_date(date);
					})
					.mouseout(function (e) { 
						c0.attr({stroke: 'none'});
						graph_tooltip_hide(); 
					})
					.click(function(e) { 
						draw_event_detail(events); 
						graph_tooltip_hide(); 
					})
					.toFront();
			};
			
			//for(var i = 0;i < events.length;i++)
			for(var k in event_map)
				draw_event_circle(Number(k),event_map[k]);
		};
		
		var draw_legend = function()
		{
			self.legend_col1 = self.paper.set();
			self.legend_col2 = self.paper.set();
			
			var legend_width = 0;
			var lyo = 33;
			for (var i=0; i<self.vals.length; i++)
			{
				var y = self.yo + self.h + lyo + i * legend_leading;
				var ltext = com_pagesociety_util_StringUtil.shorten(get_label(i),55);

				var label = self.viz_util.legend_label(self.paper, get_color(i), ltext);
				label.translate(0, y);
				legend_width = Math.max(legend_width, label.getBBox().width);// find max width
			}
			for (var i=0; i<self.vals.length; i++)
			{
				var y = self.yo + self.h + lyo + i * legend_leading +5;
				var t = self.paper.text(41+legend_width,y,"");
				t.attr({'fill': '#666', 'font-family': 'Arial', 'font-size': '11px', 'text-anchor': 'start'});
				self.legend_col1.push(t);
				var t1 = self.paper.text(101+legend_width,y,"");
				t1.attr({'fill': get_color(i), 'font-family': 'Arial', 'font-size': '11px', 'text-anchor': 'start'});
				self.legend_col2.push(t1);
			}
			if (self.show_events)
			{
				var y = self.yo + self.h + lyo + i * legend_leading;
				var label = self.paper.set();
				var r = self.paper.circle(8,4,4);
				r.attr({'fill': '#666', 'stroke-width':0, 'stroke': 'none'});
				var t = self.paper.text(23,5,"Events");
				t.attr({'fill': '#666', 'font-family': 'Arial', 'font-size': '11px', 'text-anchor': 'start'});
				label.push(r,t);
				label.translate(0, y);
				legend_width = Math.max(legend_width, label.getBBox().width);// find max width
				var t = self.paper.text(41+legend_width,y,"");
				t.attr({'fill': '#666', 'font-family': 'Arial', 'font-size': '11px', 'text-anchor': 'start'});
				self.legend_col1.push(t);
				var t1 = self.paper.text(101+legend_width,y,"");
				t1.attr({'fill': '#666', 'font-family': 'Arial', 'font-size': '11px', 'text-anchor': 'start'});
				self.legend_col2.push(t1);
			}
		};
		
		var draw_rollovers = function()
		{
			var make_rect = function(d)
			{
				var x0 = get_x(d,true,-se.sparkwi.HALF_DAY_IN_MS) ;
				var x1 = get_x(d+se.sparkwi.HALF_DAY_IN_MS) ;
				return self.paper.rect(x0,self.yo,x1-x0,self.h)
					.attr({fill:'#ff0',opacity:0})
					.mouseover(function(){ on_mouseover_date(d); })
					.mouseout(function(){ on_mouseout_date(d); });
			};
			for (var i=self.info.datemin; i<=self.info.datemax; i+= se.sparkwi.DAY_IN_MS)
				make_rect(i);
		};
		
		
		var draw_details = function(val)
		{
			self._$p.hide();//_$p is the raphael dom element container
			if (self.$details==null)
				self.$details = $$div(self.element, 'details small');
			var t = self.$title.height() + 8;
			se.sparkwi.widget.viz.util.$feed(self.$details, val.data.annotations, self.height, t);
			self.$details.show();
			var $back = $$div(self.$details, 'back').css({'top': (t-8)+'px'});
			$back.click(function(){
				self.$details.empty();
				self._$p.show();
			});
		}

		var draw_event_detail = function(events)
		{
			self._$p.hide();//_$p is the raphael dom element container
			if (self.$details==null)
				self.$details = $$div(self.element, 'details small');
			var t = self.$title.height() + 8;
			self.$content = $$div(self.$details,'content').css({'position':'absolute','top':(t+3)+'px','height':(self.height-t-3)+'px'});
			self.$content.append("<div class='item'><strong class='date'>"+sparkwise_local_date(events[0].date)+"</strong> ");

			for (var i=0; i<events.length; i++)
			{
				var event = events[i];
				var $fi = $("<p><strong>"+event.name+"</strong><br/>"+event.description+" </p>");
				self.$content.append($fi);
			}
			self.$details.show();
			var $back = $$div(self.$details, 'back').css({'top': (t-8)+'px'});
			$back.click(function(){
				self.$details.empty();
				self._$p.show();
			});
		}

		var on_mouseover_date = function(d)
		{
			var set = set_get(get_key_for_date(d));
			if (set==null) return;
			set.toFront();
			set.show();
			if (!self.show_legend)
				return;
			var da = data[get_key_for_date(d)];
			for (var i=0; i<self.vals.length; i++)
			{
				self.legend_col1[i].attr({text: format_date(d)});
				var t = da[i];
				if (t==null) t = "";
				self.legend_col2[i].attr({text: t});
			}
			if (self.show_events)
			{
				var events = g_get_events_between_dates(d+se.sparkwi.DAY_IN_MS,d-se.sparkwi.DAY_IN_MS) ;
				self.legend_col1[i].attr({text: format_date(d)});
				self.legend_col2[i].attr({text: events.length});
			}
		};
		var on_mouseout_date = function(d)
		{
			var set = set_get(get_key_for_date(d));
			if (set==null) return;
			set.hide();
			if (!self.show_legend)
				return;
			for (var i=0; i<self.vals.length; i++)
			{
				self.legend_col1[i].attr({text: ''});
				self.legend_col2[i].attr({text: ''});
			}
		};

		var get_label = function(idx)
		{
			var s;
			if (self.is_correlation)
				if (self.children[idx].props != null && self.children[idx].props.title != null)
					s = self.children[idx].props.title;
				else
					s = self.data.corr[idx].name.toLowerCase()+" for "+self.util.get_name_for(self.children[idx]);
			else if (self.title != null)
				s = self.title;
			else
				s = self.def.name;
			if (s.indexOf('get')==0)
				s = s.substring(4);
			return s;
		};
		
		var get_color = function(idx)
		{
			if (self.is_correlation)
			{
				var proxy = self.children[idx];
				var c = self.props['item_color_'+proxy.id];
				if (c!=null)
					return c;
				else
					return get_color_for_proxy(proxy);
	
			}
			return ColorUtil.toHexString(self.props._line_color)
		};
		
		var get_x = function(time,quantize,offset) //date & offset are in ms
		{
			var d = time;
			if (quantize)
				d = floor_time(time);
			if (offset)
				d += offset;
			return self.xo + self.w0 * (d - self.info.datemin);
		};
		
		var floor_time = function(time)
		{
			return Math.ceil(time/se.sparkwi.DAY_IN_MS)*se.sparkwi.DAY_IN_MS;
		}
		
		var get_y = function(val)
		{
			return Math.floor((self.info.dvalrange == 0) ? self.h*.5 : 
			              self.h - (((val-self.info.dvalmin) / self.info.dvalrange) * self.h)) + self.yo;
		};
		
		var format_date = function(d)
		{
			var d0 = new Date(d);
			return se.sparkwi.MONTHS[d0.getMonth()]+" "+d0.getDate();
		};
		
		var get_key_for_date = function(d)
		{
			var o = new Date(floor_time(d));
			return o.getFullYear()+"_"+o.getMonth()+"_"+o.getDate();
		};
		
		var index = {};
		var set_put = function(k,e)
		{
			var v = index[k];
			if (v==null)
				v = self.paper.set();
			v.push(e);
			index[k] = v;
		};
		
		var set_get = function(k)
		{
			return index[k];
		};
		
		var data = {};
		var data_put = function(k, idx, val)
		{
			var v = data[k];
			if (v==null)
				v = [];
			if (v[idx]==null)
				v[idx] = [];
			v[idx] = val;
			data[k] = v;
		};

	};
