
$.widget( "sparkwise.carousel",
{
	options: { 
		width: 600,
		left_class: "carousel-left-arrow",
		right_class: "carousel-right-arrow"
	},
	
  _create: function()
  {
    var self = this, $e = $(this.element);
    self.$m = $("<div></div>").css({'overflow':'hidden','height': '100%', 'float':'left', 'position':'relative' });
    self.$c = $("<div></div>").css({'width': '2000px', 'position':'absolute'});
    self.$m.append(self.$c);
    var s = $e.children().length;
    for (var i=0; i<s; i++)
    {
    	self.$c.append($e.children()[0]);
    }
    self.$left = $("<div class='"+self.options.left_class+"'></div>");
    self.$left.click(function(){ self.on_click_left(); });
    self.$right = $("<div class='"+self.options.right_class+"'></div>");
    self.$right.click(function(){ self.on_click_right(); });
    $e.empty();
    $e.append(self.$left);
    $e.append(self.$m);
    $e.append(self.$right);
    self.offset = 0;
    self.left = 0;
	self._compute_max();
    self.render();
  },
  
  do_empty: function()
  {
	  var self = this;
	  self.$c.empty();
  },

  set_offset: function(t)
  {
    var self = this;
    if (t==null)
      return self.offset;
    if (!self._onscreen(t))
    {
        self.offset = Math.min(self.max_idx,t);
		self.left = self._get_pos(self.offset);
    }
    self.render();
  },
  
  get_child: function(idx)
  {
    var self = this;
    return self.$c.children()[idx];
  },
  
  length: function()
  {
	var self = this;
	return self.$c.children().length;
  },
  
  add_value: function($v)
  {
	var self = this;
	self.$c.append($v);
	self._compute_max();
	self.render();
  },
  
  render: function()
  {
	var self = this;
//    var w = self.options.width;
    var w = self.options.width - 10;
    var cw = self._get_width();
    var bigger = cw>=w;
    if (bigger)
    {
        self.$m.css({ 'width': (w)+"px" });
		self.$c.animate({'left': -self.left +"px" },100);
    	self.$left.show();
    	self.$right.show();
	    if (self.offset==0)
		  self.$left.css({'opacity':.2});
	    else
		  self.$left.css({'opacity':1});
	    if (self.offset==self.max_idx)
		  self.$right.css({'opacity':.2});
	    else
		  self.$right.css({'opacity':1});
    }
    else
    {
        self.$m.css({ 'width': cw+"px" });
    	self.$left.hide();
    	self.$right.hide();
    	self.$c.css({'left':'0px'});
    }
  },

  on_click_left: function()
  {
	  var self = this;
	  self.offset--;
	  if (self.offset<0)
		  self.offset=0;
	  self.left = self._get_pos(self.offset);
	  self.render();
  },
  
  on_click_right: function()
  {
	  var self = this;
	  self.offset++;
	  if (self.offset>self.max_idx)
		  self.offset = self.max_idx;
	  self.left = self._get_pos(self.offset);
	  self.render();
  },
  
  _get_pos: function(idx)
  {
	  var self = this;
	  var cw = self._get_width();
	  var w = self.options.width;
	  var cl = 0;
	  var c = self.$c.children();
	  for (var i=0; i<idx; i++)
	  {
	    	cl += $(c[i]).outerWidth(true);
	  }
	  return cl;
  },
  
  _compute_max: function()
  {
	  var self = this;
	  var cw = self._get_width();
	  var w = self.options.width;
	  var c = self.$c.children();
	  var cl = 0;
	  for (var i=0; i<c.length-1; i++)
	  {
	    	cl += $(c[i]).outerWidth(true);
	    	if (cw - cl < w)
	    	{
	    		self.max_idx = i+1;
	    		return;
	    	}
	  }
	  self.max_idx = c.length;
  },
  
  _onscreen: function(idx)
  {
	  var self = this;
	  var w = self.options.width;
	  var cl = 0;
	  var c = self.$c.children();
	  for (var i=0; i<idx; i++)
	  {
	    cl += $(c[i]).outerWidth(true);
	  }
  	  var cr = cl + $(c[idx]).outerWidth(true);
	  return cl - self.left >= 0 && cr - self.left <= w;
  }, 
  
  _get_width: function()
  {
    var self = this;
    var cw = 0;
    var s = self.$c.children().length;
    for (var i=0; i<s; i++)
    {
    	var $ec =  $(self.$c.children()[i]);
    	cw += $ec.outerWidth(true);
    }	
    return cw;
  }

  


});