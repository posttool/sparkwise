// TODO all uses of cell methods should dispatch/trigger an event instead of accessing cell methods directly
// TODO all reads of _data, _def, etc // those fields should be copied here, like the viz

se.sparkwi.widget.config.mixins = {

    render: function($p)
    {
		var self = this;
		var def = self.cell.definition();
		var conn = self.cell._data.proxy.connection;
		var ready_for_selectors = def.required_connection_type == "None" || def.required_connection_type == "Raw" || conn != null;
		$p.css({'max-width':'230px'});
		self.error($p);
		self.connection($p);
		if (ready_for_selectors)
		{
			self.selectors($p);
			var $props = $$div($p,'widget-props-container');
			if (self.render_display_props)
				try{ self.render_display_props($props); } catch (e) { logging.log(e); }
			else
				self.standard($props);
			self.actions($p);
		}
		self.buttons($p);
    },	
    
  
    error: function($p)
    {
    	var self = this;
	    var proxy_state = self.cell.proxy_state();
	    if (proxy_state == PROXY_STATE_FAILED_DATA_ERROR)
	    {
			var err_msg = '<strong>There was a problem.</strong>'+self.cell._data.proxy.last_failure_message;
			$p.append($('<div class="widget-props-panel-error">'+err_msg+'</div>'));
			return;
	    }
	    if (self.cell._data.values != null && self.cell._data.values.length != 0 && self.cell._data.values[0].error_message != null)
	    {
	    	//console.log("ERROR",self.cell._data)
			var err_msg = '<strong>There was a problem.</strong>'+self.cell._data.values[0].error_message;
			$p.append($('<div class="widget-props-panel-error">'+err_msg+'</div>'));
			return;
	    }
    },
    
	connection:function($p)
	{
		var self = this;
		var def = self.cell.definition();
		var data = self.cell.data();
		if (def.required_connection_type == "None" || def.required_connection_type == "Raw")
			return;
		
		var refresh_conn_f = function(conn)
		{
			$_connections.add_connection(conn);
			do_module('Dashboard/SyncWidget',[data.id], function(wi)
			{
				self.cell.data(wi);
			});
		};
		var update_conn_ui = function()
		{
			var state = self.cell.state();
			var conns = $_connections.get_by_type(def.required_connection_type);
			var has_connection = data.proxy.connection != null;
			
			var $c = $$div($p, 'widget-connection-container');
			if (!has_connection)// || state == PROXY_STATE_FAILED_DATA_ERROR
			{
				$$button($c, def.required_connection_type, {}, function(e)
					{ 
						create_conn_for_wi(data.id, refresh_conn_f); 
						return false; 
					});
				$$label($c, "When connected, your widget will update once a day.","add_connection_text");
				$$br($c);
			}
			else
			{
				var $sel = $$select($c, {}, function(e){ self.cell._update_connection($sel.val()); });
				for (var i=0; i<conns.length; i++)
				{
					var conn = conns[i];
					var selected = has_connection && conn.id == data.proxy.connection.id;
					var un = com_pagesociety_util_StringUtil.shorten(conn.username,19);
					$sel.add_option(un,selected,conn.id);
				}
			}
		};
		update_conn_ui();
	},
	
	selectors:function($p)
	{
		var self = this;
		var options = self.cell.options;
		if (!options.is_editable)
			return;
		var def = self.cell.definition();
		var data = self.cell.data();

		var $d = $$div($p,'widget-selector-container');
		var selector_infos = def.service_selector_info;
		var cachekey = data.proxy.connection == null ? '' : data.proxy.connection.id+'_';
		for(var i = 0;i < selector_infos.length;i++)
		{
			var selector_info 		= selector_infos[i];
			var stype 	 			= selector_info.type;
			var sname 	 			= selector_info.name;
			var slabel 	 			= selector_info.label || sname;
			var selected_value	 	= data.proxy.selector_values == null ? null : data.proxy.selector_values[sname];
			
			switch (stype)
			{
				case SELECTOR_TYPE_SELECT:
					self.add_option_selector($d,sname,slabel,selected_value,cachekey);
					break;
				case SELECTOR_TYPE_FREE_TEXT:
					self.add_input_selector($d,slabel,sname,selected_value,selector_info.input_behavior,selector_info.input_behavior_val);
					break;
			}
			
			if (selected_value!=null)
				cachekey += selected_value;
		}
	},
	
	_selector_cache: {},
	add_option_selector: function($p, name, label, value, cachekey)
	{
		var self = this;
		var def = self.cell.definition();
		var data = self.cell.data();
		var $sel = $$select($p, {}, function(e)
			{
				var dv = $sel.get_selected().data('title');
				self.cell._update_selector_new(name, $sel.val(), dv );
			}, name);
		$sel.add_option("Loading...");
		$sel.attr('disabled', true);
		
		var get_sel_data = function()
		{
			var load_data = function()
			{
				do_module('Dashboard/GetSelectorDataForWidget',[data.id, name], on_got_data,
				function(err_msg)
				{
					err_count++;
					if (err_count>3)
						return;//TODO log this
					$sel.empty();
					$sel.add_option('Error- refresh in 3 seconds...');
					setTimeout(function(){
						$sel.empty();
						$sel.add_option('Refreshing...');
						get_sel_data();
					},3000);
				});
			}			
			
			var ckey = data.proxy.widget.id+'-'+cachekey+'-'+name;
			var on_got_data = function(obj)
			{
				se.sparkwi.widget.config.mixins._selector_cache[ckey] = obj;
				var selector_display_vals = obj.display_values;
				var selector_vals = obj.values;
				if (selector_vals.length == 0)
				{
					$p.append($('<div class="widget-props-panel-error">You must create a '+name+' before you can collect data with this widget.</div>'));
					self.cell.updatePropsHeight();
					$sel.hide();
				}
				$sel.empty();
				if(selector_vals.length > 0 && selector_vals[0].startsWith("_waiting"))
				{
					$sel.attr('disabled', true);
					$sel.add_option('Then '+label+'...',true,selector_vals[0]);
				}
				else 
				{
					$sel.attr('disabled', false);
					$sel.add_option('Choose '+label+'...',false,selector_vals[0]);
				}
				for(var i = 0;i < selector_vals.length;i++)
				{
					var selected = (value != null && value == selector_vals[i]);
					if (value == null && i == 0 && selector_vals.length == 1 && selector_vals[i] != '_waiting')
					{
						selected = true;
						self.cell._update_selector_new(name, selector_vals[i], selector_display_vals[i] );
					}
					var dv = com_pagesociety_util_StringUtil.shorten(selector_display_vals[i],19);
					var $o = $sel.add_option(dv, selected, selector_vals[i]);
					$o.data('title', selector_display_vals[i]);
				}
			}
			
			//go
			var err_count = 0;
			if (se.sparkwi.widget.config.mixins._selector_cache[ckey] != null)
				on_got_data(se.sparkwi.widget.config.mixins._selector_cache[ckey]);
			else
				load_data();
		};
		get_sel_data();
		self.add_help_to_select($p, $sel, name);
	},
	
	add_input_selector: function($d,slabel,sname,selected_value,behavior,behavior_val)
	{
		var self = this;
		var $input = $$input2($d, slabel, sname, 30, null, behavior, behavior_val);
		$input.setValue(selected_value);
		$input.change(sname, function(e)
		{
			if (e.data==null)
				return;
			self.cell._update_selector_new(sname,$input.getValue(),$input.getValue());
		});
		self.add_help_to_input($d, $input, sname);
	},

	
	_requires_props: function()
	{
		var self = this;
		return self.standard_requires_props();
	},

	buttons:function($p)
	{
		var self = this;	
		var def = self.cell.definition();
		var data = self.cell.data();
		var widget_state = self.cell.state();
		var proxy_state = self.cell.proxy_state();
		
		var $done = $$div($p, 'widget-done-wrapper');
		switch(widget_state)
		{
			case WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS:
				self.cell._$close.hide();
				self.add_buttons($done,false,false,false,true,true)
				return;
		}
		
		switch(proxy_state)
		{
			case PROXY_STATE_FAILED_DATA_ERROR:
				self.add_buttons($done,false,true,true,true,true)
				break;
			case PROXY_STATE_REQUIRES_AUTH:
			case PROXY_STATE_REQUIRES_SERVICE_SELECTION:
				self.cell._$close.hide();
				self.add_buttons($done,false,false,false,true,true)
				break;
			case PROXY_STATE_OK:
				self.cell._$close.show();
				var in_set = $("#widget-chooser").widget_selector("is_in_my_set",data);//TODO remove global lameness
				self.add_buttons($done,true,true,true,true,in_set);
				break;
		}
		$done.append("<br clear='all'/>");
	},
	
	add_buttons: function($done, embed_active, done_active, cancel_active, delete_active, is_in_my_set)
	{
		var self = this;	
		var close_overlays = function(){
			help_tooltip_hide(0);
			self.close_help(); 
		};
		var $a;
		
		$a = $$div($done, 'bottom-bar-button widget-embed-button');
		if (embed_active)
		{
			$a.click(function(){ 
				close_overlays();
				self.cell._embed_widget(); 
			});
			$a.mouseover(function(){ help_tooltip_show('Embed'); }).mouseout(function(){ help_tooltip_hide(); });
		}
		else
			$a.addClass('inactive');
			
		$a = $$div($done, 'bottom-bar-button widget-done-button');
		if (done_active)
		{
			$a.click(function(){ 
				close_overlays();
				self.cell._exit_config(true);
			});
			$a.mouseover(function(){ help_tooltip_show('Done'); }).mouseout(function(){ help_tooltip_hide(); });
		}
		else
			$a.addClass('inactive');

		$a = $$div($done, 'bottom-bar-button widget-cancel-button');
		if (cancel_active)
		{
			$a.click(function(){ 
				close_overlays();
				self.cell._exit_config(false);
			});
			$a.mouseover(function(){ help_tooltip_show('Cancel'); }).mouseout(function(){ help_tooltip_hide(); });
		}
		else
			$a.addClass('inactive');

		$a = $$div($done, 'bottom-bar-button widget-delete-button');
		if (delete_active)
		{
			$a.click(function(){ 
				close_overlays();
				self.cell.options.deletef(self);
			});
			$a.mouseover(function(){ help_tooltip_show('Delete'); }).mouseout(function(){ help_tooltip_hide(); });
		}
		else
			$a.addClass('inactive');

		var $myset = $$div($done, 'bottom-bar-button widget-myset-button');
		if (!is_in_my_set)
		{
			$myset
				.mouseover(function(){ help_tooltip_show('Save to<br/>My Set'); })
				.mouseout(function(){ help_tooltip_hide(); })
				.click(function(e)
				{
					close_overlays();
					$myset.addClass('inactive');
					do_module('Dashboard/SaveToMySet', [self.cell._data.id], function(e)
					{
					    $("#widget-chooser").widget_selector("add_to_my_set",e);//TODO remove global lameness
					    $myset.unbind("click");
					});
				});	
		}
		else
			$myset.addClass('inactive');
				
	},
	
	
	actions:function($p)
	{
		var self = this;
		var $d = $$div($p,'widget-action-container');
		$$label($d,'Engage Your Audience','engage-label');
		var $text = $$input2($d, 'Call to Action', 'action-text', 28);
		$text.setValue(self.cell._data.props['action-text']);
		var $url = $$input2($d, 'URL', 'action-link', 28, function(v)
		{
			if (v!="" && v.indexOf("http://")!=0)
				$url.setValue("http://"+v);
		 });
		$url.setValue(self.cell._data.props['action-link']);
	},
	
	events: function($p)
	{
		var self = this;
		var se = self.cell._data.props['show-events'];
		var $show_events = $$div($p,'item');
		var $se = $$checkbox($show_events,'show-events','Show Events',se);
		var $ae = $('<div class="add_event"> Add Event</div>');
		$ae.click(function()
			{ 
				$('#events-dialog').events('open',true); 
			});
		$se.append($ae);
		return $show_events;
	},
	
	title: function($p)
	{
		var self = this;
		var $totali = $$div($p,'item');
		var $total = $$input2($totali, 'Title', 'title');
		$total.setValue(self.cell._data.props['title']);
		return $totali;
	},
	

	/**
	 * the standard props form for proxies that return numbers, geodata, etc.
	 */
	standard: function($p)
	{
		var self = this;
		
		var ds = self.cell._data.props['display-style'];
		var $dsi = $$div($p,'item');
		var $ds = $$select($dsi,null,null,'display-style');
		self.$ds = $ds;
		if (self.cell.returns_annotated_number())
			$ds.add_options([['feed', 'Feed']],ds);
		if (self.cell.returns_number())
			$ds.add_options([['total', 'Total'],
			                  ['percent-change','Total &amp; Percent Change'],
			                  ['percent-complete','Percent Complete'],
			                  ['trend','Trend']], ds);
		if (self.cell.returns_geodata())
			$ds.add_options([['map', 'Map']],ds);
		if (self.cell.returns_dict())
			$ds.add_options([['pie', 'Pie']],ds);


		var $$trend = self.trend($p);

		//for 'percent-complete'
		var g = self.cell._data.props['goal'];
		var $goali = $$div($p,'item');
		var $goal = $$input2($goali, 'Goal', 'goal');
		$goal.setValue(g);
		self.add_field_error($p,$goal,'goal');
		
		var dta = self.cell._data.props['display-total-as'];
		var $display_total_as = $$div($p,'item');
		var $dta = $$select($display_total_as,null,null, 'display-total-as');
		$dta.add_options([['','Rounded'],
					        ['rounded0','2 Decimals']], dta);
		/*			        ['currency','Currency'],
					        ['percent','Percent'],
					        ['percent0','Percent Rounded'],
					        ['abbreviation','Engineering Notation']*/
		var $show_events = self.events($p);

		function hide_all()
		{
			$$trend.hide();
			$goali.hide();
			$display_total_as.hide();
			$show_events.hide();
		}
		function show_one()
		{
			var v = $ds.val();
			hide_all();
			switch(v)
			{
				case 'total':
					$display_total_as.show();
					break;
				case 'trend':
					$$trend.show();
					$show_events.show();
					break;
				case 'percent-change':
					$$trend.show(false);
					break;
				case 'percent-complete':
					$goali.show();
					break;
			}
			self.cell.updatePropsHeight();
		}

		hide_all();
		show_one();
		
		$ds.change(function(e){ show_one(); });
		$$trend.change(function(e){ show_one(); });
	},
	
	trend: function($p)
	{
		var self = this;
		var ts = self.cell._data.props['time-span'];
		var $time_span = $$div($p,'item');
		var $ts = $$select($time_span,null,null, 'time-span');
		$ts.add_options([['7', 'Week'],
				        ['30','Month'],
				        ['90','Quarter'],
				        ['365','Year'],
				        ['0','Date Range...']], ts);
	
		//for trend
		var tsfrom = self.cell._data.props['time-span-from'];
		var $time_span_range = $$div($p,'item').css('margin-left', '2px');
		$time_span_range.append('From&nbsp;');
		var $tsfrom = $$date($time_span_range,'time-span-from');
		$tsfrom.setValue(tsfrom);
		$tsfrom.css({'width':'63px'});
		self.add_field_error($p,$tsfrom,'time-span-from');
		
		$time_span_range.append('&nbsp;To&nbsp;');
		
		var tsto = self.cell._data.props['time-span-to'];
		var $tsto = $$date($time_span_range, 'time-span-to');
		$tsto.setValue(tsto);
		$tsto.css({'width':'63px'});
		self.add_field_error($p,$tsto,'time-span-to');
		
		var se = self.cell._data.props['show-trend-goal'];
		var $show_trend_goal = $$div($p,'item');
		var $trend_goals_checkbox = $$checkbox($show_trend_goal,'show-trend-goal','Show Trend Goals',se);
		var $trend_goals = $$div($p,'item');
		var $trend_goal_high = $$input2($trend_goals,'Target Range High', 'trend-goal-high');
		$trend_goal_high.setValue(self.cell._data.props['trend-goal-high']);
		var $trend_goal_low = $$input2($trend_goals,'Target Range Low', 'trend-goal-low')
		$trend_goal_low.setValue(self.cell._data.props['trend-goal-low']);

		function show_trend_timespan()
		{
			if ($ts.val()=='0')
				$time_span_range.show();
			else
				$time_span_range.hide();
		}
		function show_trend_goals()
		{
			if ($trend_goals_checkbox.getValue())
				$trend_goals.show();
			else
				$trend_goals.hide();
		}
		function hide_all()
		{
			$time_span.hide();
			$time_span_range.hide();
			$show_trend_goal.hide();
			$trend_goals.hide();
		}
		function show(goals)
		{
			$time_span.show();
			show_trend_timespan();
			if (goals==null || goals)
			{
				$show_trend_goal.show();
				show_trend_goals();
			}
		}
		return {
			hide: hide_all,
			show:show,
			change: function(f)
			{
				$time_span.change(function(){ f(); });
				$trend_goals_checkbox.change(function(){ f(); });
			}
		}
	},
	
	standard_requires_props: function()
	{
		var self = this;
		var ds = self.cell._data.props['display-style'];
		self.field_error = {};
		if (ds == 'percent-complete')
		{
			var g = Number(self.cell._data.props['goal']);
			if (isNaN(g) || g<1)
			{
				self.field_error['goal'] = 'Goal must be a positive number.';
				return true;
			}
		} 
		else if (ds == 'trend')
		{
			var ts = Number(self.cell._data.props['time-span']);
			if (ts == 0)
			{
				var requires_props = false;
				var tsf = self.cell._data.props['time-span-from'];
				if (tsf==null || tsf<0)
				{
					requires_props = true;
					self.field_error['time-span-from'] = 'Choose start date.';
				}
				var tst = self.cell._data.props['time-span-to'];
				if (tst==null || tst<0 || tst<tsf)
				{
					requires_props = true;
					self.field_error['time-span-to'] = 'Choose valid end date.';
				}
				return requires_props;
			}
			
		}
		return false;
	},
	
	
	
	add_field_error: function($parent,$field,name)
	{
		var self = this;
		var is_f_error = self.field_error != null && self.field_error[name]!=null;
		if (is_f_error)
		{
			$$error_flag($parent, $field, self.field_error[name]);
			$field.css({'border':'1px solid #f00'});//change the word goal to red
		}

	}
 
}