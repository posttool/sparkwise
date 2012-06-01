
$.extend(se.sparkwi.widget.viz,
{ 
	
	
/** FACEBOOK */
	


	facebook_getfriends: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Facebook Friends for "+self.connection.username;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	facebook_getmyposts: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Facebook Posts by "+self.selector_display_values.page;
			self.viz_util.render(self);
		};
	},

	facebook_getcomments: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Facebook Comments for "+self.selector_display_values.page;
			self.viz_util.render(self);
		};
	},

	facebook_getfanposts: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Facebook Fan Posts for "+self.selector_display_values.page;
			self.viz_util.render(self);
		};
	},
	facebook_getpageviews: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Facebook Page Views for "+self.selector_display_values['fan page'];
			self.viz_util.render(self);
		};
	},

	facebook_getpagelikes: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Facebook Page Fans for "+self.selector_display_values['fan page'];
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	facebook_getalbumlikes: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Facebook Album Likes for "+self.selector_display_values.album;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	facebook_getvideolikes: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Facebook Video Likes for "+self.selector_display_values.video;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	facebook_getusergenders: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Facebook Fan Genders for "+self.selector_display_values['fan page'];
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.pie_display_percent_of_total = true;
			self.pie_display_key_map = { 'M':'Male',
			                             'F':'Female',
			                             'U':'Unknown' };
			// self.always_show_legend = true;
			if (self.props['display-style']==null)
				self.props['display-style'] = 'pie';
			self.viz_util.render(self);
		};
	},
	facebook_getuseragegroup: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Facebook Fan Age Groups for "+self.selector_display_values['fan page'];
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.pie_display_percent_of_total = true;
			// self.always_show_legend = true;
			self.props['display-style'] = 'pie';
			self.viz_util.render(self);
		};
	},



/** SLIDESHOW */
	


	picasa_getslideshow: function()
	{
		var self = this;
		self.use_paper = false;
		self.render = function()
		{
			self.title = self.props.title;
			var paths = [];
			for (var i=0; i<self.data.values.length; i++)
				paths.push(self.data.values[i].data.value);
			self.viz_util.slideshow(self,paths);
		};
	},
	
	flickr_getslideshow: function()
	{
		var self = this;
		self.use_paper = false;
		self.render = function()
		{
			self.title = self.props.title ? self.props.title  : self.selector_display_values.album;
			var paths = [];
			for (var i=0; i<self.data.values.length; i++)
				paths.push(self.data.values[i].data.value);
			self.viz_util.slideshow(self,paths);
		};
	},

		
	
/** TWITTER */
	


	twitter_gettweets: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Tweets by "+self.connection.username;
			self.viz_util.render(self);
		};

	},
	
	twitter_gettweetssearch: function()
	{
		var self = this;
		self.render = function()
		{
			if (self.props['display-style']==null)
				self.props['display-style'] = 'feed';
	
			self.title = "Tweets by "+self.selector_values.username;
			self.viz_util.render(self);
		};
	},	
	
	twitter_getfollowers: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Twitter Followers for "+self.connection.username;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		}
	},
	
	twitter_getfollowerssearch: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Twitter Followers for "+self.selector_values.username;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	twitter_getmentions: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Twitter Mentions of "+self.connection.username;
			self.viz_util.render(self);
		}

	},
	
	twitter_getmentionssearch: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Mentions of "+self.selector_values.username;
			self.viz_util.render(self);
		};
	},
	
	twitter_getretweets: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Retweets of "+self.connection.username;
			self.viz_util.render(self);
		};

	},
	
	twitter_getretweetssearch: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Retweets of "+self.selector_values.username;
			self.viz_util.render(self);
		};
	},
	
	twitter_getrepliessearch: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Replies to "+self.selector_values.username;
			self.viz_util.render(self);
		};
	},
	
	
	twitter_gethashtagsearch: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Hashtag occurances of "+self.selector_values.hashtag;
			self.viz_util.render(self);
		}
	},

	twitter_getlinksearch: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "URL Links of "+self.selector_values.link;
			self.viz_util.render(self);
		}
	},
	
		
	
	
	
