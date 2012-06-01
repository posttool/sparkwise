//TODO unify method sigs too: getValue, setValue, setOptions, etc

function $$$($parent, class_name, t, r, b, l, callback)
{
	var p = {'position':'absolute'};
	if (t!=null) p.top = t+"px";
	if (r!=null) p.right = r+"px";
	if (b!=null) p.bottom = b+"px";
	if (l!=null) p.left = l+"px";
	var $c = $("<div></div>").css(p);
	if (class_name!=null)
		$c.addClass(class_name);
	if (callback!=null)
		$c.click(callback);
	$parent.append($c);
	return $c;
}


function $$el($parent,tag_name,class_name,callback,evt_data)
{
	var $c = $('<'+tag_name+'></'+tag_name+'>');
	if (class_name != null)
		$c.addClass(class_name);
	if (callback != null)
		if (evt_data != null)
			$c.click(evt_data,callback);
		else
			$c.click(callback);
	if ($parent != null)
		$parent.append($c);
	return $c;
}

function $$div($parent,class_name,on_click)
{
	return $$el($parent,'div',class_name,on_click);
}

function $$form($parent,class_name)
{
	var $f =	$$el($parent,'form',class_name).attr('action','#');
	// return ajax form by default
	$f.submit(function(){ return false; });
	return $f;
}

function $$label($parent,text,class_name)
{
	var $c = $$div($parent,class_name);
	$c.append(text);
	return $c;
}

function $$button($parent,text,event_data,callback)
{
	var $c = $$el($parent,'button',null,callback,event_data);
	$c.append(text);
	return $c;
}

function $$select($parent,event_data,callback,name)
{
	var $c = $$el($parent,'select','widget-prop-editor')
	if (name!=null)
		$c.attr('name',name);
	if (callback!=null)
		$c.bind('change',event_data,callback);
	$c.extend(
	{
		add_options:function(data,sel_val)
		{
			for (var i=0; i<data.length; i++)
				this.add_option(data[i][1],data[i][0]==sel_val,data[i][0]);
		},
		add_option: function(display_value,selected,value)
		{
			if (selected==null) selected=false;
	 		var $o = $$el($c,'option').append(display_value);
			if (selected)
				$o.attr('selected','selected');
			if(value)
				$o.attr('value',value);
			return $o;
		},
		empty: function()
		{
			$c.children().remove();
		},
		get_selected_index: function()
		{
			return $c[0].options.selectedIndex;
		},
		get_selected: function()
		{
			return $($c.children()[$c.get_selected_index()]);
		},
		get_selected_display_value: function()
		{
			return $c.get_selected().text();
		},
		option: function(idx)
		{
			return $($c.children()[idx]);
		}
	});
	$parent.append($c);
	return $c;
}

function $$br($parent)
{
	var $c = $('<br></br>');
	$parent.append($c);
	return $c;
}

function $$input($parent,size,name)
{
	var $c = $('<input type="text">');
	if (size!=null)
		$c.attr("size", size);
	if (name!=null)
		$c.attr("name",name);
	$c.addClass('widget-prop-editor');
	$c.extend(
	{
		set_value: function(s)
		{
			$c.val(s);
		},
		setValue: function(s)
		{
			$c.val(s);
		},
		get_value: function()
		{
			return $c.val();
		},
		getValue: function()
		{
			return $c.val();
		}
	});
	$parent.append($c);
	return $c;
}


function $$input2($parent,default_text,name,size,on_change,behavior,behavior_val)
{
	var $d = $("<div style='position:relative'></div>");
	$parent.append($d);
	var $c = $("<INPUT type='text' />");
	$c.attr('name', name);
	$c.attr('size', size);
	$c.addClass('widget-prop-editor');
	$d.append($c);

	var $e = $("<div style='position:absolute; top: 6px; left: 7px;'>"+default_text+"</div>");
	$e.click(function(){ $c.focus(); });
	$d.append($e);

	var on_update = function()
	{
		var v = $c.val();
		if (v=='')
			$e.show();
		else
		{
			$e.hide();
			if(behavior && behavior == 'force_prepend' && v.indexOf(behavior_val) != 0)
			{
				$c.setValue(behavior_val+$c.getValue());
			}
		}
		if (on_change!=null)
			on_change(v);
	};
	$c.focus(function(){ $e.hide(); });
	$c.blur(on_update);
	$c.change(on_update);
	$c.keyup(on_update);
	$c.extend(
	{
		setValue: function(s)
		{
			$c.val(s);
			if (s=='' || s==null)
				$e.show();
			else
				$e.hide();
		},
		getValue: function()
		{
			return $c.val();
		}
	});
	return $c;//shouldnt it be $d?
}

