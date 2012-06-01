$_registration_init = function()
{

	var email_filter =  /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
	var is_valid_email = function(email)
	{
		return email_filter.test(email)
	};

	var le = $.cookie('login-email');
	if (le != null)
		$('[name=login_email]').val(le);


	// check fields of 'account-forms'
    $('#registration-form').submit(function()
    {

    	var ok = true;
    	var msg = "";
    	var form = this;
    	$('.error-msg', this).empty();
		$(this).find('input').removeClass('error-input');

    	var n  	= $.trim($('[name=name]', form).val());
    	var e  	= $.trim($('[name=email]',form).val());
		var pw1 = $.trim($('[name=pw1]',form).val());
		var pw2 = $.trim($('[name=pw2]',form).val());
		


    	if(!is_valid_email(e))
    	{
    		ok = false;
	    	$('.error-msg-email', form).empty().append('Please provide a properly formatted email address.')
	    	$('[name=email]',form).addClass('error-input');
    	}


    	if(pw1.length< 5)
    	{
			ok = false;
			$('[name=pw1]',form).addClass('error-input');
			msg = 'Passwords must be at least 5 characters';
			$('.error-msg-pw1', form).empty().append(msg);
    	}

    	if(pw1!=pw2)
    	{
    		ok = false;
    		$('[name=pw2]',form).addClass('error-input');
	    	msg = 'Passwords do not match.';
	    	$('.error-msg-pw2', form).empty().append(msg);
    	}

    	if (ok) {

			do_module("Registration/Register",[e,n,pw1,false],
			function()
			{
				var m = "<div class='sign-up-success-msg'><h3>Thank You</h3><p><strong>Thanks for joining Sparkwise!<strong><br />"+
						"We just sent you an email with a link that will take you directly to your new account.</p>" +
						"<p>We're glad you&rsquo;re here.<br />"+
						"The Sparkwise Team</p</div>";
				
				m = "<div class='sign-up-success-msg'><h3>Thank You</h3><p><strong>Thanks for joining Sparkwise!</strong><br />"+
					"Login with the form to the right and start bringing your data to life.</p>" +
					"<p>We're glad you&rsquo;re here.<br />"+
					"The Sparkwise Team</p></div>";
				
				$('#registration-form').parent().empty().html(m);
			},
			function(error)
			{
				if(error.exception.code == 0x20001)
				{
					$('.error-msg-email', form).empty().append(error.message);
	    			$('[name=email]',form).addClass('error-input');
				}
				else
				{
					alert("unknown error: "+error._ps_clazz+" "+error.message);
				}

			});
    	}

    	return false;

    });

    
   

    $('#login-form').submit(function()
    {
		var form = this;
    	$('.error-msg', this).empty();

    	var e  	= $.trim($('[name=login_email]',form).val());
		var pw   = $.trim($('[name=login_pw]',form).val());


		do_module("User/Login",[e,pw,false],
		function(user)
		{
            $.cookie('login-email',e);
			window.location.href = '/dashboard';
		},
		function(error)
		{
			$('.error-msg', form).empty().append("Oops &ndash; your username / password combination doesn&rsquo;t match our records. Please re-enter your information.");
		});

		return false;
    });


    $('#forgot-password-form').submit(function()
    {
    	var form = this;
    	var e  	= $.trim($('[name=forgot_pw_email]',form).val());
    	$('.error-msg', form).empty();

    	do_module("ForgotPassword/ForgotPassword",[e],
		function()
		{
			$('#forgot-password-ok-text', form).empty();
    		$('#forgot-password-ok-text', form).append("Email sent to "+e);
		},
		function(error)
		{
			$('.error-msg', form).empty().append(error.message);
		});

		return false;
    });


}

function do_forgot_password()
{
	$('.sign-in-form').hide();
	$('.sign-up-form').hide();
	$('#forgot-password-container').show();
}

function do_sign_in()
{
	$('#forgot-password-container').hide();
	$('.sign-up-form').hide();
	$('.sign-in-form').show();
}

function do_sign_up()
{
	$('#forgot-password-container').hide();	
	$('.sign-in-form').hide();
	$('.sign-up-form').show();
}