$.extend(se.sparkwi.widget.config,
{ 
	/**
	 *  widgets without implementation use the 'standard' config and display props.
	 *  widgets that need special display properties can overload render_display_props, requires_props.
	 *  widgets that are totally custom can overload render.
	 *  widgets may define help per field- google spreadsheets for an example.
	 */
	
	facebook_getfriends: function()
	{
	},

	facebook_getmyposts: function()
	{
	},

	facebook_getcomments: function()
	{
	},

	facebook_getfanposts: function()
	{
	},
	
	facebook_getpageviews: function()
	{
	},

	facebook_getpagelikes: function()
	{
	},

	facebook_getalbumlikes: function()
	{
	},

	facebook_getvideolikes: function()
	{
	},

	facebook_getusergenders: function()
	{
	},
	
	facebook_getuseragegroup: function()
	{
	},

	twitter_gettweets: function()
	{
	},

	twitter_getfollowers: function()
	{
	},

	twitter_getmentions: function()
	{
	},

	twitter_getretweets: function()
	{
	},
	
	twitter_getmentionssearch: function()
	{
	},
	
	twitter_getrepliessearch: function()
	{
	},
	
	twitter_getretweetssearch: function()
	{
	},
	
	twitter_gettweetssearch: function()
	{
	},
	
	twitter_getfollowerssearch: function()
	{
	},
	
	twitter_gethashtagsearch: function()
	{
	},

	twitter_getlinksearch: function()
	{
	},
	
	googleanalytics_help:
	{
		help_title: 'Configuring Google Analytics',
		'page': 
		[
		 'You can track a specific url under your domain.',
		 'To track analytics on a particular page, copy and paste the URL here.',
		 'Use a relative URL. This is the part of the URL after your domain, starting with the slash.',
		 'For example, if you want to track http://site.org/about, enter /about here.'
		]
	},
	
	googleanalytics_getpageviews: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getsitevisits: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getpagespervisit: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getbouncerate: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getsitereferrers: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getabsoluteuniquevisitors: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getpercentnewvisits: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getbrowsers: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_gettrafficsources: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getconnectionspeed: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},

	googleanalytics_getavgtimeonsite: function()
	{
		var self = this;
		self.help = se.sparkwi.widget.config.googleanalytics_help;
	},


	
	picasa_getslideshow: function()
	{
		var self = this;
		self.render_display_props = function($p)
		{
			self.title($p);
			var $speedi = $$div($p,'item');
			var $d = $$select($speedi,null,null, 'speed');
			$d.add_options([['4', 'Time Between Images'],
					        ['3','3 Seconds'],
					        ['4','4 Seconds'],
					        ['5','5 Seconds'],
					        ['6','6 Seconds'],
					        ['7','7 Seconds'],
					        ['8','8 Seconds'],
					        ['9','9 Seconds'],
					        ['10','10 Seconds']], 
					        self.cell._data.props['speed']);
			var $scalei = $$div($p,'item');
			var $s = $$select($scalei,null,null, 'scale');
			$s.add_options([['fit', 'Maintain Image Ratios'],
					        ['full_bleed','Fit to Window']], 
					        self.cell._data.props['scale']);
		}
	},
	
	flickr_getslideshow: function()
	{
		var self = this;
		self.render_display_props = function($p)
		{
			self.title($p);
			var $speedi = $$div($p,'item');
			var $d = $$select($speedi,null,null, 'speed');
			$d.add_options([['4', 'Time Between Images'],
					        ['3','3 Seconds'],
					        ['4','4 Seconds'],
					        ['5','5 Seconds'],
					        ['6','6 Seconds'],
					        ['7','7 Seconds'],
					        ['8','8 Seconds'],
					        ['9','9 Seconds'],
					        ['10','10 Seconds']], 
					        self.cell._data.props['speed']);
			var $scalei = $$div($p,'item');
			var $s = $$select($scalei,null,null, 'scale');
			$s.add_options([['fit', 'Maintain Image Ratios'],
					        ['full_bleed','Fit to Window']], 
					        self.cell._data.props['scale']);
		}
	},

	youtube_getviews: function()
	{
	},

	youtube_getchannelviews: function()
	{
	},

	youtube_getchannelsubscribers: function()
	{
	},

	vimeo_getviews: function()
	{
	},

	vimeo_getchannelviews: function()
	{
	},

	googlespreadsheets_gettrend: function()
	{
		var self = this;
		self.render_display_props = function($p)
		{
			self.title($p);

			var $uniti = $$div($p,'item');
			var $unit = $$input2($uniti, 'Unit', 'unit');
			$unit.setValue(self.cell._data.props['unit']);
			
			self.standard($p);
		}

		self.help = 
		{
			help_title: 'Google Spreadsheets Trend',
			'spreadsheet': 
			[
				'Create a spreadsheet at <a href="http://docs.google.com" target="_blank">docs.google.com</a> with the following attributes.',
				'The first row must be column headers. <rollover googlespreadsheets1.png 350 114>',
				'There cannot be blank rows (this indicates the end of the data).',
				'The date column values must conform to a standard date format (dd-MM-yyyy, yyyy-MM-dd, MM/dd/yyyy, ... ). <rollover googlespreadsheets2.png 120 148>',
				'The number column should look like: <rollover googlespreadsheets3.png 95 148>',
				'The label column will be used for annotating each point in the trend.',
				'There can be other columns in the spreadsheet, but Sparkwise will only use the data you indicate.'
			],
			'date column':
			[
			 	'The date, number and label column are used to display the trend. Each dated row will display a dot in a graph over time.',
				'The date column values must conform to a standard date format (dd-MM-yyyy, yyyy-MM-dd, MM/dd/yyyy, ... ). <rollover googlespreadsheets2.png 120 148>',
				'The number column should look like: <rollover googlespreadsheets3.png 95 148>'
			]
		};
	},
	
	googlespreadsheets_getdict: function()
	{
		var self = this;
		self.render_display_props = function($p)
		{
			self.title($p);
			self.standard($p);
		}
		self.help = 
		{
			help_title: 'Google Spreadsheets Pie',
			'spreadsheet': 
			[
				'Create a spreadsheet at <a href="http://docs.google.com" target="_blank">docs.google.com</a> with the following attributes.',
				'The first row must be column headers. <rollover googlespreadsheets1.png 499 48>',
				'There cannot be blank rows (this indicates the end of the data).',
				'The label column can have any text.',
				'The number column should look like: <rollover googlespreadsheets3.png 95 148>',
				'There can be other columns in the spreadsheet, but Sparkwise will only use the data you indicate.'
			],
			'label column':
			[
				'The label column can have any text.',
				'The number column should look like: <rollover googlespreadsheets3.png 95 148>',
				'There can be other columns in the spreadsheet, but Sparkwise will only use the data you indicate.'
			]
		};
	},
	
	googlealerts_getfeed: function()
	{
		var self = this;
		self.help = 
		{
			help_title: 'Configuring Google Alerts',
			'URL feed': 
			[
				'Open <a href="http://www.google.com/alerts" target="_blank">http://www.google.com/alerts</a>',
				'Fill out the form.',
				'Select \"Feed\" from the Deliver To: menu. <rollover googlealerts00.png 300 217>',
				'Click \"Create Alert\" Button. <rollover googlealerts1.png 159 35>',
				'Right click the orange RSS icon. <rollover googlealerts2.png 271 127>',
				'Copy the link address',
				'Paste the address in the Sparkwise Google alert widget \"Unique Alert URL\" field.',
				'Please note that it may take <b>10 minutes</b> for the feed to start gathering information. Come back after the feed has been set up.'
			]
		};
	},
	
	bing_getnews: function()
	{
	},
	
	bing_searchweb: function()
	{
	},

	story_text: function()
	{
		var self = this;
		self.requires_props = function()
		{
			var txt = self.cell._data.props['text'];
			return txt == null || txt == '';
		}
		self.render_display_props = function($p)
		{
			self.title($p);
			var $texti = $$div($p,'item');
			var $text = $$rte($texti,'text',5,20);
			var tv = self.cell._data.props['text'];
			$text.setValue(tv);
			if (tv==null)
			{
				$text.setValue('Describe your project, state your mission, share a quote, or tell a story. '+
					'Just type directly into this text field.');
			}
		}
	},

	story_embed: function()
	{
		var self = this;
		self.requires_props = function()
		{
			var ec = self.cell._data.props['embed'];
			return ec == null || ec == '';
		}
		self.render_display_props = function($p)
		{
			self.title($p);
			var $texti = $$div($p,'item');
			var $text = $$textarea($texti, 'embed', 5, 20);
			$text.setMessage('Paste the embed code for your video here.');
			$text.setValue(self.cell._data.props['embed']);
		}
	},
	
	story_embed_soundcloud: function()
	{
		var self = this;
		self.requires_props = function()
		{
			var ec = self.cell._data.props['embed'];
			return ec == null || ec == '';
		}
		self.render_display_props = function($p)
		{
			self.title($p);
			var $texti = $$div($p,'item');
			var $text = $$textarea($texti, 'embed', 5, 20);
			$text.setMessage('Paste the embed code here.');
			$text.setValue(self.cell._data.props['embed']);
			$text.css({'width':'-=35px;'});
			//help
			var $icon = $("<img src='/static/image/help-rollover.png' class='icon-help' align='top' />");
			$icon.css({'margin-left':'5px'});
			$texti.append($icon);
			$icon.click(function(){ self.show_dialog('SoundCloud Embed', 
				['Widget currently takes embed code from <a href="http://soundcloud.com" target="_blank">SoundCloud</a>.'], $icon); });
		}
	},
	
	story_embed_issu: function()
	{
		var self = this;
		self.requires_props = function()
		{
			var ec = self.cell._data.props['embed'];
			return ec == null || ec == '';
		}
		self.render_display_props = function($p)
		{
			self.title($p);
			var $texti = $$div($p,'item');
			var $text = $$textarea($texti, 'embed', 5, 20);
			$text.setMessage('Paste the embed code here.');
			$text.setValue(self.cell._data.props['embed']);
		}
	},
	
	story_upload: function()
	{
		var self = this;
		self.requires_props = function()
		{
			var ec = self.cell._data.props['logo'];
			return ec == null || ec == '';
		}
		self.render_display_props = function($p)
		{
			self.title($p);
			var $uploadi = $$div($p,'item');
			var $up = $$upload($uploadi, 'logo');
			$up.setValue(self.cell._data.props['logo']);
			var $scalei = $$div($p,'item');
			var $s = $$select($scalei,null,null, 'scale');
			$s.add_options([['fit', 'Maintain Image Ratios'],
					        ['full_bleed','Fit to Window']], 
					        self.cell._data.props['scale']);
		}
	},

	custom_manual: function()
	{
		var self = this;
		self.render_display_props = function($p)
		{
			self.title($p);
			
//			var $displaytypei = $$div($p,'item');
//			var $dt = $$select($displaytypei,null,null, 'display-type');
//			$dt.add_options([['per_day', 'Total Per Day'],
//					        ['absolute_total','Absolute Total']], 
//					        self.cell._data.props['display-type']);

			var $uniti = $$div($p,'item');
			var $unit = $$input2($uniti, 'Unit', 'unit');
			$unit.setValue(self.cell._data.props['unit']);
			
			self.standard($p);
		}
	},

	custom_json: function()
	{
		var self = this;
		self.render_display_props = function($p)
		{
			self.title($p);
			
//			var $displaytypei = $$div($p,'item');
//			var $dt = $$select($displaytypei,null,null, 'display-type');
//			$dt.add_options([['per_day', 'Total Per Day'],
//					        ['absolute_total','Absolute Total']], 
//					        self.cell._data.props['display-type']);
			
			var $uniti = $$div($p,'item');
			var $unit = $$input2($uniti, 'Unit', 'unit');
			$unit.setValue(self.cell._data.props['unit']);
			
			self.standard($p);
		};
		self.help = 
		{
			help_title: 'Numeric JSON Feed',
			'JSON URL feed': 
			[
				'Add the URL of the JSON feed here.',
				'Example: http://query.yahooapis.com/v1/public/yql?q=select%20item%20from%20weather.forecast%20where%20location%3D%2248907%22&format=json'
			],
			'JSON URL path': 
			[
				'Use standard JSON notation to access a number in the feed.',
				'Example: query.results.channel.item.condition.temp'
			]
		};

	},
	
	custom_xml: function()
	{
		var self = this;
		self.render_display_props = function($p)
		{
			self.title($p);
			
//			var $displaytypei = $$div($p,'item');
//			var $dt = $$select($displaytypei,null,null, 'display-type');
//			$dt.add_options([['per_day', 'Total Per Day'],
//					        ['absolute_total','Absolute Total']], 
//					        self.cell._data.props['display-type']);
			
			var $uniti = $$div($p,'item');
			var $unit = $$input2($uniti, 'Unit', 'unit');
			$unit.setValue(self.cell._data.props['unit']);
			
			self.standard($p);
		};
		self.help = 
		{
			help_title: 'Numeric XML Feed',
			'XML URL feed': 
			[
				'Add the URL of the XML feed here.',
				'Example: http://weather.gov/xml/current_obs/KSFO.xml'
			],
			'Xpath': 
			[
				'Use standard Xpath notation to access a number in the feed.',
				'Example: //temp_f/text()',
				'For more information about XPath, see <a href="http://msdn.microsoft.com/en-us/library/aa926473.aspx">XPath Syntax</a>.'	
			]
		};
	},

	sparkwise_correlation: function()
	{
		var self = this;
		self.colors = [se.sparkwi.colors.Search, 
		               se.sparkwi.colors.Story, 
		               se.sparkwi.colors.Video, 
		               se.sparkwi.colors['Google Analytics'], 
		               se.sparkwi.colors.Twitter, 
		               se.sparkwi.colors.Facebook, 
		               se.sparkwi.colors.Custom, 
		               se.sparkwi.colors.Compare];
		for (var i=0; i<self.colors.length; i++)
			self.colors[i] = ColorUtil.toHexString(self.colors[i]);
		self.color_class = ['search', 'story', 'video', 'google_analytics', 'twitter', 'facebook', 'custom', 'compare'];
		self.render_display_props = function($p)
		{
			self.title($p);

			var $$correlation_member = function (i, proxy)
			{
				var color_prop_name = 'item_color_'+proxy.id;
				var color = self.cell._data.props[color_prop_name];
				if (color == null)
					color = get_color_for_proxy(proxy);
				self.cell._data.props[color_prop_name] = color;
				var css_class = self.color_class[jQuery.inArray(color, self.colors)];
				//
				var $c = $("<div class='widget-correlation-cell Correlate'></div>");
				var $t = $("<div class='top "+css_class+"'></div>");
				$c.append($t);
				var descr = com_pagesociety_util_StringUtil.shorten(get_description_for_proxy(proxy),55);

				$c.append("<div class='label'>"+descr+"</div>");
				var cp = $$colorpicker($c, 
							color_prop_name, 
							self.colors, 
							color,
							function()
							{ 
								$t.removeClass(css_class);
								css_class = self.color_class[jQuery.inArray(cp.getValue(), self.colors)];
								$t.addClass(css_class);
								//$t.css('background-color', cp.getValue()); 
								self.cell._data.props[color_prop_name] = cp.getValue();
							});
				return $c;
			}
				
			var $$correlation_slug = function ()
			{
				var $c = $("<div></div>");
				$c.addClass('widget-correlation-slug');
				return $c;
			}
		
			self._$corr_members = [];
			self._$corr_slugs = [];
			var l = $$div($p);
			l.css({'width':'245px','height':'225px','position':'relative'});
		
			var slug_layer = $$div(l);
			slug_layer.css({'position':'absolute','zIndex':'1'});
			for(var i = 0;i < 4;i++)
			{
				 var s = $$correlation_slug();
				 s.css({'float':'left','overflow':'hidden'});
				 slug_layer.append(s);
				 self._$corr_slugs[i] = s;
			}
		
			var member_layer = $$div(l)
			member_layer.css({'position':'absolute','zIndex':'2'});
			var cc = self.cell.data().children;
			//console.log(cc);
			for(var i = 0;i < cc.length;i++)
			{
				 var m = $$correlation_member(i, cc[i]);
				 m.css('float','left');
				 member_layer.append(m);
				 m.data({corr_cell:self.element,
					 	 corr_key: self.cell._data.id,
					 	 wi_key: cc[i].id});
		
				m.draggable({
			 	 	opacity: 			 0.9,
			 	 	helper: 			"clone",
			 	 	scrollSensitivity: 	50,
			 	 	appendTo: 			'body' ,
					drag: 				function(event, ui) {
						self.cell._dashboard._process_drag_widget_corr_member(event, ui);
					},
					zIndex: 			9999/*,
					containment:		'#grid'*/
				});
				m.css({cursor:'pointer'});
				self._$corr_members[i] = m;
			}
			slug_layer.append($$clear_both);
			member_layer.append($$clear_both);
			
			var ds = self.cell._data.props['display-style'];
			var $dsi = $$div($p,'item');
			var $ds = $$select($dsi,null,null,'display-style');
			if (self.cell.returns_number())
				$ds.add_options([['trend','Trend']], ds);
			if (self.cell.returns_geodata())
				$ds.add_options([['map', 'Map']],ds);
			if (self.cell.returns_dict())
				$ds.add_options([['pie', 'Pie']],ds);
	
			var $$trend = self.trend($p);
			var $show_events = self.events($p);
					
			function hide_all()
			{
				$$trend.hide();
				$show_events.hide();
			}
			function show_one()
			{
				var v = $ds.val();
				hide_all();
				switch(v)
				{
					case 'trend':
						$$trend.show();
						$show_events.show();
						break;
					case 'map':
					case 'pie':
						break;
				}
				self.cell.updatePropsHeight();
			}
	
			hide_all();
			show_one();
			
			$ds.change(function(e){ show_one(); });
			$$trend.change(function(e){ show_one(); });
			


		}
	}
});