function $$textarea($parent,name,rows,cols)
{
	var $c = $('<textarea></textarea>');
	$c.addClass('widget-prop-editor');
	if (rows!=null)
		$c.attr("rows",rows);
	if (cols!=null)
		$c.attr("cols",cols);
	if (name!=null)
		$c.attr("name",name);
	$c.extend(
	{
		setValue: function(s)
		{
			$c.val(s);
			$c.update();
		},
		getValue: function()
		{
			if ($c.val()==$c.data('message'))
				return '';
			return $c.val();
		},
		setMessage: function(m)
		{
			$c.data('message',m);
			$c.update();
		},
		update: function()
		{
			if ($c.val()=='')
				$c.val($c.data('message'));
		}
	});
	$c.change(function(){
		$c.update();
	}).focus(function(){
		if ($c.val()==$c.data('message'))
			$c.val('');
	}).blur(function(){
		$c.update();
	}).data('val', function(){
		return $c.getValue();
	})
	
	$parent.append($c);
	return $c;
}


function $$checkbox($parent,name,text,value)
{
	var $c = $('<div class="sw-checkbox"><div class="sw-checkbox-text">'+text+'</div> </div>');
	var $i = $('<input type="checkbox">')
	$c.extend(
	{	
		setValue: function(b)
		{
			$i.prop("checked", b);
			$i.change();
			update_ui(b);
		},
		getValue: function()
		{
			return $i.prop('checked');
		},
		change: function(f)
		{
			$i.change(function(){ f($i.prop('checked')) });
		}
	});
	$parent.append($c);
	var $si = $('<div class="sw-checkbox-input"></div>');
	var update_ui = function(b)
	{
		if (b)
			$si.addClass('checked');
		else
			$si.removeClass('checked');
	}
	$si.click(function()
	{ 
		var b = !$c.getValue();
		$c.setValue(b);
	});
	$c.append($i);
	$c.append($si);
	$i.hide();
	$c.setValue(value);
	$c.addClass('widget-prop-editor');
	$c.data('val',function(){ return $c.getValue(); });
	$c.attr('name',name);
	return $c;
}



function $$date($parent,name)
{
	var $c = $('<input type="text" />');
	$c.datepicker();
	if (name!=null)
		$c.attr('name',name);
	$c.extend(
	{
		val:function()
		{
			return $c.getValue();
		},
		getValue: function()
		{
			var d = $c.datepicker("getDate");
			if (d==null)
				return null;
			return d.getTime();
		},
		setValue: function(d)
		{
			return $c.datepicker("setDate", new Date(d));
		}
	});
	$parent.append($c);
	$c.addClass('widget-prop-editor');
	$c.data('val',function(){ return $c.getValue(); });
	$c.attr('name',name);
	return $c;
}



function $$error_flag($p, $a, msg)
{
	var $d = $$div($p, 'field-error');
	$d.text(msg);
	
	//var o = $a.offset();
	//o = $.globalToLocal($p, o.left, o.top);
	//$d.css({position:'absolute', top: o.y+'px', left: o.x+'px'});
}


function $$colorpicker($parent,name,values,selected,on_change)
{
	var value = selected;
	var cst;
	
	var $d = $("<div class='colorpicker'></div>");
	$parent.append($d);
	
	var $i = $("<INPUT type='hidden' name='"+name+"'/>")
	$parent.append($i);
	
	var $bg = $("<div class='display-value-bg'></div>");	
	var $v = $("<div class='display-value'></div>");
	var $fg = $("<div class='display-value-fg'></div>");
	$d.append($bg,$v,$fg);
	
	var update = function()
	{
		$i.val(value);
		$v.css({'background-color':value});
	};
	var on_update = function()
	{
			if (on_change!=null)
				on_change(value);
	};
	var show_choices = function()
	{
		var $cs = $("<div class='colorpicker-choices'></div>");
		
		var make_choice = function(val)
		{
			var $c = $("<div class='choice'></div>");
			$c.css({'background-color':val});
			if (val == selected)
				$c.addClass('selected');
			$c.click(function(){
				value = val;
				update();
				on_update();
				$$tooltip_hide();
			});
			return $c;
		};
		for (var i=0; i<values.length; i++)
			$cs.append(make_choice(values[i]));
		$$tooltip($d,$cs);
		
		$('#tooltip').hover(function() {
			clearTimeout(cst);
		}, function() {
			$$tooltip_hide();
		});
		
	};
	$d.click(function(){ 
		show_choices();
		cst=setTimeout("$$tooltip_hide()",3000);
	});
	$d.extend(
	{
		setValue: function(s)
		{
			// could test against 'values' here
			value = s;
			update();
		},
		getValue: function()
		{
			return value;
		}
	});
	update();
	return $d;
}


