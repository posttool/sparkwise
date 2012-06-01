/**
 * The dashboard cell represents the front (vizualization) and back (configuration	
 * panel) of each cell in the grid. Both back and front are rendered
 * dynamically by js classes with the same name as the java class. These
 * classes are packaged in se.sparkwi.widget.viz and se.sparkwi.widget.config.
 */

var WIDGET_STATE_INIT 							= 0x00;
var WIDGET_STATE_REQUIRES_DATA_PROXY 			= 0x10;
var WIDGET_STATE_REQUIRES_PROPS					= 0x15;
var WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS	= 0x20;
var WIDGET_STATE_OK 							= 0x40;

var PROXY_STATE_INIT							= 0x00;
var PROXY_STATE_REQUIRES_AUTH					= 0x10;
var PROXY_STATE_REQUIRES_SERVICE_SELECTION		= 0x20;
var PROXY_STATE_OK								= 0x30;
var PROXY_STATE_FAILED_DATA_ERROR 				= 0x40;

var WIDGET_TYPE_NORMAL 							= 0x00;
var WIDGET_TYPE_CORRELATION 					= 0x01;

var SELECTOR_TYPE_SELECT						= 0x00;
var SELECTOR_TYPE_FREE_TEXT						= 0x10;

var RETURN_TYPE_TEXT							= 0x00;
var RETURN_TYPE_NUMBER							= 0x10;
var RETURN_TYPE_ANNOTATED_NUMBER				= 0x11;
var RETURN_TYPE_GEODATA							= 0x20;
var RETURN_TYPE_DICT							= 0x30;

/* unfortunately we must access the raphaeljs container by id */
var WCELLID = 57489754;
/* g_current_config_zindex is used when we enter and exit config to keep things from overlapping */
var g_current_config_zindex = 1000;
/* if debug is false, the all errors will be caught so that the board doesn't fail... this setting
 * is appropriate for the production environment. if debug is true, errors will not be caught so 
 * that we can debug more easily.
 */
var DBCELL_DEBUG = false;

