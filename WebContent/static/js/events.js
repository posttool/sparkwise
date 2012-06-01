
var $events_dialog;
var g_events_list = new Array();

$_events_init = function()
{
	/* sparkwise js settings menu is what triggers this dialog showing up*/
	$events_dialog = $('#events-dialog').events();
	$events_dialog.hide();
	$_events_init_public();
};

$_events_init_public = function()
{
    do_module('Dashboard/ListEvents', null, function(evt_data)
	    {
	    	g_events_list = evt_data;
	    });
}

var g_get_events_between_dates = function(latest_dval,earliest_dval)
{
	var visible_events = [];
	for(var i = 0;i < g_events_list.length;i++)
	{
		var event = g_events_list[i];
		if(event.date <= latest_dval && event.date >= earliest_dval)
			visible_events.push(event);
		/* break out when the events are past our window */
		if(event.date < earliest_dval)
			break;
	}
	return visible_events;
}

$.widget("sparkwise.events",
{
	 options: {page_size:5,current_page:0,pages_in_the_middle:4},/*pages in the middle must be even for now unless you add support for odd pages in the middle*/
	  _create: function()
	  {
	    var self = this, el = this.element;
	  },
	  list: function(on_complete)
	  {
	    var self = this;
	    var el = $(self.element);
	    el.empty();
	    el.html("<div class='connections-loading'>Events loading...</div>");
	    do_module('Dashboard/ListEvents', null, function(evt_data)
	    {
	    	g_events_list = evt_data;
	    	self.num_pages = Math.ceil(g_events_list.length / self.options.page_size);
	    	self.goto_page(self.options.current_page);
	    	self.$dialog.position();
	    	if(on_complete != null)
	    		on_complete();
	    });
	  },

	 open: function(add_event_mode)
	 {	
		  	var self = this;
		  	
		  	if (self.$root!=null)
		  		self.$root.remove();
		  	self.$root = $$div($(this.element));
		  	self.$dialog = $$dialog('EVENTS','',self.$root, null, {width: 790});
		  	self.$dialog.content.css({'padding-top':'0','margin-top':'0'});
		  	self.$dialog.content_text.css({'padding-top':'0','margin-top':'0'});
		  	self.list(function(){
			  	if (add_event_mode)
			  		self.add_event();
		  	});
		  	
	 },
	 add_event:function(b)
	 {
		var self = this;
		self.$page.hide(); 
		if (b==null || b)
			self.set_event_form();
		self.$ce_form.show();
		self.$dialog.setButtons([{ name: "Back to List", destroy: false, click: function(){ self.hide_form(); }, class_name: 'back-btn-event' }]);

	 },
	 hide_form:function()
	 {
		var self = this;
		self.$dialog.setButtons([{ name: "Add Event", destroy: false, click: function(){ self.add_event(); } }]);
		if (self.$ce_form==null)
			return;
		self.$ce_form.hide();
		self.$page.show(); 
	 },
	  goto_page:function(p)
	  {
		  var self = this;
		  self.options.current_page   = p;
		  self.options.current_offset = self.options.current_page * self.options.page_size;		  
		  self.value(g_events_list);
	  },
	  value: function(v)
	  {

		  var self = this;
		  if(v == null)
			return self.val;
		  self.val = v;
		  self.hide_form();
		  self.update_ui();
	  },
	  
	  update_ui: function()
	  {
		  var self = this;

		  self.$root.empty();
		  var el = self.$root;
		  
		  var $e_content = $("<div class='events-dialog-content'></div>");
		  
		  self.$page = $("<table class='events-page'></table>");
		  var $page = self.$page;
		  
		  var $header_row = $(
			"<tr class='events-page-table-row events-page-table-row-header'>\n"+
				"<th class='events-page-table-header events-page-table-header-date'>Date</th>\n"+
				"<th class='events-page-table-header events-page-table-header-accounts-name-desc'>Name &amp; Description</th>\n"+
				"<th class='events-page-table-header events-page-table-header-edit'>Edit</th>\n"+
				"<th class='events-page-table-header events-page-table-header-delete '>Delete</th>\n"+
			"</tr>\n");
		  $page.append($header_row);

		  for(var i = self.options.current_offset;(i < self.options.current_offset + self.options.page_size && i < self.val.length);i++)
		  {
			  	var event = self.val[i];
			  	var event_id = event.id;
			  	var descr = com_pagesociety_util_StringUtil.shorten(event.description,100);
			  	
				var $e_row 		  	= $("<tr class='events-page-table-row' id='event_row_"+event_id+"'></tr>");
				var $edate        	= $("<td class='events-page-table-data events-page-table-data-date'>"+format_event_date(event.date)+"</td>");
				var $ename_desc	  	= $("<td class='events-page-table-data events-page-table-data-name-desc'><span class='events-page-event-name'>"+event.name+"</span><br/><span class='events-page-event-description'>"+descr+"</span></td>");
				var $eedit       	= $("<td class='events-page-table-data  events-page-table-data-edit events-page-button id='event_edit_button_"+event_id+"'><span>[o]</span></td>");
				var $edelete		= $("<td class='events-page-table-data  events-page-table-data-delete events-page-button' id='event_delete_button_"+event_id+"'><span>[o]</span></td>");
		  
				$('span', $edelete).click({id:event.id,name:(event.name)},function(ee)
						{
						$$dialog_ok('Confirm','Are you sure you want to delete the event: '+ee.data.name+' ?','OK','Cancel',
								      function() {
											 	do_module('Dashboard/DeleteEvent', [ee.data.id], function(data) {
												 self.list(function(){$_grid.load();});
										        });
								      });
						}
				);
				
				$('span.events-page-event-name', $ename_desc).click({id:event.id,date:event.date,name:event.name,desc:event.description},function(ee)
						{
							
							self.set_event_form(ee.data.id,ee.data.date,ee.data.name,ee.data.desc);
							self.add_event(false);
						});
				  
				
				$('span', $eedit).click({id:event.id,date:event.date,name:event.name,desc:event.description},function(ee)
				{
					self.set_event_form(ee.data.id,ee.data.date,ee.data.name,ee.data.desc);
					self.add_event(false);
				});
		  
				$e_row.append($edate);
				$e_row.append($ename_desc);
				$e_row.append($eedit);
				$e_row.append($edelete);
				$page.append($e_row);
		  
		  }

		  /* make pager */
			 var $pager_row 	= $("<tr class='events-pager-row' ></tr>");
			 //$page.append($add_event_row);
			 var $pager_data 	= $("<td colspan='4'></td>"); 	
			 $pager_row.append($pager_data);
			 var $pager_container = $("<div id='events-page-pager-container'></div>");
			 $pager_data.append($pager_container);
			 
			 var $left_arrow = $("<span class='event-pager-element-wrapper pager-arrow-left-wrapper'><span class='events-page-pager-element pager-arrow-left'>&lt;</span></span>");
			 $pager_container.append($left_arrow);
			 $left_arrow.css({'display':'none'});
			 if(self.options.current_page != 0)
			 {
				 $left_arrow.click({page:p},function(ee){
					 self.goto_page(self.options.current_page-1); 
				 });
				 $left_arrow.css({'display':'block'});
			 }
			 
			 
			 
			 if(self.num_pages > (self.options.pages_in_the_middle+2))
			 {


				 var $first_page = $("<span class='event-pager-element-wrapper event-pager-element-wrapper-1'><span class='events-page-pager-element'>1</span></span>");
				 if(self.options.current_page == 0)
					 $first_page.addClass('selected');
				 else
				 {
					 $first_page.click(function(ee){
						 self.goto_page(0); 
					 });
				 }
				 $pager_container.append($first_page);
				 
				 var h = self.options.pages_in_the_middle / 2;
				 var start =  self.options.current_page - h;
				 var end   =  self.options.current_page + h;
				 var show_start_elipses = true;
				 var show_end_elipses 	= true;
				 if(start < 1)
				 {
					 show_start_elipses = false;
					 start = 1;
					 end   = 1 + 2*h;
				 }
				 if(end > self.num_pages -1)
				 {
					 show_end_elipses = false;
					 start = (self.num_pages -1) - 2*h;
					 end = self.num_pages -1;
				 }
				 
				 if(show_start_elipses)
				 {
						var $selip =  $("<span class='event-pager-element-wrapper'><span class='events-page-pager-element'>...</span></span>");
						$pager_container.append($selip);
				 }
				 
				 for(var p = start;p < end;p++)
				 {
					 var $pager_element = $("<span class='event-pager-element-wrapper'><span class='events-page-pager-element'>"+(p+1)+"</span></span>");
					 $pager_container.append($pager_element);					 
					 if(p != self.options.current_page)
					 {
						 $pager_element.click({page:p},function(ee){
							 self.goto_page(ee.data.page); 
						 }
						 );
					 }
					 else
					 {
						 $pager_element.addClass("selected");
					 }
					 
				 }
				 
				 if(show_end_elipses)
				 {
						var $eelip =  $("<span class='event-pager-element-wrapper'><span class='events-page-pager-element'>...</span></span>");
						$pager_container.append($eelip);
				 }
				 var $last_page = $("<span class='event-pager-element-wrapper'><span class='events-page-pager-element'>"+(self.num_pages)+"</span></span>");
				 $pager_container.append($last_page);
				 if(self.options.current_page == self.num_pages-1)
					 $last_page.addClass('selected');
				 else
				 {
					 $last_page.click(function(ee){
						 self.goto_page(self.num_pages-1); 
					 });
				 }
			 }
			 else
			 {
				 for(var p = 0;p < self.num_pages;p++)
				 {
					 var $pager_element = $("<span class='event-pager-element-wrapper event-pager-element-wrapper-"+(p+1)+"'><span class='events-page-pager-element'>"+(p+1)+"</span></span>");
					 $pager_container.append($pager_element);
					 
					 if(p != self.options.current_page)
					 {
						 $pager_element.click({page:p},function(ee){
							 self.goto_page(ee.data.page); 
						 }
						 );
					 }
					 else
					 {
						 $pager_element.addClass("selected");
					 }
					 
				 }				 
			 }

			 var $right_arrow = $("<span class='event-pager-element-wrapper pager-arrow-right-wrapper'><span class='events-page-pager-element pager-arrow-right'>&gt;</span></span>");
			 $pager_container.append($right_arrow);
			 $right_arrow.css({'visibility':'hidden'});
			 if((self.options.current_page != self.num_pages - 1))
			 {

				 $right_arrow.click({page:p},function(ee){
					 self.goto_page(self.options.current_page+1); 
				 }
				 );
				 $right_arrow.css({'visibility':'visible'});
				 
			 }

			 $page.append($pager_row);
			 if(self.num_pages < 2)
				 $pager_row.hide();
			 else
				 $pager_row.show();
			 /* end pager...easy right */
		  $e_content.append($page);
		  
		  
		  /*being create/edit form */
		  var $ce_form  =$("<form id='events-editor-form'></form>"); 
		  var $ce_table = $("<div id='events-editor'></div>"); 
		  $ce_table.append("<div class='events-editor-table-row'><div class='events-editor-label'>Date</div><div class='events-editor-editor'><input class='events-editor-input' id='event-form-date'/></div><div class='clear'></div></div>");
		  $ce_table.append("<div class='events-editor-table-row'><div class='events-editor-label'>Name</div><div class='events-editor-editor'><input class='events-editor-input' id='event-form-name'/></div><div class='clear'></div></div>");
		  $ce_table.append("<div class='events-editor-table-row'><div class='events-editor-label'>Description</div><div class='events-editor-editor'><textarea class='events-editor-input' id='event-form-description' maxlength='200'></textarea><br/><span id='event-form-description-remaining-label'>&nbsp;Characters Remaining </span><span id='event-form-description-remaining-value'></span><br clear='both'/></div><div class='clear'></div></div>");
		  $ce_table.append("<div id='events-editor-action-row'><button value='save' id='events-dialog-save' class='btn-save'>Save</button><button value='cancel' id='events-dialog-cancel' class='btn-cancel'>Cancel</button><span id='event-form-error'></span></div>")
		  $ce_form.append($ce_table);
		  el.append($e_content);
		  el.append($ce_form);

		  $('#events-editor-form').submit(function(){return false;});
		  
		  $('.events-page-table-row').not('.events-page-table-row-header').hover(function() {
			 $(this).css('background','#fff'); 
		  }, function() {
			  $(this).css('background','none');
		  });
		  
		  $ce_form.hide();
		  /* install date picker */
		  $('#event-form-date').datepicker();
		  
		  /* deal with char counting stuff for desc */
		  $('#event-form-description-remaining-value').text($('#event-form-description').attr('maxlength'));
		  $('#event-form-description').keyup({},function(e){
			  var maxLength = this.getAttribute('maxlength');
			  var remaining = (maxLength - this.value.length);
			  if(remaining < 0)
			  {
				  $('#event-form-description-remaining-label').addClass('over');
				  $('#event-form-description-remaining-value').addClass('over');
			  }
			  else
			  {
				  $('#event-form-description-remaining-label').removeClass('over');
				  $('#event-form-description-remaining-value').removeClass('over');  
			  }
			  $('#event-form-description-remaining-value').text(remaining);
		  });
		  
		  $('#events-dialog-save').click({},function(e){
			 var date = $('#event-form-date').datepicker( "getDate" )

			 var name 		 = $('#event-form-name').val();
			 var description = $('#event-form-description').val();
			 if(description.length > $('#event-form-description').attr('maxlength') )
				 description = description.substring(0, $('#event-form-description').attr('maxlength'));
			 
			 if(date == null || date == '' || name == '')
			 {
				 $('#event-form-error').text('An event must contain at least a date and a name.');	 
				 return;
			 }

			 if(self.current_edit_id == null)
			 {
				  do_module('Dashboard/AddEvent', [name, date.getTime(), description], function(data) {
						 self.list(function(){$_grid.refresh();});
				        },
				        function(error){
				        	$('#event-form-error').text('There was an unexpected problem saving your event.'+error.message);	 
				        });
			 }
			 else
			 {
				  do_module('Dashboard/UpdateEvent', [self.current_edit_id,name, date.getTime(), description], function(data) {
						self.current_edit_id = null; 
					  	self.list(function(){$_grid.refresh();});
				        },
				        function(error){
				        	$('#event-form-error').text('There was an unexpected problem saving your event.'+error.message);	 
				        }); 
			 }
			 
		  });
		  
		  $('#events-dialog-cancel').click({},function(e){
			  self.hide_form();
		  });
		  
		  $('#events-dialog-back').click({},function(e){
			  self.hide_form();
		  });
		  self.$page = $page;
		  self.$ce_form = $ce_form;
		  return null;
		
	  }, 
	  set_event_form:function(id,date,name,desc)
	  {
		  //console.log(id,date,name,desc)
		  var self = this;
		  if(id == null)
		  {
			  $('#event-form-date').val("");
			  $('#event-form-name').val("");
			  $('#event-form-description').val("");
			  $('#event-form-description-remaining-label').removeClass('over');
			  $('#event-form-description-remaining-value').removeClass('over');  
			  $('#event-form-description-remaining-value').text($('#event-form-description').attr('maxlength'));
		  }
		  else
		  {
			  self.current_edit_id = id;

			  if(desc == null)
				  desc = '';
			  
			  $('#event-form-date').datepicker("setDate",date);
			  $('#event-form-name').val(name);
			  $('#event-form-description').val(desc);
			  var remaining = $('#event-form-description').attr('maxlength')-desc.length;
			  if(remaining < 0)
			  {
				  $('#event-form-description-remaining-label').addClass('over');
				  $('#event-form-description-remaining-value').addClass('over');
			  }
			  $('#event-form-description-remaining-value').text(remaining);  
		  }
	  }
	  
	
});


var format_event_date = function(time)
{
	var d = new Date(time);
	var dd = d.getDate();
	return (d.getMonth()+1)+"/"+dd+"/"+d.getFullYear();
}