function $$clear_both()
{
	var $c = $("<br style='clear_both'/>");
	return $c;
}
	
function $$image($parent,res,props)
{
	function fit_to_size(model,cues,fit_w,fit_h,orig_w,orig_h)
	{
		var sx = fit_w/orig_w;
		var sy = fit_h/orig_h;
		var sw, sh, ot, ol;

		if(model == "fit")
		{
			if ( sx < sy )
				model = "fit_width";
			else
				model = "fit_height";
		}

		switch (model)
		{

			case "fit_height":
				sw = orig_w*sy;
				sh = orig_h*sy;
				break;
			case "fit_width":
				sw = orig_w*sx;
				sh = orig_h*sx;
				break;
			case "full_bleed":
				if ( sx > sy )
				{
					sw = orig_w*sx;
					sh = orig_h*sx;
				}
				else
				{
					sw = orig_w*sy;
					sh = orig_h*sy;
				}
				break;
		}
		if(cues)
		{
			switch(cues.align)
			{
				case 'left':
					ol = 0;
					break;
				case 'center':
					ol = (fit_w - sw)/2;
					break;
				case 'right':
					ol = fit_w - sw;
					break;
				default:
					ol = 0;
					break;
			}
			switch(cues.valign)
			{
				case 'top':
					ot = 0;
					break;
				case 'center':
					ot = (fit_h - sh)/2;
					break;
				case 'bottom':
					ot = fit_h - sh;
					break;
				default:
					ot = 0;
					break;
			}
			return {top:Math.floor(ot), left:Math.floor(ol), width:Math.floor(sw),height:Math.floor(sh)}
		}
		else
			return {width:Math.floor(sw),height:Math.floor(sh)}
	}
	var self = this;
	var component_w 		 = props.width;
	var component_h 		 = props.height;
	var scale				 = props.scale || "fit";
	var scale_cues			 = props.scale_cues || { align:'center', valign: 'center'};
	var wrap_classname		 = props.wrap_classname || "ps_image_wrap";
	var loading_classname	 = props.loading_classname || "ps_image_loading";
	var img_loaded_callback	 = props.onload;

	var $wrap = $$div($parent,wrap_classname).css({'overflow':'hidden','position':'relative','width':component_w+'px','height':component_h+'px'});
	var $wrap_loading = $$div($wrap, loading_classname).css({});
	var $img 	= $(new Image());
	$img.css({opacity:0});
	$wrap.append($img);
	var c 	 	= 0;
	$img.load(function()
	{
		var fit_info	= fit_to_size(scale,scale_cues,component_w,component_h,$img[0].width,$img[0].height);
		if(img_loaded_callback)
			img_loaded_callback(this,fit_info.width,fit_info.height);
		$wrap.css({'width':component_w+"px",'height':component_h, 'overflow':'hidden'});
		$img.css({'position':'absolute','top':fit_info.top+'px','left':fit_info.left+'px','width':fit_info.width+'px','height':fit_info.height+'px'});
		$img.fadeTo(333, 1);
		$wrap_loading.remove();
		$wrap_loading = null;
	});
	$img.error(function()
	{
		if (c!=0)
			throw new Error("cannot get preview for resource!")
		c++;
//		com_pagesociety_web_ResourceUtil.getPreviewUrl(res, preview_width, preview_height, function(p) { $img.attr('src', p) })
	});
	$img.attr('src', res);
	return $wrap;
}
	