se.sparkwi.dashboard.cell = function(o)
{
	var el = $("<div></div>");
	var def = 
	{
		options: {width: 50, height: 50, is_editable: false,class_name:null },
	
		init: function()
		{
			var self = this;
			jQuery.extend(self.options,o);
	
			el.css('position','absolute');
			el.addClass('widget-grid-cell');
	
			/* front side of widget */
			self._$front = $$div(el,'widget-front');
			self._$front.css({ '-webkit-backface-visibility': 'hidden'});
	
			/* element content */
			self._$c = $$$(self._$front, null, 19, null, null, 10);//.css({'overflow':'hidden'})
	
			/* raphael content */
			WCELLID++;
			self.pid = "__wc"+WCELLID;
			self._$p = $$$(self._$front, null, 30, null, null, 10).attr('id',self.pid);
	
			/* drag handle */
			self._$drag_handle = $$$(self._$front, 'widget-drag-handle');
			self._$drag_handle.css({cursor:'pointer'});
			self._$drag_handle_shadow = $$div(self._$front,'widget-top-shadow');
		
			/* title */
			self._$title = $$$(self._$front, 'widget-title', 26, null, null, 10);
	
			/* bottom part */
			self._$action_bar = $$$(self._$front, 'widget-action-bar', null, null, 31, null);
			self._$action_text = $$$(self._$action_bar, 'widget-action-text');
			if (self.options.is_editable)
				self._$edit_widget = $$$(self._$action_bar, 'widget-edit-button', 0, 18, null, null, function(){ self.do_enter_config(); });
	
			/* editbar*/
			self._$edit_widget1 = $$div(self._$front,'widget-bottom-shadow');
			
			/* data error */
			self._$no_data = $$div(self._$front,'widget-no-data');
			$$div(self._$no_data,'widget-no-data-bubble').append('<p>This feed needs time to gather information. Kick back, relax &ndash; and check back in a while to see your data.</p>');
			$$div(self._$no_data,'widget-no-data-close-button').click(function(){
				self._$no_data.hide();
			});
			
			/* add the edit/style panel if necessary */
			if (self.options.is_editable)
			{
				/*	when visible it indicates that widget can be dropped on a correlation */
				self._$plus = $$div(el, 'widget-over-correlation widget-correlation-cell');
				self._$minus = $$div(el, 'widget-over-correlation-but-not-droppable');
				
				self._$minus.append('<div class="icon-not-droppable">&nbsp;</div>');
				self._$minus.append('<div class="bg-not-droppable">&nbsp;</div>');

				/* back side */
				self._$back = $$$(el, 'widget-props-panel');
				self._$back.css('height',self.options.height+'px');
				self._$back.click(function(){
					++g_current_config_zindex;
					self.element.css("z-index",g_current_config_zindex);;
				});
				self._$back.hide();
	
				self._$props_top = $$form(self._$back, 'widget-props');
				self._$state = $$div(self._$props_top, 'widget-state');
				self._$props_new = $$div(self._$props_top);
	
				/* close button */
				self._$close = $$$(self._$back, 'widget-close-button', -17, -17, null, null, function(){ self._exit_config(false); });
			}
			self.update_size();
		},
	
	
		_destroy: function()
		{
			this.element.empty();
		},
		
		_add_renderer:function()
		{
			var self = this;
			if (self.renderer == null)
			{
				var t = get_class_type_for_wi(self._data);
				// create new widget renderer
				var f = se.sparkwi.widget.viz[t];
				if (f == null)
					throw new Error("Can't create renderer se.sparkwi.widget.viz."+t);
				
				self.renderer = new f();
				jQuery.extend(self.renderer,
				{
					setValue: function(ctx)
					{
						self.renderer.def = self.definition();
						self.renderer.children = ctx.children;
						self.renderer.data = ctx.data;
						self.renderer.props = ctx.props;
						self.renderer.connection = ctx.proxy.connection;
						self.renderer.selector_values = ctx.proxy.selector_values;
						self.renderer.selector_display_values = ctx.proxy.selector_display_values;
					},
					setSize: function(width,height)
					{
						self.renderer.width	= width;
						self.renderer.height = height;
						if (self.renderer.render != null)
							if (DBCELL_DEBUG)
								self.renderer.render();
							else
								try { self.renderer.render(); } catch (e) { logging.log(e); }
					},
					viz_util: se.sparkwi.widget.viz.util,
					util: se.sparkwi.widget.util,
					save_props: function(p)
					{
						clearTimeout(self.stimeoutid);
						self.stimeoutid = setTimeout(function()
						{
							self._save_props(p,false);
							self.stimeoutid = -1;
						}, 2000);
					}
				});
				if (self.renderer.use_paper == null || self.renderer.use_paper)
				{
					self._$widget_renderer = Raphael(self.pid,self.options.width,self.options.height);
					self.renderer.paper = self._$widget_renderer;
					self.renderer._$p = self._$p;
				}
				else
					self._$p.remove();
						
				self.renderer.element = self._$c;
				if(self.renderer.init != null)
					try { self.renderer.init(); } catch (e) { logging.log(e); }
				
			}
			self._$c.empty();
			var special_case = self._data.props['display-style'] == 'map' || self._data.props['display-style'] == 'feed';
			if (special_case)
				self._$p.hide();
			else
				self._$p.show();
				
		},
	
		data:function(wi)
		{
			var self = this;
			if (wi==null)
				return self._data;
			if (wi.props==null)
				wi.props = {};
			if(!wi.proxy)
				wi.proxy = {widget:{id:-1}};
			if (wi.proxy.selector_values==null)
				wi.proxy.selector_values = {};
			if (wi.proxy.selector_display_values==null)
				wi.proxy.selector_display_values = {};
			self._data = wi;
			self._update_ui();
		},
	
		dashboard: function(d)
		{
			var self = this;
			if (d==null)
				return self._dashboard;
			self._dashboard = d;
		},
	
		definition: function()
		{
			var self = this;
			return self._data.proxy.widget;
		},
	
		key: function()
		{
			var self = this;
			return self._data.id;
		},
	
		state: function()
		{
			var self = this;
			return self._data.state;
		},
	
		proxy_state: function()
		{
			var self = this;
			return self._data.proxy.state;
		},
		
		returns: function(type)
		{
			var self = this;
			if (self.definition().is_correlation)
			{
				for (var i=0; i<self._data.children.length; i++)
					if (!self._returns(type,self._data.children[i].widget))
						return false;
				return true;
			}
			else
				return self._returns(type,self._data.proxy.widget)
		},
		
		_returns: function(type,widget)
		{
			var a = widget.return_types;
			var s = a.length;
			for (var i=0; i<s;i++)
				if (a[i]==type)
					return true;
			return false;
		},
		
		returns_text: function()
		{
			var self = this;
			return self.returns(RETURN_TYPE_TEXT);
		},
		returns_number: function()
		{
			var self = this;
			return self.returns(RETURN_TYPE_NUMBER);
		},
		returns_annotated_number: function()
		{
			var self = this;
			return self.returns(RETURN_TYPE_ANNOTATED_NUMBER);
		},
		returns_geodata: function()
		{
			var self = this;
			return self.returns(RETURN_TYPE_GEODATA);
		},
		returns_dict: function()
		{
			var self = this;
			return self.returns(RETURN_TYPE_DICT);
		},
	
		_update_ui: function()
		{
			var self = this;
			var conn_class = self._data.proxy.widget.required_connection_type.toLowerCase();
			if (conn_class=='none')
				conn_class = get_class_name_for_widget(self._data.proxy.widget);
			self.element.addClass(conn_class);
			var c = get_css_class_for_proxy(self._data.proxy);
			self.element.addClass(c);
			if (self.options.is_editable && self.propsform == null)
			{
				var t = get_class_type_for_wi(self._data);
				var f = se.sparkwi.widget.config[t];
				if (f==null)
					throw new Error("Can't create config for se.sparkwi.widget.config."+t);
			
				self.propsform = new f();
				jQuery.extend(self.propsform, 
					se.sparkwi.widget.config.mixins, 
					se.sparkwi.widget.config.help, 
					{ cell: self });
			}
			
			var ok = self.state() == WIDGET_STATE_OK;
			var needs_props = false;
			if (self.propsform != null)
			{
				if (self.propsform.requires_props)
					needs_props = self.propsform.requires_props();
				else 
					needs_props = self.propsform._requires_props();
			}
			if (self.in_config || !ok || needs_props)
				this._show_config();
			else
				this._show_viz();

			//back/error
			var state = self.state();
			var proxy_state = self.proxy_state();
	
			var title = self.definition().name;
			if (state != WIDGET_STATE_OK)
				title = se.sparkwi.widget.util.widget_state_to_str(state,self._data.proxy.widget);
			if (self._$state!=null)
				self._$state.text(title);
			if (self._$plus!=null)
				self._$plus.html('<div class="label">Drop to compare '+title+'</div>');
			
			if (proxy_state == PROXY_STATE_FAILED_DATA_ERROR)
			{
				// Removing newly created elements for error display
				$(this.element).find('.widget-front-error-overlay').remove();
				$('.widget-edit-button',self._$front).removeClass('widget-front-error');
	
				// Adding error elements
				self._$front.append('<div class="widget-front-error-overlay">&nbsp;</div>');
				self._$front.append('<div class="widget-front-error-overlay">&nbsp;</div>');
				$('.widget-edit-button',self._$front).addClass('widget-front-error');
			}
			else
			{
				$(this.element).find('.widget-front-error-overlay').remove();
				$(this.element).find('.widget-props-panel-error').remove();
				$('.widget-edit-button',self._$front).removeClass('widget-front-error');
			}
			if (self._$action_text!=null && self._data.props != null && self._data.props['action-text']!=null && self._data.props['action-text']!='')
				self._$action_text.empty().append('<a href="'+self._data.props['action-link']+'" target="_blank">'+self._data.props['action-text']+'</a>');
	
		},
	
		updatePropsHeight: function(v)
		{
			var self = this;
			if (self._$props_new!=null)
			{
				var h = self._$props_new.height() + 105;
				h = Math.max(230, h);
					self._$back.css({'height':h+'px'});
			}
		},
	
		size: function(t)
		{
			var self = this;
			if (t==null)
				return self._size;
			self.options.width = t[0];
			self.options.height = t[1];
			self.update_size();
		},
	
		update_size: function()
		{
			var self = this;
			var innerw = self.options.width - 20;
			var innerh = self.options.height - 62;
			self._$c.css({'width':innerw+'px','height':innerh+'px'});
			self._$p.css({'width':innerw+'px','height':innerh+'px'});
			if (self._$widget_renderer!=null)
				self._$widget_renderer.setSize(innerw,innerh);
			if (self.renderer!=null)
				self.renderer.setSize(innerw,innerh);
			if (self._$back!=null)
				self._$back.css({'width':self.options.width+'px'});
			self._$no_data.css({'top':((innerh-100)*.5+30)+'px','left':((innerw-174)*.5)+'px'});
		},
		
		do_enter_config:function()
		{
			var self = this;
			$$tooltip_hide();
			self._show_config();
		},
		
		_exit_config:function(save)
		{
			var self = this;
			if (self.propsform!=null)
				self.propsform.close_help();
			$$tooltip_hide();
			self.in_config = false;
			if (save)
			{
				self._$back.hide();
				self._$front.show();
				--g_current_config_zindex;
				$(this.element).css("zIndex",10); 
				self._save_props(null,true);
				self._loading(true);
			}
			else
			{
				$(self.element).find('.ui-resizable-handle').show();
				self.data(self._data);
			}
		},
		
		_save_props: function(xtra,set_data)
		{
			var self = this;
			if (self.propsform!=null)
				self.propsform.close_help();
			var o = {};
			//existing props
			jQuery.extend(o,self._data.props);
			//gather editor values
			var qm = [];
			var $upload = null;//only handles 1 for now
			var $inputs = self._$props_top.find('.widget-prop-editor');
			$inputs.each(function(idx,el)
			{
				var $el = $(el);//<org.jquery.jQuery
				var name = $el.attr('name');
				var type = $el.data('type');
				if (type=='file')
					$upload = $el;
				else if ($el.data('val')!=null)
					qm.push({name:name, value: $el.data('val')() });
				else
					qm.push({name:$el.attr('name'), value: $el.val() });
			});
			for (var i=0; i<qm.length; i++)
				o[qm[i].name] = qm[i].value;
			//extra props (probably coming from the front of the widget)
			if (xtra)
				jQuery.extend(o,xtra);
			var update_props = function()
			{
				if (set_data)
					self._loading(true);
				do_module('Dashboard/UpdatePropsForWidget',[self.key(),o], function(wi)
				{
					if (set_data)
						self._loading(false);
					$(self.element).find('.ui-resizable-handle').show();
					if (set_data)
						self.data(wi);
				});
			};
			if ($upload == null)
			{
				update_props();
			} 
			else
			{
				var ued = $upload.data('editor');
				if (!ued.hasFile())
				{
					alert("You must pick a file!");//TODO use sparkwise dialog
					return;
				}
				ued.upload(function()
				{
					o[ued.getName()] = ued.getValue();
					update_props();
				});
			}
		},
		
		_show_config: function()
		{
			//build config form
			var self = this;
			if (self._$back==null)
				return;
			self.in_config = true;
			self._$front.hide();
			$(this.element).find('.ui-resizable-handle').hide();
			$(this.element).css("zIndex",++g_current_config_zindex);
			self._$back.show();
			self._$back.scrollTop(0);
		
			self._$props_new.empty();
			self.propsform.render(self._$props_new);
			self.updatePropsHeight();
			
			if (self.active_element != null)
			{
				var name = self.active_element.attr("name");
				self._$props_new.find("[name='"+name+"']").focus();
				self.active_element = null;
			}
		},
		
		_show_viz: function()
		{
			var self = this;
			self._add_renderer();

			var ds = self._data.props['display-style'];
			var is_trend = ds == 'trend';
			var is_feed = ds == 'feed';

			var on_get_data = function(data)
			{
				if (data.error != null)
				{
					self._data.data = data;
					self._data.proxy.state = PROXY_STATE_FAILED_DATA_ERROR;
					self._data.proxy.last_failure_message = data.error;
					self._show_config();
					return;
				}
				self._data.data = data;
				if (self.renderer.paper != null)
					self.renderer.paper.clear();
				self.renderer.setValue(self._data);
				self.size([self.options.width,self.options.height]);
				
				var l = 0;
				if (data.values!=null)
					l = data.values.length;
				if (data.corr!=null)
					for (var i=0; i<data.corr.length; i++)
						if (data.corr[i]!=null)
								l += data.corr[i].values.length;
				if ((l<=1 && is_trend) || (is_feed && l!=0 && data.values[0].data.value==0))
				{
					self._$no_data.show();
				}
				if (self.options.viz_render_callback!=null)	
					self.options.viz_render_callback();
			};
			
			//
			var ts = self._data.props['time-span'];
			if (isNaN(ts))
				ts = 1;
			else
				ts = parseInt(ts);

			var date_from = null;
			var date_to = null;
			if (is_trend)
			{
				if (ts == 0)
				{
					date_to = self._data.props['time-span-to'];
					date_from = self._data.props['time-span-from'];
				}
			}
											
			if (date_from == null)
				do_module('Dashboard/GetDataForWidget',[self.key(),ts], on_get_data);
			else
				do_module('Dashboard/GetDataForWidget',[self.key(),date_from,date_to], on_get_data);
	
			if (self._$back!=null)
				self._$back.hide();
			self._$front.show();
		},
		
		in_edit_mode: function()
		{
			var self = this;
			return self.in_config;//_$back.is(':visible');
		},
		

		_update_connection: function(conn_key)
		{
			var self = this;
			if (self.propsform!=null)
				self.propsform.close_help();
			var args = [self._data.id, parseInt(conn_key)];
			do_module('Dashboard/SetConnectionForWidget', args, function(wi) {
				self.data(wi);
			});
		},

		_update_selector_new: function(name,value,display_value)
		{
			var self = this;
			if (self.propsform!=null)
				self.propsform.close_help();
			self._loading(true);
			do_module('Dashboard/SetSelectorValueForWidget',[self._data.id,name,value,display_value], function(wi)
			{
				self.active_element = $(document.activeElement);
				self._loading(false);
				self.data(wi);
			});
		},
		
		_refresh_widget_data: function(e)
		{
			var self = this;
			self._loading(true);
			do_module('Dashboard/RefreshWidgetData',[self._data.id], function(wi)
			{
				self._loading(false);
				self.data(wi);
			});
		},
		
		_loading: function(b)
		{
			var self = this;
			if (!b)
			{
				self._$close.show();
				self._$c.empty();
			}
			else
			{
				self._$no_data.hide();
				self._$c.empty();
				self._$c.append("<div class='widget-grid-cell-loading-small' ></div>");
				self._$p.hide();
				self._$close.hide();
				if (self._$done!=null)
					self._$done.hide();
				self._$state.text("Loading...");
			}
		},
		
		over_correlation: function(b)
		{
			var self = this;
			if (self._$corr_slugs==null || self._$corr_members==null)
				return;
			var i = self._$corr_members.length-1;
			if (i<0) i = 0;
			var $c = self._$corr_slugs[i];
			$c.toggleClass('over',b);
		},
		
		_embed_widget: function()
		{
			var self = this;
			var embed = '<iframe src="'+g_host+'/w/'+self._data.uuid+'"'+
				' scrolling="no" marginwidth="0" marginheight="0"'+
				' allowtransparency="true" frameborder="0" '+
				' style="width:'+(self.options.width+7)+'px;height:'+(self.options.height+7)+'px;"></iframe>';
		
			var $xtra = $('<br/><br/><textarea id="widget-embed-code" rows="4" cols="40">'+embed+'</textarea>');
			$$dialog('Embed Widget', 'You can embed this widget on your own website. Just copy and paste the embed code '+
					'into the html on a page of your site and you&rsquo;re set.', $xtra, [{name:'OK'}])

		},
		
		attr: function(o) { this.element.attr(o); },
		css: function(o) { this.element.css(o); },
		draggable: function(o) { this.element.draggable(o);	} ,
		resizable: function(o) { this.element.resizable(o); },
		hasClass: function(o) { this.element.hasClass(o); },
		animate: function(o,p) { this.element.animate(o,p); },
		remove: function(o,p) { this.element.remove(o,p); }
	};

	jQuery.extend(this,def);
	this.element = el;
	this.element.data('def',this);
	this.init();
};
