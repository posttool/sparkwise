
var $connections_dialog;

$_connections_init = function()
{
	$connections_dialog = $('#connections-dialog').connections();
	$connections_dialog.hide();
};

$_connection_auth_window = function(type, look_for_type)
{
	create_conn(type, $_connection_complete, $_connection_error);
};

$_connection_complete = function(c)
{
	$_connections.add_connection(c);
	$_grid.load();
	$connections_dialog.connections('list');
};

$_connection_error = function(c)
{
	$connections_dialog.connections('list');
	alert("The connection failed.");
};



/**
 *connections
 */


$.widget("sparkwise.connections",
{
	_create: function()
	{
		var self = this;
	},
	list: function(on_complete)
	{
		var self = this;
		var el = $(this.element);
		el.empty();
		el.html("<div class='connections-loading'>Connections loading...</div>");
		do_module('Dashboard/ListConnectionData', null, function(evt_data)
		{
			self.value(evt_data);
			if (self.$dialog)
				self.$dialog.position();
			if(on_complete != null)
				on_complete();
		});
	},
	open: function()
	{	
		var self = this;
		if (self.$root!=null)
		{
			self.$root.remove();
			self.$header.remove();	
		}
		self.$root = $$div();
		self.$header = $$div();
		self.$dialog = $$dialog('CONNECTIONS','',self.$root, 
			[
				{name: 'Add Account', destroy:false, click: function(){$_connection_auth_window(self.selected_type,true);}},
				{name: 'Update Password', destroy:false, click: function(){$_connection_auth_window(self.selected_type,true);}}
			],
			{width: 850, classname:'sw-connections-dialog', $header:self.$header });
		self.$dialog.content.css({'padding-top':'0','margin-top':'0'});
		self.$dialog.content_text.css({'padding-top':'0','margin-top':'0'});
		self.$dialog.content_end.css({'background':'none'});
		self.list();
	},

	value: function(v)
	{

		var self = this;
		if(v == null)
			return self.val;
		
		self.val = v;
		var el = self.$root;
		self.error_states = {};
		el.empty();
		
		var $c_content 			= $("<div class='connections-dialog-content'></div>");
		var $c_tab_titles_bar	= $('<div class="connections-tab-titles-bar connections-dialog-content">');
		self.$c_tabs			= $('<div class="connections-tabs"></div>')
		self.$c_tab_titles_bar  = $c_tab_titles_bar;
		var idx = -1;
		for(var type in self.val)
		{
			if (type=="Raw")
				continue;
			idx++;
			var $c_tab_title = $("<div class='connections-tab-title connections-status' id='connections-tab-"+type+"'></div>");	
			if (type=="GoogleAnalytics")
				$c_tab_title.text("Google Analytics");	
			else if (type=="GoogleSpreadsheets")
				$c_tab_title.text("Spreadsheets");	
			else 
				$c_tab_title.text(type);	
			
			$c_tab_title.click({idx:idx},function(e){self.select_tab(e.data.idx)});
			$c_tab_titles_bar.append($c_tab_title);
			
			var connections = self.val[type];
			var $tab = $("<table class='connections-tab'></table>");
			$tab.data('type',type);
			self.$c_tabs.append($tab);

			for(var i=0;i < connections.length;i++)
			{
				var conn = connections[i];
				var uname = conn.username;

				if (conn.type=="GoogleAnalytics")
					uname = uname.substring(uname.lastIndexOf(' '));
				
				var $connection_row = $("<tr class='row row-connection'></tr>");
				var $cstatus = $("<td class='data-status'></td>");
				var $cac = $("<td class='data-accounts-connections'>"+uname+"</td>");
				var $ccs = $("<td class='data-connected-since'>"+format_date(conn.date_created)+"</td>");
				var $clu = $("<td class='data-last-updated'></td>");
				var $crepair = $("<td class='data-repair'></td>");
				var $crefresh = $("<td class='data-refresh'></td>");
				var $cdelete = $("<td class='data-delete connections-button'><span>[o]</span></td>");
				
				$('span', $cdelete).click({id:conn.id,name:(conn.type+" "+uname)},function(ee)
				{
					$$dialog_ok('Confirm','Are you sure you want to delete '+ee.data.name+'?','OK','Cancel',
						function() 
						{
							do_module('Dashboard/DeleteConnection', [ee.data.id], function(e)
							{
								$_connections.remove_connection(e);
								$connections_dialog.connections('list');	
								$_grid.load();
							});
						});
				});
				
				$connection_row.append($cstatus);
				$connection_row.append($cac);
				$connection_row.append($ccs);
				$connection_row.append($clu);
				$connection_row.append($crefresh);
				$connection_row.append($cdelete);
				$tab.append($connection_row);
				
				var proxies = conn.children;
				for(var ii = 0; ii < proxies.length;ii++)
				{
					var proxy = proxies[ii];
					var pname= proxy.widget.name;
					var args = "";
					var sel_vals = proxy.selector_display_values;
					for(var p in sel_vals)
					{
						if (p.startsWith("_"))
							continue;
						var v = sel_vals[p];
						if(v && v.startsWith("www."))
							v = v.substring(4);
						args += v+" ";
					}
					if (args.length!=0)
					{
						args = args.substring(0,args.length-1);
						pname = pname + " for "+args;
					}
					
					var state = proxy.state;
					var connected = ( state == PROXY_STATE_OK);
					var repair= !connected;
					var $proxy_row = $("<tr class='row row-proxy'></tr>");
					var $pstatus = $("<td style='vertical-align: top;' class='data-status'>"+(connected?"Connected":"Broken")+"</td>");
					if(connected)
						$pstatus.addClass('connections-status connections-status-green');
					else
						$pstatus.addClass('connections-status connections-status-red');
					
					var $pname = $("<td class='data-accounts-connections'>"+pname+"</td>");
					var $pcs = $("<td class='data-connected-since'></td>");
					var $plupdate = $("<td class='data-last-updated'>"+(format_date_ago(proxy.last_updated)||"")+"</td>");
					var $prepair = $("<td class='data-repair "+(repair?"":" connections-button connections-button-inactive")+"'><span>[o]</span></td>");
					var $prefresh= $("<td class='data-refresh connections-button'><span>[o]</span></td>");
					var $pdelete = $("<td class='data-delete connections-button'><span>[o]</span></td>");
					
					$proxy_row.hover(function() {
						$(this).css('background', '#fff');
					}, function() {
						$(this).css('background', 'none');
					});
					
					if(repair)
					{
						self.error_states[type] = true;
						$('span', $prepair).click({id:proxy.id},function(e)
						{
								do_module('Dashboard/RepairProxy', [e.data.id], function(e)
							{
								$connections_dialog.connections('list');	
								$_grid.load();
							});
						});
					}

					
					$('span', $prefresh).click({id:proxy.id},function(e)
					{
						do_module('Dashboard/RefreshProxy', [e.data.id], function(e)
						{
							$connections_dialog.connections('list');	
							$_grid.load();
						});
					});

					$('span', $pdelete).click({id:proxy.id,name:pname},function(e)
					{
						$$dialog_ok('Confirm','Are you sure you want to delete '+e.data.name+'?','OK','Cancel',
						function() 
						{
							do_module('Dashboard/DeleteProxy', [e.data.id], function(e)
							{
								$connections_dialog.connections('list');	
								$_grid.load();
							});
						});
					});

					$proxy_row.append($pstatus);
					$proxy_row.append($pname);
					$proxy_row.append($pcs);
					$proxy_row.append($plupdate);
					$proxy_row.append($prefresh);
					$proxy_row.append($pdelete);
					$tab.append($proxy_row);
				}
			}
			
			if (connections.length <= 0) {
				var typ;
				if (type=="GoogleAnalytics")
					typ = "Google Analytics";	
				else if (type=="GoogleSpreadsheets")
					typ = "Spreadsheets";	
				else 
					typ = type;	
				
				var $no_connections_row = $("<tr class='connections-tab-add-account-row'></tr>");
				$tab.append($no_connections_row);
				var $no_connections_data = $("<td colspan='7' class='no-connections')><h4>NO CONNECTIONS</h4>You currently have no accounts set up with "+typ+".<br />Click the <strong>Add Account</strong> button below to make a new connection.</td>"); 	
				$no_connections_row.append($no_connections_data);			
			}
			
		}
		$c_tab_titles_bar.append($("<br style='clear:both;'/>"));
		
		var $header_row = $(
			"<table class='connections-tab'>\n"+
			"<tr class='row row-header'>\n"+
				"<th class='header header-status'>Status</th>\n"+
				"<th class='header header-accounts-connections'>Accounts &amp; Connections</th>\n"+
				"<th class='header header-connected-since'>Connected Since</th>\n"+
				"<th class='header header-last-updated'>Last Updated</th>\n"+
				"<th class='header header-refresh'>Refresh</th>\n"+
				"<th class='header header-delete '>Delete</th>\n"+
			"</tr>\n"+
			"</table>");
		
		self.$header.append($c_tab_titles_bar);
		self.$header.append($header_row);
		
	
		$c_content.append(self.$c_tabs)
		el.append($c_content);
		if(self.selected_tab)
			self.select_tab(self.selected_tab);
		else
			self.select_tab(0);

	/* make 'lights' work on connection tabs */
		for(var type in self.val)
		{

			var s ="#connections-tab-"+type;
			if(self.error_states[type])
				$("#connections-tab-"+type).addClass('connections-status-red');
			else
				$("#connections-tab-"+type).addClass('connections-status-green');
			
			if(self.val[type] && self.val[type].length == 0)
				$("#connections-tab-"+type).addClass('connections-status-off');
				
		} 
		self.check_error(false);
		return null;/* to get rid of stupid eclipse warning */
	},
	select_tab:function(idx)
	{
		var self = this;
		self.selected_tab = idx;
		var tt = self.$c_tabs.children();
		for(var i = 0;i < tt.length;i++)
		{
			$(tt[i]).hide();
		}
		$(tt[idx]).show();

		var ttt = self.$c_tab_titles_bar.children();
		for(var i = 0;i < ttt.length;i++)
		{
			$(ttt[i]).removeClass('selected');
		}
		$(ttt[idx]).addClass('selected');
		self.selected_type = $(tt[idx]).data('type');
	},
	/* this one is for the indicator next to the connections button on the dashboard */
	check_error:function(do_list)
	{
		var self = this;
		function do_check()
		{
			for(var k in self.error_states)
			{
				if(self.error_states[k] == true)
				{
					$("#connections-button").removeClass('green');
					$("#connections-button").addClass('red');
					return;
				}
			}
			$("#connections-button").removeClass('red');
			$("#connections-button").addClass('green');
		}
		
		if(do_list == null)
			do_list = true;
		
		if(do_list)
			self.list(function(){do_check();});
		else
			do_check();

	}
	
	
});







