
/**
 * display grid
 */
var g_events_list;
$_widget_embed_init = function(widget_uuid)
{
	$(document.body).css({'background':'transparent','padding':'3px'});

	do_module('Dashboard/GetWidgetEmbed', [widget_uuid], function(data)
	{
		var widget_instance = data.widget;
		var widget_type = data.collection.name;
		g_events_list = data.events;
		
	    var cell_size  = 230;
	    var cell_pad  = 10;
	    var padded_cell  = cell_size + cell_pad;
		var wi_w = widget_instance.rect[2];
		var wi_h = widget_instance.rect[3];
		var w = padded_cell * wi_w - cell_pad;
		var h = padded_cell * wi_h - cell_pad;
 
	    var $cell = new se.sparkwi.dashboard.cell({
			is_editable: false,
			width:w,
			height:h,
			viz_render_callback: function(){
				var $icon = $cell.element.find(".widget-icons");
				$icon.removeClass('powered-by');
				$icon.addClass('powered-by');
				$icon.mouseenter(function(){
					graph_tooltip_show(null,"Powered by Sparkwise")
				}).mouseleave(function(){
					graph_tooltip_hide();
				}).click(function(){
					location.href = 'http://sparkwi.se';
				});
			}
		});
		$cell.element.addClass(widget_type.toLowerCase());
	    $("#widget-wrapper").append($cell.element);
	    	
		$cell.css({'postion':'relative','width':w+'px','height':h+'px'})
	 	$cell.attr('id', widget_type);
		$cell.data(widget_instance);
		
	},
	function(error){
		$("#widget-wrapper").html(error.message);
	});


}



