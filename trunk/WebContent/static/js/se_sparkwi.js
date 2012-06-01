/**
 * sparkwise app
 */
var se = 
{
	sparkwi: 
	{
		DISPLAY_ABSOLUTE_TOTAL: 0x10, 		
		DISPLAY_PER_DAY_TOTAL: 0x20,
		DISPLAY_PER_DAY_PERCENT: 0x30, 		
		DISPLAY_ABSOLUTE_PERCENT: 0x40, 		
		colors: 
		{
			'GoogleAnalytics': 0x98C538,
			'Google Analytics': 0x98C538,
			'Facebook': 0x37A6DE,
			'Twitter': 0x46CAB4,
			'YouTube': 0xFD5A57,
			'Vimeo': 0xFD5A57,
			'Video': 0xF75854,
			'Search': 0xFFC939,
			'Story': 0xFC8828,
			'Custom': 0xC55397,
			'Raw': 0xC55397,//0xDCAD2D,
			'GoogleSpreadsheets': 0xC55397,//0xE762B0,
			'Grey': 0xCCCCCC,
			'Compare': 0xCCCCCC,
			'event_color':"#800080"//????
		},
		DAY_IN_MS: 1000*60*60*24,
		HALF_DAY_IN_MS: 1000*60*60*12,
		MONTHS: ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'],
		
		

		component: 
		{
			raphael: {}
		},
		
		date:
		{
			
		},
		
		widget: 
		{
			//the master collection of widgets
			collection:null,
			
			// the main visualizations for each widget definition 
			// se_sparkwi_widget_viz.js 
			viz: 
			{
				// the viz utilities do most of the display work
				// se_sparkwi_widget_viz_util.js
				// se_sparkwi_widget_viz_util_graph.js
				// se_sparkwi_widget_viz_util_map.js
				// se_sparkwi_widget_viz_util_...
				util: {}
			},
			
			// to configure connections/selectors/props of each widget
			// se_sparkwi_config.js
			config: 
			{
				// the bulk of the config abstractions are defined in se_sparkwi_config_mixins.js
				// assumes that a class in config will spec the form - these methods are added 
				mixins: {},
				
				// the config help "system"
				help: {}
			},
			
			// stuff like: filter_nulls, add_commas, etc - defined in se_sparkwi_widget_util.js 
			// TODO refactor to se.sparkwi.util and move all utils there
			util: {}
		},
		
		//TODO refactor dashboard.js, etc to the following:
		dashboard:
		{
			
		},
		
		events:
		{
			
		},
		
		account:
		{
			
		},
		
		connections:
		{
			
		}
		
	}
};








/** global utils */
	

var logging = {
	info: function(){ try { console.log(arguments); } catch(e){} },
	log: function(){ try { console.log(arguments); } catch(e){} },
	error: function(){ try { console.log(arguments); } catch(e){} }
};

//TODO namespace auth related...
var authorization_callback = function(res){};
var authorization_err = function(res){};
function authorization_error(err)
{ 	
	$$dialog('ERROR',err,null,[{name:'OK'}]);
	logging.error(err);
};
function create_conn_for_wi(id,callback,err)
{
	if (callback!=null)
			authorization_callback = callback;
	if (err!=null)
			authorization_err = err;
	 window.open('Dashboard/DoAuthorization/.raw?wid='+id,'','width=1000,height=600');
}
function create_conn(type,callback,err)
{
	if (callback!=null)
			authorization_callback = callback;
	if (err!=null)
			authorization_err = err;
	 window.open('Dashboard/DoAuthorization/.raw?type='+type,'','width=1000,height=600');
}
function authorization_complete(response)
{
	if (authorization_callback!=null)
		authorization_callback(response);
}
function authorization_failed(response)
{
	if (authorization_err!=null)
		authorization_err(unescape(response));
}




/* more global utilities... */


$.globalToLocal = function( context, globalX, globalY ){
	 // Get the position of the context element.
	 var position = context.offset();
	 // Return the X/Y in the local context.
	 return({
	 x: Math.floor( globalX - position.left ),
	 y: Math.floor( globalY - position.top )
	 });
};


function do_module(m,a,f,e,async)
{
	var e_f = e || function(e)
	{ 
		if(e.message == "NO PERMISSION")
		{
			window.location.href = "/";
			return;
		}
		$$dialog('ERROR',e.message,null,[{name:'OK'}]);

		logging.error(e.message);
	}
	com_pagesociety_web_ModuleRequest.doModule(m,a,f,e_f,async);
}


function do_logout()
{
	do_module("User/Logout",[],function(e)
	{
		window.location.href = "signin";
	});
}


function disable_scroll_wheel()
{
	$(document.body).bind('mousewheel DOMMouseScroll', function(e) {
		e.preventDefault();
	});
}

function enable_scroll_wheel()
{
	$(document.body).unbind('mousewheel DOMMouseScroll');
}

