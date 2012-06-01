

function toggle_feedback_form() {
	var $fbk = $('#give-feedback');
	var $fbk_button = $('.feedback-button', $fbk);
	
	if ($fbk_button.hasClass('feedback-button-opened')) {
		$fbk_button.removeClass('feedback-button-opened');
		$fbk.stop().animate({left: '-290px'}, 300);
		do_feedback_form_reset();
	} else {
		$fbk_button.addClass('feedback-button-opened');
		$fbk.stop().animate({left: 0}, 300);
	}
}

function do_feedback_form_reset() {
	var $fbk = $('#give-feedback');
	
	$('form', $fbk).find("input[type=text], textarea").val("");
	$('form', $fbk).find("select").each(function(){
		var field = jQuery(this);
		field.val( jQuery("option:first", field).val() );
	});
}

function do_feedback_thanks()
{
	$('#give-feedback .thanks-message').show();
	$('#give-feedback .feedback-form').hide();
}

function do_feedback_form()
{
	$('#give-feedback .thanks-message').hide();
	do_feedback_form_reset();
	$('#give-feedback .feedback-form').show();
}

function init_feedback_form(email)
{
	
    $('#frm_feedback').submit(function(e)
    {
    	e.preventDefault();
    	var ok = true;
    	var msg = "";
    	var form = this;
    	$('.error-msg', this).empty();
		$(this).find('input').removeClass('error-input');

		var subj = jQuery.trim($('[name=txt_subject]', form).val());
		var os = jQuery.trim($('[name=cmb_os]', form).val());
    	var browser = jQuery.trim($('[name=cmb_browser]',form).val());
    	var msg = jQuery.trim($('[name=txt_message]',form).val());
    	
    	if(subj=="")
    	{
    		ok = false;
	    	$('.error-msg-first-name', form).empty().append('Please provide a subject.')
	    	$('[name=first_name]',form).addClass('error-input');
    	}
    	//TODO hook up all fields
    	
    	if (ok) 
    	{
    		do_feedback_thanks();
			do_module("Feedback/AddFeedbackEntry",[subj,os,browser,msg,email],
			function()
			{
			},
			function(error)
			{
				alert("error: "+error._ps_clazz+" "+error.message);
			});
    	}
    	return false;
    });

}