function make_dropdown_menu($trigger, $dd, etype, speed, fadespeed)
{
	if (etype==null)
		etype = "click";
	if (speed==null)
		speed = 1000;
	if (fadespeed==null)
		fadespeed = 300;
	$trigger
		.bind(etype, showDD)
		.mouseover(cancelHideDD)
		.mouseout(hideDD);
	$dd
		.mouseover(cancelHideDD);
	function showDD() 
	{
		$dd.stop().fadeIn(fadespeed);
		$trigger.addClass('selected');
	}
	var ddid = -1;
	function hideDD() 
	{
		ddid = setTimeout(function(){
				$dd.stop().fadeOut(fadespeed);
				$trigger.removeClass('selected');
		}, speed);
	}
	function cancelHideDD()
	{
		clearTimeout(ddid);
		ddid = -1;
	}
	var ctx = {
	  hide: hideDD,
	  show: showDD
	};
	return ctx;
}




/*	tooltips */

// v1

var $$_tooltip = null;
var $$_tooltipc = null;
var $$_tooltipp = null;
function $$tooltip($o, $c)
{
	if ($$_tooltip==null)
	{
		$$_tooltip = $("<div id='tooltip'></div>");
		$$_tooltipc = $("<div class='content'></div>");
		$$_tooltipp = $("<div class='pointer'></div>");
		$$_tooltip.append($$_tooltipc,$$_tooltipp);
		$(document.body).append($$_tooltip);
	}
	$$_tooltipc.empty();
	$$_tooltipc.append($c);
	var o = $o.offset();
	var h = $$_tooltip.height() + $$_tooltipp.height();
	var w = $$_tooltip.width() - $$_tooltipp.width();
	$$_tooltipp.css({left:w * .5+2})
	$$_tooltip.css({top:(o.top - h )+'px',left:(o.left - w * .5)+'px'});
	$$_tooltip.show();
}

function $$tooltip_hide()
{
	if ($$_tooltip!=null)
		$$_tooltip.hide();
}



//v2

var $$_graph_tooltip;
function graph_tooltip_show(p,text)
{
	if ($$_graph_tooltip == null)
		$$_graph_tooltip = s_tooltip_init('graph_tooltip');
	$$_graph_tooltip.show(text);
}
function graph_tooltip_hide()
{
	$$_graph_tooltip.hide();
}

var $$_help_tooltip;
function help_tooltip_show(text)
{
	if ($$_help_tooltip == null)
		$$_help_tooltip = s_tooltip_init('help_tooltip', {left_offset: -5});
	$$_help_tooltip.show(text);
}
function help_tooltip_hide(s)
{
	$$_help_tooltip.hide(s);
}

function s_tooltip_init(classname, options)
{
	options     	= jQuery.extend({},options);
	var $ctx 		= { id: -1 };
	$ctx.root 		= $$div($(document.body), classname);
	$ctx.p 			= $$div($ctx.root, 'content');
	$ctx.pointer 	= $$div($ctx.root, 'pointer');
	var margin =  18;//option?
	var hoffset =  10;
	var left_offset = options.left_offset ? options.left_offset : 0;
	
	var s_tooltip_move = function(e)
	{
		var ww = $(window).width();
		var wh = $(window).height();
		var w = $ctx.p.width();
		var h = $ctx.p.height();
		var w0 = $ctx.pointer.width();
		var h0 = $ctx.pointer.height();
		var t = e.pageY - (h + h0 + margin + hoffset); 
		var above = true;
		if (t < 0)
		{
			t = e.pageY + hoffset; 
			above = false;	
		}
		var l = e.pageX - w * .5 ;
		if (l + w + margin > ww)
			l = ww - (w + margin) ;
		if (l<margin) 
			l = margin;
		$ctx.root.css(
		{
			'top': (t+5) + 'px',
			'left': (l - w0*.5 - left_offset)  + 'px'
		});
		$ctx.p.css(
		{
			'top': (above ? 0 : h0)
		});
		var pl = Math.min(w - 6, Math.max(0, e.pageX - l)) + left_offset;
		var pt = above ? h + hoffset + 10 : 0;
		$ctx.pointer.css(
		{ 
			'top': pt + 'px',
			'left': pl + 'px' 
		});
		if (above)
			$ctx.pointer.removeClass('down');
		else
			$ctx.pointer.addClass('down');
		
	}
	$ctx.hide = function(s)
	{
		if (s==0)
		{
			$ctx.id = -1;
			$ctx.moving = false;
			$(document).unbind('mousemove',s_tooltip_move);
			$ctx.root.stop().fadeOut(1);
			return;
		}
		if ($ctx.id==-1)
			$ctx.id = setTimeout(function(){
				$ctx.id = -1;
				$ctx.moving = false;
				$(document).unbind('mousemove',s_tooltip_move);
				$ctx.root.stop().fadeOut(150);
			},	100);
	}
	$ctx.show = function(text)
	{
		if ($ctx.id != -1)
		{
			clearTimeout($ctx.id);
			$ctx.id = -1;
		}
		$ctx.p.html(text);
		if ($ctx.moving)
			return;
		$ctx.moving = true;
		$(document).bind('mousemove',s_tooltip_move);
		$ctx.root.css('z-index', '9999999999999').stop().fadeTo(300,1);
	}
	return $ctx;
}




