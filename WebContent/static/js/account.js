var g_show_account;

$_account_init = function(username,email)
{
	if(username == null)
		username = "";
	if(email == null)
		email = "";

	var email_filter =	/^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
	var is_valid_email = function(email)
	{
		return email_filter.test(email)
	};
	
	var delete_account = function()
	{
		$$dialog_ok('Delete Confirmation','Are you sure you want to delete this account?<br/> '+
			'Doing so will cause all of your information to be removed.<br/> '+
			'There will be no way to recover this information. This is permanent.','Delete','Cancel',
			function() 
			{
				do_module('Dashboard/DeleteAccount', [], function(e)
				{
					window.location.href = "/";							
				});
			});
	}
	
	// grab account html from the page
	var $acct = $('#account-content');
	var $acct_dialog = null;

	var delete_button_info = { name: "Delete Account", destroy: false, click: delete_account };
	var back_button_info = { name: "Account", destroy: false, click: show_account_form, class_name: 'back-btn' };

	var show_account = function(page)
	{
		reset_change_password_form();
		reset_change_email_form();
		reset_change_username_form();
		$('#change-password-container').hide();
		$('#change-email-container').hide();
		$('#change-username-container').hide();
		$('#account-content-user-info-wrapper').show();
	
		$acct_dialog = $$dialog('ACCOUNT','',$acct);
		if(page == 'password')
			show_password_form();
		else
			show_account_form();
		bind_account_f();//because dialog throws them away when closed
	};
	

	g_show_account = show_account; //TODO put in se_sparkwi namespace
	if (location.hash=="#account")
		show_account();

	function reset_change_password_form()
	{
		$('#change-password-form input[name=old-pw]').val("");
		$('#change-password-form input[name=new-pw]').val("");
		$('#change-password-form input[name=new-pw2]').val("");
	}

	function reset_change_password_form()
	{
		$('#change-password-ok-msg').html("");
		$('#change-password-error-msg').html("");
		$('#change-password-form input[name=old-pw]').val("");
		$('#change-password-form input[name=new-pw]').val("");
		$('#change-password-form input[name=new-pw2]').val("");
	}
	
	function reset_change_username_form()
	{
		$('#change-username-ok-msg').html("");
		$('#change-username-error-msg').html("");
		$('#change-username-form input[name=new-username]').val(username);
	}
	
	function reset_change_email_form()
	{
		$('#change-email-ok-msg').html("");
		$('#change-email-error-msg').html("");
		$('#change-email-form input[name=new-email]').val(email);
	}
	
	function show_username_form()
	{
		$acct_dialog.setButtons([back_button_info]);
		reset_change_username_form();
		$('#change-username-container').show();
		$('#account-content-user-info-wrapper').hide();
	}
	
	function show_email_form()
	{
		$acct_dialog.setButtons([back_button_info]);
		reset_change_email_form();
		$('#change-email-container').show();
		$('#account-content-user-info-wrapper').hide();
	}
	function show_password_form()
	{
		$acct_dialog.setButtons([back_button_info]);
		reset_change_password_form();
		$('#change-password-container').show();
		$('#account-content-user-info-wrapper').hide();
	}
	function show_account_form()
	{
		$acct_dialog.setButtons([delete_button_info]);
		$('#change-password-container').hide();
		$('#change-email-container').hide();
		$('#change-username-container').hide();
		$('#account-content-user-info-wrapper').show();
	}
	
	function bind_account_f()
	{
		$('#edit-username-button').click(show_username_form);
		$('#edit-email-button').click(show_email_form);
		$('#edit-password-button').click(show_password_form);
		$('.back-to-account-button').click(show_account_form);
		//

		var change_password_submitter = null;
		$('#change-password-form :button').click(function() {
			change_password_submitter = this.value;
		});

		$('#change-password-form').submit(function()
		{
			var form = this;
			$('#change-password-error-msg').html('');
			$('#change-password-ok-msg').html('');
			if(change_password_submitter == 'cancel')
			{
				show_account_form();
				return false;
			}

		 	var npw	= $.trim($('#change-password-form input[name=new-pw]').val());
		 	var npw2 = $.trim($('#change-password-form input[name=new-pw2]').val());
			 	
		 	if(npw == "" || npw2 == "")
		 	{
		 		$('#change-password-error-msg').html('<span>Please fill out all form fields.</span>');
		 		return false;
		 	}
		 	if(npw.length < 5)
		 	{
		 		$('#change-password-error-msg').html('<span>New password has to be at least 5 characters long.</span>');
		 		return false;
		 	}
		 	if(npw != npw2)
		 	{
		 		$('#change-password-error-msg').html('<span>New passwords do not match.</span>');
		 		return false;
		 	}
		 	do_module("User/UpdatePassword",[npw,false],
				function(user)
				{
					$('#change-password-error-msg').html("");
					$('#change-password-ok-msg').html("<span>Password was successfully updated.</span>");

				},
				function(error)
				{
					$('#change-password-error-msg').html('<span>'+error.message+'</span>');
				});
		 	return false;
		});


		var change_username_submitter = null;
		$('#change-username-form :button').click(function() {
				change_username_submitter = this.value;
		});
		$('#change-username-form').submit(function()
		{

			var form = this;
			$('#change-username-ok-msg').html('');
			$('#change-username-error-msg').html('');
			if(change_username_submitter == 'cancel')
			{
				show_account_form();
				return false;
			}

		 	var nun	= $.trim($('#change-username-form input[name=new-username]').val());

		 	if(nun == "")
		 	{
		 		$('#change-username-ok-msg').html('');
		 		$('#change-username-error-msg').html('<span>Please fill out all form fields.</span>');
		 		return false;
		 	}
		 	do_module("User/GetUser",[],function(user)
					{
						do_module('User/UpdateUserName',[user.getId(),nun],
							function(user)
							{
								username	= user._attributes.username;
								$('#account-first_name').text(username);
								$('#header-user-name').text(username);
								$('#change-uername-error-msg').html("");
								$('#change-username-ok-msg').html("<span>Username was successfully updated.</span>");
							},
							function(error)
							{
								$('#change-username-ok-msg').html('');
								$('#change-username-error-msg').html('<span>'+error.message+'</span>');
							}
						);
				 	},
				 	function(error)
					{
				 		$('#change-username-error-msg').html('<span>Problem looking up user. '+error.message + '</span>');
				 		return false;
					});
			return false;
			
		});
			
		var change_email_submitter = null;
		$('#change-email-form :button').click(function() {
					 change_email_submitter = this.value;
		});
		$('#change-email-form').submit(function()
		{
			var form = this;
			$('#change-email-error-msg').html('');
			$('#change-email-ok-msg').html('');
		 	if(change_email_submitter == 'cancel')
		 	{
		 		show_account_form();
		 		return false;
		 	}
		 	var ne	= $.trim($('#change-email-form input[name=new-email]').val());
		 	if(ne == "")
		 	{
		 		$('#change-email-ok-msg').html('');
		 		$('#change-email-error-msg').html('<span>Please fill out all form fields.</span>');
		 		return false;
		 	}
		 	if( !is_valid_email(ne))
		 	{
		 		$('#change-email-ok-msg').html('');
		 		$('#change-email-error-msg').html('<span>Please provide valid email address.</span>');
		 		return false;
		 	}

		 	do_module("User/GetUser",[],function(user)
		 		{
						do_module('User/UpdateEmail',[user.getId(),ne],
							function(user)
							{
							email	= user._attributes.email;
							$('#account-email').text(email);
							$('#change-email-error-msg').html("");
						$('#change-email-ok-msg').html("<span>Email was successfully updated.</span>");
							},
							function(error)
							{
								$('#change-email-error-msg').html('<span>'+error.message+'</span>');
							}
						);
				},
				function(error)
				{
				 		$('#change-email-error-msg').html('<span>Problem looking up user. '+error.message+'</span>');
				 		return false;
				});
		 		return false;
		});
	} 	
}


