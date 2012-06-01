se.sparkwi.dashboard.controller = function(el, o) 
{
  var def = 
  {
	  options: { widget_draggables: [] },
	  init: function()
	  {
	    var self = this;

	    self.element = el;
	    jQuery.extend(self.options,o);
	    
	    self.width      = 4;
	    self.height     = 4;
	    self.cell_size  = 230;
	    self.cell_pad   = 10;
	
	    el.css({'position':'relative'});
	
	    self._$loading = $("<div class='widget-grid-cell-loading' ></div>")
	    					.css({'position':'absolute', 'top':'0px', 'left': '0px', 'height': '100px', 'width': '100px'});
	    self._$loading.hide();
	    el.append(self._$loading);
	
	    self._init_bg();
	
	    self._$children = $("<div></div>").css({'position':'absolute', 'top':0, 'left': 0});
	    el.append(self._$children);
	
	    el.droppable({
	      drop: function( event, ui )
	      {
	        //find out if something is in the row column
	        // if correlation widget is there and in config mode
	        var info = self._get_cell_dd_event_info(ui);
	        if(ui.draggable.hasClass("widget-def"))
	        {
	          return self._process_drop_widget_def(info,ui);
	        }
	        else if(ui.draggable.hasClass("widget-grid-cell"))
	        {
	          var $cell = $(ui.draggable).data('def');
	          return self._process_drop_widget($cell, info);
	        }
	        else if(ui.draggable.hasClass("widget-correlation-cell"))
	        {
	          return self._process_drop_remove_from_correlation(info,ui);
	        }
	      }
	    });
	
	    self.setWidgets(self.options.widget_draggables);
	  },
	
	
	  /*  add draggable to a list of widget definitions
	   *  each widget definition element must have the id widget-{{dbkey}}
	   *    param widgets the list of page elements
	   */
	  setWidgets: function(widgets)
	  {
	    var self = this, el = this.element;
	    widgets.each(function(i,el)
	    {
	      $(el).draggable({
	        opacity:  0.85,
	        helper:   "clone",
	        scrollSensitivity: 50,
	        cursorAt: { left: 5 },
	        appendTo: 'body' ,
	      	drag:     function(event, ui) {
	            var info = self._get_cell_dd_event_info(ui);
	        	self._process_drag_widget_def(info,ui);
	        },
	        zIndex:   9999
	      });
	      $(el).css({cursor:'pointer'})
	    });
	  },
	  
	  /* converts regular widget to correlation */
	  convertToCorrelation: function(wid,type)
	  {
		var self = this;
		var $cell = self._get_cell_by_id(wid);
		var wi = $cell.data();
		do_module('Dashboard/ConvertToCorrelation',[wid,type], function(corr_wi)
		{
			  $cell.remove();
			  $cell = self._add_cell('sparkwise_correlation', wi.rect[1], wi.rect[0], wi.rect[2], wi.rect[3]);
		      $cell.data(corr_wi);
		});
	  },
	  
	  /* converts correlation to regular widget  */
	  convertToWidget: function(wid,type)
	  {
		var self = this;
		var $cell = self._get_cell_by_id(wid);//<se.sparkwi.dashboard.cell
		var wi = $cell.data();
		do_module('Dashboard/ConvertToWidget',[wi.id,type], function(wi)
		{
			  $cell.remove();
			  $cell = self._add_cell(wi.proxy.widget, wi.rect[1], wi.rect[0], wi.rect[2], wi.rect[3]);
		      $cell.data(wi);
			  $cell.do_enter_config();
		});
	  },
	  
	
	  /* set up a grid from a dashboard key */
	  load: function(dashboard_key)
	  {
	    var self = this;
	    self.wi_map = {};
	    if (self.dashboard_key == dashboard_key)
	      return;
	    if (dashboard_key!=null)
	    	self.dashboard_key = dashboard_key;
	    self._$loading.show();
	    self._$bg.hide();
	    self._$children.hide();
	    self._over_bg_cell();//resets the grid rollover hilight state
		$$tooltip_hide();
	
	    self._$children.empty();
	    do_module('Dashboard/GetWidgets', [self.dashboard_key], function(data) 
	    {
	      self.data = data;
	      self._$loading.hide();
	      self._$bg.show();
	      self._$children.show();
	      for (var i=0; i<data.length; i++)
	      {
	        var wi = data[i];
	        var	$cell = self._add_cell(wi.proxy.widget, wi.rect[1], wi.rect[0], wi.rect[2], wi.rect[3]);
	        $cell.data(wi);
	        self.wi_map['wi_'+wi.id]=$cell;
	      }
	      self._update_height();
	    });
	  },
	  refresh:function()
	  {
		  /* this could be more complex */
		  /* it should take a list of wi it wants
		   * to refresh or some sort of refresh profile.
		   * right now this works fine for events.
		   * 
		   * could be {add:[wi,wi,wi],delete:[wi,wi,wi],delete_by_proxy:[proxy] etc}
		   */
		  var self = this;
	      for (var i=0; i<self.data.length; i++)
	      {
	        var wi = self.data[i];
	        var	$cell = self.wi_map['wi_'+wi.id];
	        $cell.data(wi);
	      }  
	  },
	  _add_cell: function(widget,r,c,w,h)
	  {
	    var self = this;
	    if (w==null) w=1;
	    if (h==null) h=1;
	    var p = self.cell_pad;
	    var pc = self.cell_size + p;
	    
	    var $cell = new se.sparkwi.dashboard.cell({
	      is_editable: true,
	      deletef: function(){ self._on_delete_cell($cell); },//why not listen for a delete event?
	      width:(pc*w-p), 
	      height: (pc*h-p)
	    });
	    $cell.dashboard(self);
	    $cell.draggable({
	      handle: '.widget-drag-handle',
	      helper: 'clone',
          cursorAt: { left: 35 },
	      drag: function(event, ui) { 
	    	  var info = self._get_cell_dd_event_info(ui);
	    	  var $helper = ui.helper;
	    	  self._process_drag_widget($cell, $helper, info);
	      }
	    });
	    $cell.resizable({
//      start: function(event, ui) { self._process_instance_resize_start(ui.size.width,ui.size.height,$cell); },
	      resize: function(event, ui) { self._process_instance_resizing(ui.size.width,ui.size.height,$cell); },
	      stop: function(event, ui) { self._process_instance_resize_stop(ui.size.width,ui.size.height,$cell); },
	      helper: "ui-resizable-helper"
	    });
	    $cell.css({
	      'width':  (pc*w-p)+'px',
	      'height': (pc*h-p)+'px',
	      'top':    (pc*r)+'px',
	      'left':   (pc*c)+'px',
	      'position': 'absolute'
	    });
		var intersect = self._get_intersecting_cells(c,r,w,h);
	    if(intersect)
			$cell.css('zIndex',parseInt(intersect.cell.css('zIndex'))+1);
	    else
	    	$cell.css('zIndex',10);
	
	    self._$children.append($cell.element);
	    return $cell;
	  },
	
	  _add_temp_cell: function(message,r,c,w,h)
	  {
	    var self = this;
	    if (w==null) w=1;
	    if (h==null) h=1;
	    var p = self.cell_pad;
	    var pc = self.cell_size + p;
	    var $cell = $("<div>"+message+"</div>");
	    $cell.rect = [r,c,w,h];
	    $cell.css({
	      'width':  (pc*w-p)+'px',
	      'height': (pc*h-p)+'px',
	      'top':    (pc*r)+'px',
	      'left':   (pc*c)+'px',
	      'position': 'absolute'
	    });
	    $cell.addClass('widget-grid-cell-loading');
		var intersect = self._get_intersecting_cells(c,r,w,h);
	    if(intersect)
			$cell.css('zIndex',parseInt(intersect.cell.css('zIndex'))+1);
	    else
	    	$cell.css('zIndex',10);
	
	    self._$children.append($cell);
	    return $cell;
	  },
	  
	  
	  /* all the drag and drop handlers */
	  _process_drag_widget_def: function(info,ui)
	  {
		var self = this, el = this.element;
        self._over_bg_cell();
	    if (!info.valid)
	        return false;
	    var intersect = self._get_cell_dd_intersection_info({rect:[0,0,1,1]},info);
	    if (!intersect.intersection)
	    	self._over_bg_cell(info.row,info.col);
	  },
	
	  _process_drop_widget_def: function(info,ui)
	  {
	    var self = this;
	    self._over_bg_cell();
	    if (!info.valid)
	      return false;
	    var intersect = self._get_cell_dd_intersection_info({rect:[0,0,1,1]},info);
	    if (intersect.intersection)
	    	return;
	    
	    var from_my_set = ui.helper.hasClass("my-set");
	    var $temp = self._add_temp_cell('',info.row,info.col);
	    if (from_my_set)
	    {
		    var wid = parseInt(ui.helper.attr('id').substring(7));
		    do_module('Dashboard/AddWidgetInstance', [self.dashboard_key,wid,info.col,info.row], function(data) {
		    	$temp.remove();
		    	var $cell = self._add_cell(data.proxy.widget,info.row,info.col);
		        $cell.data(data);
		        self._update_height();
		    });   
	    }
	    else
	    {
		    var wid = parseInt(ui.helper.attr('id').substring(7));
		    do_module('Dashboard/AddWidget', [self.dashboard_key,wid,info.col,info.row], function(data) {
		    	$temp.remove();
		    	var $cell = self._add_cell(data.proxy.widget,info.row,info.col);
		        $cell.data(data);
		        self._update_height();
		    });   
		 }
	  },
	
	  _process_drag_widget: function($cell,$helper,info)
	  {
		var self = this;
		$cell.draggable('option','revert',false);
	  	self._reset_dd_helper($helper);
    	self._over_bg_cell();
	    if (!info.valid)
	      return false;
	    var wi = $cell.data();
	    var intersect = self._get_cell_dd_intersection_info(wi,info,$cell);
	    var z = 1;
	    if(!intersect.intersection)
	    {
	    	self._over_bg_cell(info.row, info.col, wi.rect[2], wi.rect[3]);
	    }
	    else if(intersect.over_a_corr)
		{
	    	self._over_bg_cell();
			if (!intersect.has_common_return_types)
			{
				$helper.find('.widget-over-correlation-but-not-droppable .bg-not-droppable').css('opacity', '.3');
				$helper.find('.widget-over-correlation-but-not-droppable').show();
				return;
			}
			$helper.find('.widget-front').hide();
			$helper.find('.ui-resizable-handle').hide();
			$helper.find('.widget-over-correlation').show();
			intersect.cell.over_correlation(true);
			self._$last_intersection = intersect.cell;
			z = .6;
		}
	    
	    var $z = $helper.find('.widget-front');
	    $z.css({'zoom': z, '-moz-transform': 'scale('+z+')'});
		
	  },
	  
	  _process_drop_widget: function($cell,info)
	  {
	    var self = this;
    	self._over_bg_cell();
	    if (!info.valid)
	        return false;
	
	    var wi = $cell.data();
	    var intersect = self._get_cell_dd_intersection_info(wi,info,$cell);
	    if(!intersect.intersection)
	    {
	    	$cell.animate({'top':info.row*info.pc,'left':info.col*info.pc},100);
	        do_module('Dashboard/UpdateWidgetRect', [ wi.id, info.col, info.row, wi.rect[2], wi.rect[3] ], function(data) { 
	        	$cell.data(data);
	            self._update_height();
	        });
	    }
	    else if (!intersect.dragging_a_corr && intersect.over_a_corr && intersect.has_common_return_types)
	    {
	    	var $drop_cell = intersect.intersect.cell;
			var args = [$drop_cell.key(), wi.id];
			do_module('Dashboard/AddWidgetToCorrelation', args, 
					function(data) {
						$drop_cell.data(data);
				});
	    }
	    else
	    {
	    	$cell.draggable('option','revertDuration',150);
	    	$cell.draggable('option','revert',true);
	    }
	    
	  },
	  
	  _process_drag_widget_corr_member: function(event,ui)
	  {
		  var self = this;
		  var info = self._get_cell_dd_event_info(ui);
		  //console.log(info,ui)
	  },
	  
	  _process_drop_remove_from_correlation: function(info,ui)
	  {
		    var self = this;
	    	var r 			= info.row;
	    	var c 	   		= info.col;
	    	var draggable   = $(ui.draggable);
	    	var $corr_cell 	= $(ui.draggable.data('corr_cell'));
	
	    	var corr_key 	= draggable.data('corr_key');
	    	var member_key 	= draggable.data('wi_key');
	  		var intersect 	= self._get_intersecting_cells(c,r,1,1);
	
	  		if(intersect && intersect.wi.id == corr_key)
	    	{
				return;
	    	}
	
	  		draggable.fadeOut();
			do_module('Dashboard/RemoveWidgetFromCorrelation', [corr_key, member_key], function(data) {
	    		$corr_cell.data(data);
			});
	  },
	  
	  _process_instance_resizing: function(width,height,$cell)
	  {
	    var self = this;
	    var p = self.cell_pad;
	    var pc = self.cell_size + p;
	    var w = Math.round(width/pc);
	    var h = Math.round(height/pc);
	    var wi = $cell.data();
		var intersect = self._get_intersecting_cells(wi.rect[0],wi.rect[1],w,h, wi.id);
	    if (intersect || wi.rect[0]+w > 4)
	    {
	    	logging.log('cant do it');
	    }
	    else
	    {
	    	//sok
	    }
		  
	  },
	
	  _process_instance_resize_stop: function(width,height,$cell)
	  {
	    var self = this;
	    var p = self.cell_pad;
	    var pc = self.cell_size + p;
	    var r = Math.round(width/pc);
	    var c = Math.round(height/pc);
	    if (r<1) r = 1;
	    if (c<1) c = 1;
	    if (c>4) c = 4;
	    var wi = $cell.data();
		var intersect = self._get_intersecting_cells(wi.rect[0],wi.rect[1],r,c, wi.id);
	    if (intersect || wi.rect[0]+r > 4)
	    {
	        $cell.animate({'width':wi.rect[2]*pc-p,'height':wi.rect[3]*pc-p},40);
	    }
	    else
	    {
	        $cell.animate({'width':r*pc-p,'height':c*pc-p},40);
	        $cell.size([r*pc-p,c*pc-p]);
	        do_module('Dashboard/UpdateWidgetRect', [ wi.id, wi.rect[0], wi.rect[1], r, c ], function(data) { 
	        	$cell.data(data);
	        	self._update_height();
	        });
	     }
	
	  },
	  
	  /* helpers for dnd */
	  _get_cell_dd_event_info: function(ui)
	  {
		var self = this, el = this.element;
		var p    = $.globalToLocal(el,ui.offset.left,ui.offset.top);
		var top  = p.y;
		var left = p.x;
		var pc = self.cell_size + self.cell_pad;
		var r = Math.floor(top/pc);
		var c = Math.floor(left/pc);
		var valid = self._$bg_els[r] != null && self._$bg_els[r][c] != null;
		return {
			row:r, col:c,
			top:top, left:left,
			pc:pc,
			valid:valid
		}
	  },
	  
	  _get_cell_dd_intersection_info: function(wi, info, $cell)
	  {
		var self = this, el = this.element;
		var intersect = self._get_intersecting_cells(info.col,info.row,wi.rect[2],wi.rect[3]);
		if (intersect ==  null)
			return { intersection: false };
		if (wi.id == intersect.wi.id)
			return { intersection: false, over_self: true };
			
		var def = intersect.cell.definition();
		var over_a_corr = intersect.wi.type == 1;
		var info = { intersection: true, def: def, intersect: intersect, cell: intersect.cell,
								  over_a_corr: over_a_corr,
				                  dragging_a_corr: $cell == null ? false : $cell.definition().is_correlation };
		if(over_a_corr && wi.id/* is not a widget def if it has an id */)
		{
			info.common_return_types       = self._get_common_return_types(intersect.cell, wi);
			info.has_common_return_types   = info.common_return_types.length!=0;
		}
		return info;
	  },
	  
	  _get_woffset: function()
	  {
	    var self = this, el = this.element;
	    return {top: el.offset().top , left: el.offset().left };
	  },
	
	  _get_common_return_types: function(corr,wi)
	  {
	    var corr_data = corr.data();
	    if (corr_data.children.length==0)
	    	return [-1];
		var corr_child0_def = corr_data.children[0].widget;
		var drop_def = wi.proxy.widget;
		var common_return_types = [];
		for (var i=0; i<drop_def.return_types.length; i++)
		{
			var f = jQuery.inArray(drop_def.return_types[i], corr_child0_def.return_types);
			if (f!=-1)
				common_return_types.push(drop_def.return_types[i]);
		}
		return common_return_types;
	  },
	  
	  _reset_dd_helper: function($helper)
	  {
		  var self = this;
		  $helper.css('zIndex', 9999);
		  $helper.find('.widget-front').css('opacity',.85);
	  	  $helper.find('.widget-over-correlation').hide();
		  $helper.find('.widget-over-correlation-but-not-droppable').hide();
		  $helper.find('.widget-front').show();
		  $helper.find('.ui-resizable-handle').show();

	  	  if (self._$last_intersection!=null)
	      {
	  		self._$last_intersection.over_correlation(false);
	  		self._$last_intersection = null;
	  	  }  
	  },
	
	  _on_delete_cell: function($cell)
	  {
	    var self = this;
	    var d = $cell.data();
		$$dialog_ok('Confirm','Are you sure you want to delete this widget?','OK','Cancel',
				function()
				{
					do_module('Dashboard/DeleteWidget', [ $cell.key() ], function(data) {
						$cell.remove();
				    	self._over_bg_cell();
						if(data._was_in_correlation_)
							self.load();
					});			
				});
	    
	
	  },
	  
	  _get_intersecting_cells:function(x,y,w,h,exclude_key)
	  {
		  var self = this;
		  var cc = self._$children.children();
		  var isects = [];
		  for(var i = 0;i < cc.length;i++)
		  {
			  var $cell = $(cc[i]);
			  var cell = $cell.data('def');
			  if (cell == null)
				  continue;
			  if (cell.hasClass('ui-draggable-dragging'))
				  continue;
			  var wi   = cell.data();
			  if (wi.rect==null)
		      {
				  logging.info('no wi rect');
				  continue; 
		      }
			  var wih = wi.rect[3];
			  var wiw = wi.rect[2];
			  if(cell.definition().is_correlation && cell.in_edit_mode())
			  {
				  wih = Math.max(2.5,wih);
			  }
	
			  if(exclude_key)
				  if (wi.id == exclude_key)
				  	continue;
			  
			  //var A = {x1: x, y1: y, x2: x+w, y2: y+h};
			  var A = {x1: x, y1: y, x2: x+w-.5, y2: y+h-.5};
			  var B = {x1: wi.rect[0], y1: wi.rect[1], x2: wi.rect[0]+wiw, y2: wi.rect[1]+wih};
		      if(A.x1 < B.x2 && A.x2 > B.x1 && A.y1 < B.y2 && A.y2 > B.y1)
			  {
			    isects.push({cell:cell,wi:wi});
			  }
		  }
		  if(isects.length > 0)
		  {
		  	isects.sort(function (a, b){
					return (parseInt(b.cell.css('zIndex')) - parseInt(a.cell.css('zIndex'))) //causes an array to be descending by zIndex
		  	});
		  	return isects[0]
		  }
		  return null;
	  },
		
	  _get_cell_by_id: function(key)
	  {
		  var self = this;
		  var cc = self._$children.children();
		  for(var i = 0;i < cc.length;i++)
		  {
			  var $cell = $(cc[i]).data('def');
			  var wi   = $cell.data();
			  if (wi.id==key)
				  return $cell;
		  }
		  return null;
	  },
	  
	  _init_bg: function()
	  {
	    var self = this, el = this.element;
	    var pc = self.cell_size + self.cell_pad;
	    if (self._$bg == null)
	    {
	    	self._$bg = $('<div></div>').css({'position':'absolute', 'top':0, 'left': 0});
	        el.append(self._$bg);
	    }
	    else
	    {
	    	self._$bg.empty();
	    }
	    self._$bg_els = [];
	    for (var i=0; i<self.height; i++)
	    {
	      var row = $('<div></div>')
	                .attr('id','__wr'+i)
	                .css({'position':'absolute', 'height':self.cell_size+'px', 'top': (i*pc)+'px'});
	      self._$bg_els[i] = [];
	      for (var j=0; j<self.width; j++)
	      {
	         var col = $('<div></div>')
	                .attr('id','__wc'+i+'_'+j)
	                .css({'position':'absolute', 'width':self.cell_size+'px','height':self.cell_size+'px', 'left':j*pc})
	                .addClass('rounded-corners bgcell');
	         self._$bg_els[i][j] = col;
	         row.append(col);
	      }
	      self._$bg.append(row);
	    }
	    
	    self._$over_bg_cell = $('<div></div>').css({'position':'absolute'}).addClass('rounded-corners bgcell ui-over').hide();
	    self._$over_bg_cell.css('opacity', '.3');
	    self._$bg.append(self._$over_bg_cell);
	    
	    el.height(i*pc);
	  },
	  
	  _update_height: function()
	  {
		  var self = this;
		  var max_h = 4;
		  var cc = self._$children.children();
		  for(var i = 0;i < cc.length;i++)
		  {
			  var $cell = $(cc[i]).data('def');
			  var wi   = $cell.data();
			  var wix =  wi.rect[1]+wi.rect[3]+1;
			  max_h = Math.max(max_h,wix);
		  }
		  if (self.height!=max_h)
		  {
			  self.height = max_h;
			  self._init_bg();
		  }
	  },
	
	  _over_bg_cell: function(r,c,w,h)
	  {
	    var self = this;
	    if (r==null)
	    {
	    	self._$over_bg_cell.hide();
	    	return;
	    }
	    if (w==null) w = 1;
	    if (h==null) h = 1;
		self._$over_bg_cell.show();
		var p = self.cell_pad;
		var pc = self.cell_size + p;
		self._$over_bg_cell.css({
		  'width':  (pc*w-p)+'px',
		  'height': (pc*h-p)+'px',
		  'top':    (pc*r)+'px',
		  'left':   (pc*c)+'px',
		  'position': 'absolute'
		});
	  }
   }
  jQuery.extend(this,def);
  this.init();
};