/**
 * CellList
 * requires param cell_renderer a function that returns a $(jQuery) object
 *
 */

$.widget( "sparkwise.clist",
{

	options: {
		 cell_renderer: null,
		 addable: true,
		 deletable: true,
		 reorderable: false,
		 selectable: false,
		 carousel: false,
		 carousel_width: -1,
		 carousel_left: null,
		 carousel_right: null,
		 on_select:null
	 },

	_create: function()
	{
		var self = this, el = $(this.element);
		self._value = [];
		el.css({'position':'relative'});
		self._$children = $("<div></div>").addClass('clist-children-wrapper');
		el.empty();
		el.append(self._$children);
		if (self.options.addable)
		{
			self._$add = $('<div class="dashboard-tab rounded-corners-top dashboard-add-button" '+
				'style="position: relative;"><span class="click_area">Add</span></div>');
			self._$add.click(function(){ self._trigger( "add" ); });
			el.append(self._$add);
		}
		if (self.options.carousel)
		{
			self._$children.carousel(
			{
				width: self.options.carousel_width,
				left_class: self.options.carousel_left,
				right_class: self.options.carousel_right
			});
			self._$children.css({'height':'100%', 'float':'left'});
		}
	},

	_destroy: function()
	{
		var self = this, el = this.element;
		el.empty()
	},

	value: function(t)
	{
		var self = this;
		if (t==null)
			return self._value;
		if (self.options.cell_renderer==null)
			throw new Error("error: set cell_renderer before calling set value")
		self._value = t;
		if (!self.options.carousel)
			self._$children.empty();
		for (var i=0; i<self._value.length; i++)
			self._add_cell( i, self._value[i] );
	},

	add_value: function(t) //TODO? at index
	{
		var self = this;
		if (self.options.cell_renderer==null)
			throw new Error("error: set cell_renderer before calling set value")
		self._value.push(t);
		self._add_cell(self._value.length-1, t);
		self.select(self._value.length-1);
	},

	remove: function(index)
	{
		var self = this;
		var $c = $(self.get_child(index));
		$c.remove();
		self._value.splice(index,1);
		var cc = self._$children.children();
		for(var i = 0;i < cc.length;i++)
		{
			$(cc[i]).data('index',i);
		}
		
	},
	
	select: function(index)
	{
		var self = this;
		var $c = $(self.get_child(index));
		self._select_cell($c,index);
	},

	get_child: function(index)
	{
		var self = this;
		if (self.options.carousel)
			return self._$children.carousel('get_child',index);
		else
			return self._$children.children()[index];
	},
	
	get_value: function(idx)
	{
		var self = this;
		return self._value[idx];
	},
	
	set_value: function(idx,value)
	{
		var self = this;
		self._value[idx] = value;
		self.update();
	},

	update: function()
	{
		var self = this;
		if(self.options.carousel)
			self._$children.carousel('do_empty');
		else
			self._$children.empty();
		for (var i=0; i<self._value.length; i++)
		{
			self._add_cell( i, self._value[i] );
		}
	},

	selection_index: function(t)
	{
		var self = this;
		if (t!=null)
			self.select(t);
		else
			return self.selected_index;
	},
	
	_add_cell:function(i, v)
	{
		var self = this;
		var $cell = self.options.cell_renderer(v);
		$cell.css('position','relative')
		$cell.data({index:i});
		// selectable cells trigger a select event, selected cells have cell-selected class
		if (self.options.selectable)
		{
			$cell.click(function()
			{
				self._select_cell($cell, i);
				self._trigger( "select", 0, { index: i, value: self._value[i] });
			});
		}
		// deletable cells trigger a delete event, a delete button is appended
		if (self.options.deletable)
		{
			var $close = $("<div>Remove</div>")
					.addClass('ui-icon ui-icon-close')
					.css({'position':'absolute','right': '5px', 'top': 0});
			$close.click(function(){	self._trigger( "deletef",	0, { index: $cell.data().index } ); });
			$cell.append($close);
		}
		// use the carousel method if its that
		if (self.options.carousel)
			self._$children.carousel('add_value',$cell);
		else
			self._$children.append($cell);
	},

	_select_cell: function($cell,idx)
	{

		var self = this;
		var do_swoosh = (idx != self.selected_index);
		self.selected_index = idx;
		if (self._$last_selected_cell!=null)
			self._$last_selected_cell.removeClass('cell-selected');
		$cell.addClass('cell-selected');
		if(do_swoosh)
			self._$children.carousel('set_offset', idx);
		self._$last_selected_cell = $cell;
		if(self.options.on_select)
			self.options.on_select(idx,$cell);
	}
});





