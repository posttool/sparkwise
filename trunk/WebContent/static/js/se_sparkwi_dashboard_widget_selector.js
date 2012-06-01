$.widget( "sparkwise.widget_selector",
{
	 options: {collection:[], myset:[]},
	_create: function()
	{
		var self = this;
		var r = self.options.collection;
		
		self._$tabs = $("#widget-tabs");
		self.lis = [];
		
		self._$categories = $("#widget-category-wrapper");
		self.panels = [];
	 
		for (var i=0; i<r.categories.length; i++)
		{
			var cat = r.categories[i];
			
			var $li = self._init_li(i,cat);
			self._$tabs.append($li);
			self.lis.push($li);
			
			var $p = self._init_panel(cat);
			self._$categories.append($p);
			self.panels.push($p);
			$p.carousel({width:1001});
			$p.css({'display':'none'});
		}
		
		self.my_set_idx = i;
		var $li = self._init_my_li(i);
		self._$tabs.append($li);
		self.lis.push($li);
		
		var $p = self._init_my_panel(self.options.myset.widgets);
		self._$categories.append($p);
		self.panels.push($p);
		$p.carousel({width:1001});
		$p.css({'display':'none'});

		var init_sel = $.cookie('dashboard-widget-selector-pref-idx');
		if (init_sel == null)
			init_sel = 0;
		self.select(init_sel);
		
	},
	
	get_my_set_children: function()
	{
		var self = this;
		return $($(self.$myset.children()[1]).children()[0]).children(); // because its a carousel now
	},
	
	is_in_my_set: function(wi)
	{
		var self = this;
		var c = self.get_my_set_children();
		for (var i=0; i<c.length; i++)
		{
			var $cw = $(c[i]);
			var cwd = $cw.data("data");
			if (wi.proxy.id == cwd.proxy.id)
				return true;
		}
		return false;
	},
	
	add_to_my_set: function(w)
	{
		var self = this;
		var $el = self._create_my_set_item(w);
		$el.hide();
		self.$myset.carousel("add_value",$el);
		$_grid.setWidgets($el);
 	 	self.select(self.my_set_idx);
 	 	$el.delay(300).fadeIn(1000);
	},
	
	remove_from_my_set: function(w)
	{
		var self = this;
		var c = self.get_my_set_children();
		for (var i=0; i<c.length; i++)
		{
			var $cw = $(c[i]);
			var cwd = $cw.data("data");
			if (cwd.id==w.id)
			{
				$cw.remove();
				return;
			}
		}
	},


	 _init_li: function(index, cat)
	 {
		 var self = this;
		 var css_class_name = get_css_class_for_cat(cat);
			 var $li = $("<li><div><span>"+cat.name+"</span></div></li>");
		 $li.addClass('menu-'+css_class_name);
		 $li.addClass('widget-category-tab');
		 $('span', $li).click(function(){
		 	 self.select(index);
		 	 return false;
			 });
		 
		 $('span', $li).hover(function(){
		 	 self.mouseover(index);
		 }, function() {
		 	 self.mouseout(index);
		 });
		 return $li;
	 },

	 _init_panel: function(cat)
	 {
		 var css_class_name = get_css_class_for_cat(cat);
		 var $panel = $('<div id="widgets-'+css_class_name+'" class="widget-category"></div>');
		 for (var i=0; i<cat.widgets.length; i++)
		 {
			 var w = cat.widgets[i];
			 var $r = $$div($panel,'widget-def').attr('id','widget-'+w.id);
			 var $c = $$div($r,'top '+css_class_name);
			 $r.append("<div class='label'>"+w.name+"</div>");
		 }
		 return $panel;
	 },
 
	 _init_my_li: function(index)
	 {
		 var self = this;
		var $li = $("<li><div><span>MY SET</span></div></li>");
		 $li.addClass('menu-my-set');
		 $li.addClass('widget-category-tab');
		 $('span', $li).click(function(){
		 	 self.select(index);
		 	 return false;
			 });
		 
		 $('span', $li).hover(function(){
		 	 self.mouseover(index);
		 }, function() {
		 	 self.mouseout(index);
		 });
		 return $li;
	 },

	 _init_my_panel: function(widgets)
	 {
		 var self = this;
		 self.$myset = $('<div id="widgets-my-set" class="widget-category"></div>');
		 for (var i=0; i<widgets.length; i++)
			 self.$myset.append(self._create_my_set_item(widgets[i]));
		 return self.$myset;
	 }, 
	 
	 _create_my_set_item: function(w)
	 {
		 var self = this;
		 var css_class = get_css_class_for_proxy(w.proxy);
		 var c = get_description_for_widget_instance(w);
		 var $r = $$div(null,'widget-def my-set').attr('id','widget-'+w.id);
		 var $c = $$div($r,'top '+css_class);
		 var $d = $$div($r,'trash');
		 $r.append("<div class='label'>"+c+"</div>");
		 $r.data("data",w);
		 make_dropdown_menu($r,$d,"mouseover",10,0);
		 $d.click(function(){
			 $$dialog_ok("Confirm","Are you sure you want to delete "+c+" from My Set?", "OK", "Cancel", function(){
				 do_module('Dashboard/RemoveFromMySet', [w.id], function(e)
					{
					 self.remove_from_my_set(w);
					});
			 });
		 });
		 return $r; 
		
	 },


	 mouseover: function(index)
	 {
		 var self = this, $panel = $(self.panels[index]), $li = $(self.lis[index]);
		 $li.addClass('mouseover');
	 },
	 
	 mouseout: function(index)
	 {
		 var self = this, $panel = $(self.panels[index]), $li = $(self.lis[index]);
		 $li.removeClass('mouseover');
	 },
 
	 select: function(index)
	 {
		 var self = this, $panel = $(self.panels[index]), $li = $(self.lis[index]);
		 $.cookie('dashboard-widget-selector-pref-idx',index);
		 $('#logo').attr('class', 'logo_'+$li.text().toLowerCase().replace(' ','-'));
		 if ($li.hasClass('selected')) 
		 	return;
		 if (self._$last_panel!=null) 
		 	self._$last_panel.css('display','none');
		$('#content').animate({'padding-top': '230px'}, 500);
		 $panel.fadeIn(1200);
		 self._$last_panel = $panel;
		 if (self._$last_li!=null)
			 self._$last_li.removeClass('selected');
		 $li.addClass('selected');
		 self._$last_li = $li;
	 }

});


