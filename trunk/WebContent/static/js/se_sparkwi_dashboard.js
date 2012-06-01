var $_grid;
var $_connections;
var $_myset;
var g_host;


$_dashboard_init = function(host)
{
	var ok_browsers = [["Chrome",10],["Safari",5],["Firefox",4]];
	var ok_browser = false;
	for (var i=0; i<ok_browsers.length; i++)
	{
		var o = ok_browsers[i];
		if (BrowserDetect.browser == o[0] && BrowserDetect.version > o[1])
		{
			ok_browser = true;
			break;
		}
	}
	if (!ok_browser)
	{
		$$dialog("PLEASE UPDATE YOUR BROWSER",
				"We're sorry but the browser you're using is not fully supported by Sparkwise. "+
				"You may continue using your current browser but certain features may not work. "+
				"To get the most out of the tool, please upgrade to the latest version of Firefox or Chrome.",
				null,
				[{name:'Get Chrome', click: function(){ location.href = 'http://getfirefox.com'; }},
				 {name:'Get Firefox', click: function(){ location.href = 'https://www.google.com/chrome'; }}]);
		//does not return //allows dashboard to attempt to render
	}

	
	//ok
	g_host = host;
	$('#widget-tabs-wrapper, #widget-category-wrapper').disableTextSelect();
    $("#widget-category-bg").css('opacity',.5);//TODO use rgba in css instead

	$_init_global_functions(host, function(conn_data,myset_data)
	{
		$_connections.data = conn_data;

		//init- load data
		do_module("Dashboard/GetWidgetCollectionByName", ["Default"], function(r)
		{
			se.sparkwi.widget.collection = r;
		    $( "#widget-chooser" ).widget_selector({collection: r, myset: myset_data});
		    var $grid = new se.sparkwi.dashboard.controller($( "#grid" ),{ widget_draggables: $( ".widget-def" ) });
	
		    $_grid = $grid; // for global access (used by connections, events, se_sparkwi_dashboard_widget_selector)
	
		    //manage selected dashboard
		    var _sel_index = -1;
		    var _dshbrd_data = [];
		    var current_db = function()  { return _dshbrd_data[_sel_index]; };
			var update_props = function(d) {  };
	
		    // list dashboards, set up a tab for each as well as the grid w/ default dashbaord
		    do_module('Dashboard/ListDashboards', [], function(dshbrd_data)
		    {
		      _dshbrd_data = dshbrd_data;
		      var $dashboard_tabs = $('#dashboards').dashboard_tabs({
		          select: function(e,ui)
		          {
		        	_sel_index = ui.index;
		        	$(window).scrollTop(0);
		            $grid.load(current_db().id);
		            $.cookie('dashboard-tab-pref-idx',_sel_index);
		            update_props(current_db());
		          }
		      });
		      $dashboard_tabs.dashboard_tabs('value', _dshbrd_data);
		      if (_dshbrd_data.length!=0)
		      {
		        var tab_pref_idx = $.cookie('dashboard-tab-pref-idx');
		        if (tab_pref_idx == null || isNaN(tab_pref_idx)) tab_pref_idx = 0;
		        if (tab_pref_idx>_dshbrd_data.length-1) tab_pref_idx = 0;
		        //logging.log('selecting dashboard tab '+tab_pref_idx+' of '+_dshbrd_data.length)
		        _sel_index = tab_pref_idx;
		        $grid.load(current_db().id);
		        $dashboard_tabs.dashboard_tabs('select',_sel_index);
		        update_props(current_db());
		      }
	
		    });
		});
	});
};




$_init_global_functions = function(host, on_complete)
{

    /* get all connections for user */
    $_connections = {
      data: [],
      get_by_type: function(t)
      {
        var conns = [];
        for (var i=0; i<this.data.length; i++)
          if (this.data[i].type==t)
            conns.push(this.data[i]);
        return conns;
      },
      add_connection: function(conn)
      {
        for (var i=0; i<this.data.length; i++)
          if (this.data[i].id == conn.id)
          {
            this.data[i] = conn;
            return;
          }
        this.data.push(conn);
      },
      remove_connection: function(conn)
      {
        for (var i=0; i<this.data.length; i++)
        {
          if (this.data[i].id == conn.id)
          {
            this.data.splice(i,1);
            break;
          }
        }
      },
      refresh_connections:function()
      {
    	  do_module('Dashboard/ListConnections', null, function(c) {
    			$_connections.data = c;
    		});
      }
    };

	do_module('Dashboard/ListConnections', null, function(c) {
		do_module('Dashboard/GetMySet', null, function(m) {
			on_complete(c,m);
		});
	});
	

};






/** settings menu **/
	


function $_settings_menu_init()
{
	
    /*  bind the tool functions */
    var $dd = $('#settings-dd');
    make_dropdown_menu( $('#settings-menu'), $('#settings-dd') );
    
	$('#settings-menu-my-account').click(function() {
		$dd.stop().slideToggle(250);
		g_show_account();
	});
	
	$('#connections-button').click(function() {
		$dd.stop().slideToggle(250,function(e){});
		$('#connections-dialog').connections('open')
	});
	
	$('#settings-menu-events').click(function() {
		$dd.stop().slideToggle(250,function(e){});
		$('#events-dialog').events('open');
	});

}