/**
 * RTE
 */
	
	
//add name and invisible text area that keeps syncd for jquery serialize array compatibility!!!!!1
function $$rte($parent, name, options) {
	

	var defaults = 
	{
		media_url: "",
		content_css_url: "/static/css/rte.css",
		dot_net_button_class: null,
		max_height: 350
	};

	var opts = jQuery.extend(defaults, options);
	var iframe = document.createElement("iframe");
	iframe.frameBorder = 0;
	iframe.frameMargin = 0;
	iframe.framePadding = 0;
	iframe.height = '200px';
	iframe.width = '100%';
		
	function tryEnableDesignMode(doc, callback) 
	{
		try 
		{
			iframe.contentWindow.document.open();
			iframe.contentWindow.document.write(doc);
			iframe.contentWindow.document.close();
		} catch(error) {
			//console.log(error);
		}
		if (document.contentEditable) 
		{
			iframe.contentWindow.document.designMode = "On";
			callback();
			return true;
		}
		else if (document.designMode != null) 
		{
			try 
			{
				iframe.contentWindow.document.designMode = "on";
				callback();
				return true;
			} catch (error) {
					//console.log(error);
			}
		}
		setTimeout(function(){tryEnableDesignMode(doc, callback)}, 500);
		return false;
	}

	// create toolbar and bind events to it's elements
	function init_ui() 
	{
		var tb = $("<div class='rte-toolbar'><div>\
			<p>\
				<div class='bold'></div>\
				<div class='italic'></div>\
				<div class='link'></div>\
			</p>\
			<p>\
				<select class='size'>\
				<option value='h4'>12</option>\
				<option value='h3'>14</option>\
				<option value='h2'>18</option>\
				<option value='h1'>24</option>\
				</select>\
			</p>\
			<p>\
				<a href='#' class='justifyleft'></a>\
				<a href='#' class='justifycenter'></a>\
				<a href='#' class='justifyright'></a>\
			</p></div></div>");		

		$('.bold', tb).click(function(){ formatText('bold');return false; });
		$('.italic', tb).click(function(){ formatText('italic');return false; });
		$('.link', tb).click(function(){
			var p=prompt("URL:");//TODO use sparkwise dialog
			if(p)
				formatText('CreateLink', p);
			return false; 
		});

		$('select', tb).change(function()
		{
			var index = this.selectedIndex;
			var selected = this.options[index].value;
			formatText("formatblock", '<'+selected+'>');
		});
		
		$('.justifyleft', tb).click(function(){ formatText('justifyleft');return false; });
		$('.justifycenter', tb).click(function(){ formatText('justifycenter');return false; });
		$('.justifyright', tb).click(function(){ formatText('justifyright');return false; });

		var iframeDoc = $(iframe.contentWindow.document);

		var select = $('select', tb)[0];
		iframeDoc.mouseup(function()
		{
			setSelectedType(getSelectionElement(), select);
			return true;
		});

		return tb;
	};

	function formatText(command, option) 
	{
		iframe.contentWindow.focus();
		try
		{
			iframe.contentWindow.document.execCommand(command, false, option);
		}catch(e){
			//console.log(e)
		}
		iframe.contentWindow.focus();
	};

	function setSelectedType(node, select) 
	{
		while(node.parentNode) 
		{
			var nName = node.nodeName.toLowerCase();
			for(var i=0;i<select.options.length;i++) 
			{
				if(nName == select.options[i].value)
				{
					select.selectedIndex=i;
					return true;
				}
			}
			node = node.parentNode;
		}
		select.selectedIndex = 0;
		return true;
	};

	function getSelectionElement() 
	{
		var selection,range,node;
		if (iframe.contentWindow.document.selection)// IE selections
		{
			selection = iframe.contentWindow.document.selection;
			range = selection.createRange();
			try 
			{
					node = range.parentElement();
			}
			catch (e) 
			{
					return false;
			}
		} 
		else // Mozilla selections
		{
			
			try 
			{
				selection = iframe.contentWindow.getSelection();
				range = selection.getRangeAt(0);
			}
			catch(e)
			{
					return false;
			}
			node = range.commonAncestorContainer;
		}
		return node;
	};
	
	function val(v)
	{ 
		if (v==null)
		{
			return $(iframe).contents().find("body").html();
		}
		else
		{
			// Mozilla needs this to display caret
			if(jQuery.trim(v)=='') 
				v = '<br />';
			$(iframe).contents().find("body").html(v);
		}
	}
	
	// init
	var $iframe = $(iframe);
	$iframe.addClass('widget-prop-editor');
	$iframe.attr('name',name);
	$parent.append($iframe);

	var css = "";
	if(opts.content_css_url) 
	{
		css = "<link type='text/css' rel='stylesheet' href='" + opts.content_css_url + "' />";
	}
	var doc = "<html><head>"+css+"</head><body class='frameBody'></body></html>";
	tryEnableDesignMode(doc, function() 
	{
		$parent.prepend(init_ui());
	});
	
	jQuery.extend($iframe,{
		val: val,
		getValue: val,
		setValue: val
	});
	$iframe.data('val',val);

	return $iframe;//$
		
}; 

	
	
