/**
 *	dashboard tabs with add button and 'board tools'
 */

$.widget( "sparkwise.dashboard_tabs",
{
	_create: function()
	{
		var self = this, $e = $(this.element);
		self._$clist = $e.clist({
			add:      function(e,ui){ self._add(); },
			deletef:  function(e,ui){ self.do_delete(ui.index); },
			select:   function(e,ui){ self._trigger('select',0,{index: ui.index}); },
			cell_renderer: function(d)
			{
				var $c = $("<div class='dashboard-tab rounded-corners-top'></div>");
				var $l = "<span class='click_area'>"+d.name+"</span>";
				var $pl = $("<span class='dashboard-published-button' id='dashboard-published-button-"+d.uuid+"'>&nbsp; &nbsp;&nbsp;</span>");
				$pl.click({uuid:d.uuid,name:d.name},function(ee){
					var uuid = ee.data.uuid;
					var url	= '/b/'+uuid;
					window.open(url);
				});
				if (d['public']) 
					$pl.css('display', 'inline') 
				else
					$pl.hide();
				$c.append($pl);
				$c.append($l);
				return $c;
			},
			deletable:   false,
			selectable:  true,
			carousel:    true,
			carousel_width:  760,
			carousel_left:  'dashboard-tabs-carousel-left',
			carousel_right: 'dashboard-tabs-carousel-right',
			on_select:   function(idx,cell){self._do_select_update(idx,cell);}
		});
		self._$list_wrap = $("<div class='dashboard-tabs-all'></div>");
		self._$trigger = $("<a class='dashboard-tabs-all rounded-corners-top'>All</a>");
		self._$list_items = $("<div class='dashboard-tabs-all-dd'></div>");
		self._$lul = $("<ul></ul>");
		self._$list_items.append(self._$lul);
		self._$list_wrap.append(self._$trigger);
		self._$list_wrap.append(self._$list_items);
		self._$list_wrap.append("<div class='clear'></div>");
		$e.append(self._$list_wrap);
		self._$list_wrap.click(function()
		{
			self._$list_items.stop().slideToggle(400);
		});

		/*	bind the html elements to functions on click */
		var dd = make_dropdown_menu( $('.dashboard-options'), $('.dashboard-tools-dd') );
		$('#tools-dashboard-rename').click(function() 
		{
			dd.hide();
			self.do_rename();
		});
//		$('#tools-dashboard-duplicate').click(function() 
//		{
//			dd.hide();
//			self.do_dupe();
//		});
//		$('#tools-dashboard-embed').click(function() 
//		{
//			dd.hide();
//			$$dialog('Embed Dashboard', '', '$('#dashboard-embed'), [{name:'OK'}]);
//		});
//		$('#tools-dashboard-export').click(function()
//		{
//			dd.hide();
//			location.href = '/Dashboard/Export/.raw?id='+current_db().id;
//		});
		$('#tools-dashboard-delete').click(function() 
		{
			dd.hide();
			self.do_delete();
		});
		$('#tools-dashboard-publish').click(function() 
		{
			dd.hide();
			self.do_publish();
		});
		$('#tools-dashboard-fb').click(function() 
		{
			dd.hide();
			self.do_pubshare('Facebook');
		});
		$('#tools-dashboard-tw').click(function() 
		{
			dd.hide();
			self.do_pubshare('Twitter');
		});
		$('#tools-dashboard-unpublish').click(function() 
		{
			dd.hide();
			self.do_unpublish();
		});
		$('#tools-dashboard-view-published').click(function() 
		{
			dd.hide();
			self.do_view_published();
		});
	},
	_do_select_update:function(idx,cell)
	{
		var self = this;
		var board =	self._$clist.clist('value')[idx];
		var name = board.name;
		$('#tools-dashboard-rename').text("Rename "+name);
		$('#tools-dashboard-duplicate').text("Duplicate "+name);
		$('#tools-dashboard-embed').text("Embed "+name);
		$('#tools-dashboard-export').text("Export "+name);
		$('#tools-dashboard-delete').text("Delete "+name);
		
		$('#tools-dashboard-publish').text("Publish "+name+" to the Web");
		$('#tools-dashboard-unpublish').text("Unpublish "+name);
		$('#tools-dashboard-view-published').text("View Published "+name);
		
		if(board['public'])
		{
			$('#tools-dashboard-publish').parent().hide();
			$('#tools-dashboard-unpublish').parent().show();
			$('#tools-dashboard-view-published').parent().show();
			$('#tools-dashboard-fb').text("Share "+name+" on Facebook");
			$('#tools-dashboard-tw').text("Share "+name+" on Twitter");
		}
		else
		{
			$('#tools-dashboard-publish').parent().show();
			$('#tools-dashboard-unpublish').parent().hide();
			$('#tools-dashboard-view-published').parent().hide();
			$('#tools-dashboard-fb').text("Publish "+name+" to Facebook");
			$('#tools-dashboard-tw').text("Publish "+name+" to Twitter");
		}
		
		var num_dashboards = self._$clist.clist('value').length;
		if(num_dashboards > 1)
			$('#tools-dashboard-delete').parent().show(); 
		else
			$('#tools-dashboard-delete').parent().hide();	
		
	},

	value: function(t)
	{
		var self = this, el = this.element;
		if (t==null)
			return self._$clist.clist('value');

		self._$clist.clist('value',t);
		self._$lul.empty();
		for (var i=0; i<t.length; i++)
			self._add_to_lul(t[i], i);
	},
 
	select: function(index)
	{
		var self = this;
		self._$clist.clist('select',index);
	},

	_add: function()
	{
		var self = this, el = this.element;
		$$dialog_input('Add Board','Name','OK','Cancel',
			function(name) {
				do_module('Dashboard/AddDashboard',[name], function(data) {
					self._$clist.clist('add_value',data);
					var idx = self.value().length-1;
					self._add_to_lul(data,idx);
					self.select(idx);
					self._trigger('select',0,{index: idx});
				});
			});
	},
	
	_add_to_lul: function(d,idx)
	{
		var self = this;
		var $item = $("<li></li>");
		if (idx==0)
			$item.attr({'class':'first'});
		$item.append('<a>'+d.name+'</a>');
		$item.click(function(){ self.select(idx); self._trigger('select',0,{index: idx}); })
		self._$lul.append($item);
	},

	do_dupe: function()
	{
		var self = this, el = this.element;
		var value = self.value();
		var idx = self._$clist.clist('selection_index');
		$$dialog_input('Duplicate Dashboard','Name','Done','',
			function(name) {
				do_module('Dashboard/DuplicateDashboard', [value[idx].id,name], function(data) {
					self._$clist.clist('add_value',data);
					var idx = self.value().length-1;
					self.select(idx);
					self._trigger('select',0,{index: idx});
				});
			});
	},
	
	do_rename: function()
	{
		var self = this, el = this.element;
		var value = self.value();
		var idx = self._$clist.clist('selection_index');
		$$dialog_input('Rename Dashboard','Name','OK','Cancel',
			function(name) {
				do_module('Dashboard/RenameDashboard', [value[idx].id, name], function(data) {
					//var $cell = $(self._$clist.clist('get_child',idx));
					//$cell.find('.click_area').text(data.name);
					var v = self._$clist.clist('get_value',idx);
					v.name = data.name;
					self._$clist.clist('set_value',idx, v)
					self.select(idx);
					self._do_select_update(idx);
				});
			});
	},

	do_delete: function(idx)
	{
		var self = this, el = this.element;
		var value = self.value();
		if (idx==null)
				idx = self._$clist.clist('selection_index');
		$$dialog_ok('Really?','Are you sure you want to permanently delete '+value[idx].name+'?','Delete','Cancel',
			function() {
			do_module('Dashboard/DeleteDashboard', [value[idx].id], function(data) {
					self._$clist.clist('remove',idx);
					idx--;
					if (idx<0) idx=0;
					self.select(idx);
					self._trigger('select',0,{index: idx});
				});
			});
	},
	

	get_pub_extra: function(dash)
	{
		var $xtra = $$div(null,'dialog-extra');
		
		var $line = $$div($xtra,'dialog-line');
		$$label($line,'Organization','dialog-label');
		var $orgname = $$input($line);
		
		var $line2 = $$div($xtra,'dialog-line');
		$$label($line2,'Board Title','dialog-label');
		var $dbname = $$input($line2);
		
		$dbname.parent().css({'display':'inline-block'});
		$orgname.parent().css({'display':'inline-block'});

		$dbname.setValue(dash.public_name ? dash.public_name : dash.name);
		$orgname.setValue(dash.public_org);
		
		jQuery.extend($xtra, { $dbname: $dbname, $orgname: $orgname });
		return $xtra;
	},
	
	do_publish:function(share)
	{
		var self = this;
		var value = self.value();
		var idx = self._$clist.clist('selection_index');
		var dash = value[idx];
		var $xtra = self.get_pub_extra(dash);
		$$dialog_ok('Publish Confirmation','You are about to make this board public, which means anyone can view it. '+
			'Please add a board title before doing so. Items left blank will not be included.','Publish','Cancel',
				function() 
				{
				$$dialog('Working...','');
				do_module('Dashboard/PublishDashboard', [dash.id, $xtra.$dbname.getValue(), $xtra.$orgname.getValue() ], function(data) {
					$('#dashboard-published-button-'+data.uuid).css('display','inline');
					//value[idx] = data;
					self._$clist.clist('set_value',idx,data);
					self.select(idx);
					$$dialog_close();
					$$dialog_ok('Publish Confirmation','Your board has been published.',
						'View','Cancel',function(){ window.open('/b/'+data.uuid+"?"+share); });
				});
				},$xtra);
	},
	
	do_pubshare:function(s)
	{
		var self = this;
		var value = self.value();
		var idx = self._$clist.clist('selection_index');
		var dash = value[idx];
		var $xtra = self.get_pub_extra(dash);
		var get_name = function(){
			var name = dash.public_name ? dash.public_name : dash.name;
			var org = dash.public_org;
			if (org && name)
				return org+" | "+name;
			else if (name)
				return name;
			else if (org)
				return org;
			else
				return "Sparkwise Dashboard";

		}
		var do_share = function(uuid){
			var link = 'http://sparkwi.se/b/'+uuid;
			if (s=='Facebook')
			{
				FB.ui(
					{
						method: 'feed',
						name: get_name(),
						link: link,
						picture:	'http://sparkwi.se/static/image/logo_sparkwise.png',
						caption: 'powered by Sparkwise'
					},
					function(response) {
//						if (response && response.post_id) {
//							alert('Post was published.');
//						} else {
//							alert('Post was not published.');
//						}
					}
				);
			}
			else
			{
				window.open("http://twitter.com/intent/tweet?url="+encodeURIComponent(link), 'tweet','width=600,height=300')
			}
		};
		if (dash['public'])
			do_share(dash.uuid);
		else
			$$dialog_ok('Publish to '+s,'You are about to make this board public, which means anyone can view it. '+
				'Please review the information below before doing so. Items left blank will not be included.','Publish','Cancel',
					function() 
					{
						do_module('Dashboard/PublishDashboard', [dash.id, $xtra.$dbname.getValue(), $xtra.$orgname.getValue() ], function(data) {
							$('#dashboard-published-button-'+data.uuid).css('display','inline');
							value[idx] = data;
							self.select(idx);
							do_share(data.uuid);
						});
					},$xtra);
	},
	
	do_unpublish:function(idx)
	{
		var self = this, el = this.element;
		var value = self.value();
		if (idx==null)
				idx = self._$clist.clist('selection_index');
		$$dialog_ok('Unpublish Confirmation','Are you sure you want to unpublish this published board? Doing so will remove this board from public view.','Unpublish','Cancel',
			function() {
			do_module('Dashboard/UnpublishDashboard', [value[idx].id], function(data) {
				$('#dashboard-published-button-'+data.uuid).hide();
				self._$clist.clist('set_value',idx,data);
				self.select(idx);
				});
			});
	},
	do_view_published:function(idx)
	{
		var self = this, el = this.element;
		var value = self.value();
		if (idx==null)
			idx = self._$clist.clist('selection_index');
		var board = value[idx];
		var uuid = board.uuid;
		var url	= '/b/'+uuid;
		window.open(url);
	}


});

