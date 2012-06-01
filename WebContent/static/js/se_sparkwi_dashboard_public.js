
/**
 * display grid
 */

var g_events_list;
var g_board;
$_dashboard_public_init = function(dashboard_uuid)
{
	do_module('Dashboard/GetPublishedDashboard', [dashboard_uuid], function(dshbrd_data)
    {
		g_board = dshbrd_data.dashboard;
		var username = dshbrd_data.username;
		var wi   = dshbrd_data.widget_instances;
		g_events_list = dshbrd_data.events;
		se.sparkwi.widget.collection = dshbrd_data.collection;
		$('#grid').dashboard_public({
    		dashboard: g_board,
      		widgets:wi
    	});
    	
    	$('#published-info-boardname').text(g_board.public_name);
    	$('#published-info-username').text(g_board.public_org);
    	$('#published-info-username').text(g_board.public_org);
    	$('#published-info-boardname-edit').click(edit_info);
    });
	
	
	// from se_sparkwi_dashboard_tabs
	function edit_info()
	{
		var $xtra = get_pub_extra();
	    $$dialog_ok('Update Details','Edit details about your public board.','Submit','Cancel',
	   	      function() 
	   	      {
	   	    	 do_module('Dashboard/PublishDashboard', [g_board.id, $xtra.$dbname.getValue(), $xtra.$orgname.getValue() ], 
	   	    	 function(data)
	   	    	 {
	   	    		g_board = data;
	   		    	$('#published-info-boardname').text(data.public_name);
	   		    	$('#published-info-username').text(data.public_org);
	   	    	 });
	   	      }, $xtra);
	}
	
	function get_pub_extra()
	{
	    var $xtra = $$div(null,'dialog-extra');
	    
	    var $line = $$div($xtra,'dialog-line');
	    $$label($line,'Organization','dialog-label');
	    var $orgname = $$input($line,'dialog-input');
	    
	    var $line2 = $$div($xtra,'dialog-line');
	    $$label($line2,'Board Title','dialog-label');
	    var $dbname = $$input($line2,'dialog-input');
	    
	    $dbname.setValue(g_board.public_name ? g_board.public_name : g_board.name);
	    $orgname.setValue(g_board.public_org);
	    
	    jQuery.extend($xtra, { $dbname: $dbname, $orgname: $orgname });
	    return $xtra;
	}

	
	
}



$.widget( "sparkwise.dashboard_public",
{

  options: { dashboard: null, widgets: [] },
  _create: function()
  {
    var self = this, el = this.element;

    self.cell_size  = 230;
    self.cell_pad   = 10;

	var wis = self.options.widgets;
    self._$children = $("<div></div>");
    el.append(self._$children);

	for (var i=0; i<wis.length; i++)
	  {
	    var wi = wis[i]
	    if (wi==null)
	    	self._add_blank_cell(1,1);
	    else
	    {
	        var	$cell = self._add_cell(wi.proxy.widget, wi.rect[1], wi.rect[0], wi.rect[2], wi.rect[3]);
	    	$cell.data(wi);
	    }
	  }
  },
  
  _add_cell: function(widget,r,c,w,h)
  {
    var self = this, el = this.element;
    if (w==null) w=1;
    if (h==null) h=1;
    var p = self.cell_pad;
    var pc = self.cell_size + p;
    
    var $cell = new se.sparkwi.dashboard.cell({
	    is_editable: false,
	    width:(pc*w-p), 
	    height: (pc*h-p)
	  });
	$cell.dashboard(self);
  
    $cell.css({
      'width':  (pc*w-p)+'px',
      'height': (pc*h-p)+'px',
      'top':    (pc*r)+'px',
      'left':   (pc*c)+'px',
      'position': 'absolute'
    });

	self._$children.append($cell.element);
    return $cell;
  
  }

});