/** UPLOADER */

var s3basepath = 'http://sparkwise.s3.amazonaws.com/';
function $$upload($parent,name)
{
	var $d = $('<form></form>');
	$d.attr('enctype', 'multipart/form-data');
	$d.attr('method', 'POST');
	$d.attr('target', 'hidden_iframe_for_upload');
	$d.attr('type', 'file');
	$d.attr('name', 'form-'+name);
	$d.addClass('widget-prop-editor');
	var $c = $('<input/>');
	$c.attr('type', 'file');
	if (name != null)
		$c.attr('name', 'file-'+name);
	$d.append($c);
	var $e = null, $f = null;
	$d.append('<input type="hidden" name="channel" value="A" />')
	$parent.append($d);
	var mixins = 
	{
		getName: function()
		{
			return name;
		},
		setValue: function(s)
		{
			if (s == null || s.indexOf(';')==-1)
				return;
			// remove the upload fields and set display
			if ($e!=null)
				$e.remove();
			if ($f!=null)
				$f.remove();
			var p = s.split(';');
			var id = p[0].substring(3);
			var path = p[1].substring(5);
			$e = $('<a href="' + s3basepath + path + '">'+path+'</a>');
			$f = $('<input type="hidden" name="' + name + '" value="' + s + '"/>');
				$f.addClass('widget-prop-editor');
			$parent.append($e,$f);
			$d.hide();
		},
		getValue: function()
		{
			if ($f==null)
				return null;
			return $f.val();
		},
		hasFile: function()
		{
			return $f != null || ($c[0].files != null && $c[0].files[0] != null);
		},
		getFile: function()
		{
			return $c[0].files[0];
		},
		upload: function(on_complete)
		{
			if (!$d.hasFile())
				return;
			if ($f != null)
			{
				on_complete();
				return;
			}
			$d.attr('action', '/Resource/CreateResource/.form?channel=A&size='+$d.getFile().size);
			$d[0].submit();
			$d.hide();
			var $up = $('<div>Upload in progress..</div>');
			$parent.append($up);
			var update_id = -1;
			var update_ui = function()
			{
				do_module('Resource/GetUploadProgress',['A'], function(info)
				{
					if (info == null)
						return;
					var p = Math.floor(info.progress);
					$up.text(p+"% complete...");
					if (info.completionObject != null)
					{
						clearInterval(update_id);
						var res = info.completionObject;
						var id = res._id;
						var path = res._attributes['path-token'];
						$d.setValue('id='+id+';path='+path);
						on_complete();
					}
				});
			}				
			update_id = setInterval(update_ui,500);
		}
	};
	$d.extend(mixins);
	$d.data('editor',mixins);
	$d.data('type','file');
	return $d;
}