/** GOOGLE ANALYTICS*/
	



	googleanalytics_getpageviews: function()
	{
		var self = this;

		self.render = function()
		{
			var account =	self.util.google_fluff_account(self);
			self.title = "Page Views for "+account;
			self.viz_util.render(self);
		}

	},

	googleanalytics_getsitevisits: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Site Visits for "+ self.util.google_fluff_account(self);
			self.viz_util.render(self);
		}

	},

	googleanalytics_getpagespervisit: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Pages per Visit for "+ self.util.google_fluff_account(self);
			self.viz_util.render(self);
		}

	},

	googleanalytics_getbouncerate: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Bounce Rate for "+ self.util.google_fluff_account(self);
			self.display_type = se.sparkwi.DISPLAY_PER_DAY_PERCENT;
			self.viz_util.render(self);
		}

	},

	googleanalytics_getsitereferrers: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Top Site Referrers for "+ self.util.google_fluff_account(self);
		// self.always_show_legend = true;
			self.props['display-style'] = 'pie';
			self.viz_util.render(self);
		}
	},

	googleanalytics_getabsoluteuniquevisitors: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Absolute Unique Visitors for "+ self.util.google_fluff_account(self);
			self.viz_util.render(self);
		}
	},

	googleanalytics_getpercentnewvisits: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Percent New Visits for "+ self.util.google_fluff_account(self);
			self.display_type = se.sparkwi.DISPLAY_PER_DAY_PERCENT;
			self.viz_util.render(self);
		}
	},

	googleanalytics_getbrowsers: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Browsers for "+ self.util.google_fluff_account(self);
		//	self.always_show_legend = true;
			self.props['display-style'] = 'pie';
			self.viz_util.render(self);
		}

	},

	googleanalytics_gettrafficsources: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Traffic Sources for "+ self.util.google_fluff_account(self);
			//self.always_show_legend = true;
			self.props['display-style'] = 'pie';
			self.viz_util.render(self);
		}
	},

	googleanalytics_getconnectionspeed: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Connection Speed for "+ self.util.google_fluff_account(self);
			self.props['display-style'] = 'pie';
			self.viz_util.render(self);
		}
	},

	googleanalytics_getavgtimeonsite: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Average Time on Site for "+ self.util.google_fluff_account(self);
			self.unit = "seconds";
			self.viz_util.render(self);
		}
	},
	
	googlespreadsheets_gettrend: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = self.selector_display_values["spreadsheet"];
			self.viz_util.render(self);
		};
	},
	
	googlespreadsheets_getdict: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = self.selector_display_values["spreadsheet"];
			self.viz_util.render(self);
		};
	},
	
	googlealerts_getfeed: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = self.data.values[0].data.title;
			self.viz_util.render(self);
		};
	},
	
	bing_getnews: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "News search for "+self.selector_display_values["Search Query"];
			self.viz_util.render(self);
		};
	},
	
	bing_searchweb: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = "Web search for "+self.selector_display_values["Search Query"];
			self.viz_util.render(self);
		};
	},

	youtube_getviews: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Video Views for "+self.selector_display_values.video;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	youtube_getchannelviews: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Channel Views for "+self.connection.username;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	youtube_getchannelsubscribers: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Channel Subscribers for "+self.connection.username;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	vimeo_getviews: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Video Views for "+self.selector_display_values.video;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},

	vimeo_getchannelviews: function()
	{
		var self = this;

		self.render = function()
		{
			self.title = "Channel Views For: "+self.selector_display_values.channel;
			self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.viz_util.render(self);
		};
	},


	/* custom0 */
	story_text: function()
	{
		var self = this;
		self.use_paper = false;
		self.render = function()
		{
			var html	= $('<div>'+self.props.text+"</div>");
			html.find('a').attr('target','_blank');
			self.element.empty();
			self.viz_util.title_and_icon(self);
			self.viz_util.wrap(self);
			self.$wrap.addClass('custom-text');
			self.$wrap.append(html);
			self.$wrap.jScrollPane();
		};
	},
	
	story_upload: function()
	{
		var self = this;
		self.use_paper = false;
		self.render = function()
		{
			self.element.empty();
			self.viz_util.title_and_icon(self);
			
			// the image
			var p = self.props.logo.split(';');
			var id = p[0].substring(3);
			var path = p[1].substring(5);
			var scale = self.props.scale ? self.props.scale : 'fit';
			var to = self.$title.height()+4;
			var $img = $$image(self.element, s3basepath+path, { width: self.width, height: self.height-to, scale: scale });
			$img.css('margin-top',to+'px');
		}

	},

	story_embed: function()
	{
		var self = this;
		self.use_paper = false;
		self.render = function()
		{
			self.element.empty();
			self.viz_util.title_and_icon(self);
			self.viz_util.wrap(self);
			var e = self.props.embed;
			if (e!=null)
			{
				e = e.replace(/width=['|"]\d*%?p?x?['|"]/g, 'width="100%"');
				e = e.replace(/height=['|"]\d*%?p?x?['|"]/g, 'height="100%"');
				if(e.indexOf("www.youtube.com")!=-1)
					e = e.replace(/src=[\'"]?([^\'" >]+)/, "src='$1?wmode=transparent'");/* get it to pay attention to zindex */
				self.$wrap.append(e);
			}
		};
	},

	story_embed_soundcloud: function()
	{
		var self = this;
		self.use_paper = false;
		self.render = function()
		{
			self.element.empty();
			self.viz_util.title_and_icon(self);
			self.viz_util.wrap(self);
			var e = self.props.embed;
			if (e!=null)
				self.$wrap.append(e);
		};
	},	

	story_embed_issu: function()
	{
		var self = this;
		self.use_paper = false;
		self.render = function()
		{
			self.element.empty();
			self.viz_util.title_and_icon(self);
			self.viz_util.wrap(self);
			var e = self.props.embed;
			if (e!=null)
			{
				e = e.replace(/width:\d*%?p?x?;height:\d*%?p?x?/g, 'width:100%;height:100%;');
				var s = e.indexOf("<a");
				e = e.substring(0,s);
				self.$wrap.append(e);
			}
		};
	},	

	custom_manual: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = self.props['title'] || "Untitled" ;
			if(self.props['display-type'] == "absolute_total")
				self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.unit = self.props['unit'];
			self.viz_util.render(self);
		};
	},

	custom_json: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = self.props['title'] || "Untitled" ;
			if(self.props['display-type'] == "absolute_total")
				self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.unit = self.props['unit'];
			self.viz_util.render(self);
		};
	},
	
	custom_xml: function()
	{
		var self = this;
		self.render = function()
		{
			self.title = self.props['title'] || "Untitled" ;
			if(self.props['display-type'] == "absolute_total")
				self.display_type = se.sparkwi.DISPLAY_ABSOLUTE_TOTAL;
			self.unit = self.props['unit'];
			self.viz_util.render(self);
		};
	},

	sparkwise_correlation: function()
	{
		var self = this;
		self.render = function()
		{
			$(self.element).empty();
			self.paper.clear();
			var title = self.props['title'] || "Untitled Comparison" ;
			self.$title = $("<div class='widget-label'>"+title+"</div>");
			$(self.element).append(self.$title);
 			$(self.element).append('<div class="widget-icons">&nbsp;</div>');
			switch(self.props['display-style'])
			{
				case 'pie':
					self.render_total_pie();
					break;
					case 'map':
					self.render_map();
					break;
				case 'trend':
					self.render_trend();
					break;
			}
		};

		self.get_label = function(i)
		{
			var s = self.util.get_name_for(self.children[i]);
			s = self.data.corr[i].name+" for "+s;
			if (s.toLowerCase().indexOf('get')==0)
				s = s.substring(4);
			s = s.substring(0,1).toUpperCase()+s.substring(1);
			return s;
		};

		self.render_total_pie = function()
		{
			var pie_data = {};
			for(var i = 0; i < self.data.corr.length; i++)
			{
				var c = self.data.corr[i];
				pie_data[self.get_label(i)] = c.values[0].data.value;
			}
			self.viz_util.pie(self,pie_data);
		};

		self.render_map = function()
		{
			var geo_list = [];
			var geo_labels = [];
			for(var i = 0;i < self.data.corr.length;i++)
			{
				geo_list.push(self.data.corr[i].values[0].data.geo_values);
				geo_labels.push(self.get_label(i));
			}
			self.viz_util.map(self, geo_list, geo_labels);
		};

		self.render_trend = function()
		{
			var graph = new self.viz_util.graph(self);
			graph.init();
			graph.draw();
		};
	}

});