/* NEW dialog */

var $$_dialog_container = null;
function $$dialog(title,message,$xtra,button_info,options)
{
	options = jQuery.extend({},options);

	if ($$_dialog_container==null)
		$$_dialog_container = { root: $$div($(document.body), 'sw-dialog-container'), dialogs: [] };

	var $$_dialog = { root: $$div($$_dialog_container.root, 'sw-dialog') };
	$$_dialog_container.dialogs.push($$_dialog);
	
	$$_dialog.bg = $$div($$_dialog.root, 'bg');
	$$_dialog.window = $$div($$_dialog.root, 'window').addClass(options.classname);
	$$_dialog.close = $$div($$_dialog.window, 'close');
	$$_dialog.title = $$div($$_dialog.window, 'title');
	if (options.$header)
	{
		$$_dialog.$header = $$div($$_dialog.window, 'header');
		$$_dialog.$header.append(options.$header);
	}
	$$_dialog.content = $$div($$_dialog.window, 'content');
	$$_dialog.content_text = $$div($$_dialog.content, 'content-text');
	$$_dialog.content_end = $$div($$_dialog.content, 'content-end');
	$$_dialog.buttons = $$div($$_dialog.window, 'buttonset');
	
	var position = function()
	{
		var w = $$_dialog.window.width();
		var t = ( $(window).height() - $$_dialog.window.height() ) *.5;
		var l = ( $(window).width() - w ) *.5;
//		$$_dialog.window.css({'top':t+'px','left':l+'px'});
		$$_dialog.window.css({'top':t+'px','left':l+'px'});
		$$_dialog.close.css({'top':-15+'px','left':(w-20)+'px'});
	}
	var destroy = function()
	{
		$$dialog_close();
	}
	var set_buttons = function(buttons)
	{
		$$_dialog.buttons.empty();
		for (var i=0; i<buttons.length; i++)
		{
			(function(b){
				if (b.name==null)
					b.name = "no name";
				if (b.destroy==null)
					b.destroy = true;
				var $b = $('<button>'+b.name+'</button>');
				if (b.class_name)
					$b.addClass(b.class_name);
				if (b.click)
					$b.click(function()
					{ 
						if (b.destroy)
							destroy(); 
						b.click() 
					});
				else
					$b.click(destroy);
				$$_dialog.buttons.append($b);
			})(buttons[i]);
		}		
	}
	
	if (options.width)
		$$_dialog.window.width(options.width);
	$$_dialog.title.html('<p>'+title+'</p>');
	$$_dialog.content_text.append(message);
	if ($xtra!=null)
	{
		$$_dialog.content_text.append($xtra);
		$xtra.show();	
	}
	button_info = button_info ? button_info : [];
	set_buttons(button_info);
	$$_dialog.setButtons = function(b)
	{ 
		button_info = b;
		set_buttons(b)
	}
	
	//
	$$_dialog.bg.click(destroy);
	$$_dialog.close.click(destroy);
	$$_dialog.position = position;
	
//	disable_scroll_wheel();//could do overflow hidden, but then the page shifts... TODO discuss w/ carl
	$(window).bind('resize',$$_dialog.position);
	position();
	return $$_dialog;
}

function $$dialog_close()
{
	var $$_dialog = $$_dialog_container.dialogs.pop();
	$(window).unbind('resize',$$_dialog.position);
	$$_dialog.root.remove();
	$$_dialog = null;
//	enable_scroll_wheel();
}

function $$dialog_ok(title,msg,ok,cancel,okf,$xtra)
{
	$$dialog(title,msg,$xtra,[{name: ok, click: okf},{name: cancel}]);
}

function $$dialog_input(title,msg,ok,cancel,okf)
{
	var $xtra = $$div(null,'dialog-input-wrap');
	var $input = $$input($xtra);
	$$dialog(title,msg,$xtra,[{name: ok, click: function(){ okf($input.getValue()); }},{ name: cancel }]);
}
