/*
*
* Contact JS
* @DynamicLayers
*/
$(function() {
    // Get the form.
    var form = $('#volunteer-form');

    // Get the messages div.
    var formMessages = $('#volunteer-form-messages');

    // Set up an event listener for the contact form.
	$(form).submit(function(event) {
		// Stop the browser from submitting the form.
		event.preventDefault();

		// Serialize the form data.
		var formData = $(form).serialize();
		// Submit the form using AJAX.
		$.ajax({
			type: 'POST',
			url: $(form).attr('action'),
			data: formData
		})
		.done(function(response) {
			// Make sure that the formMessages div has the 'success' class.
			$(formMessages).removeClass('alert-danger');
			$(formMessages).addClass('alert-success');

			// Set the message text.
			$(formMessages).text(response);

			// Clear the form.
			$('#volunteer-name').val('');
			$('#volunteer-email').val('');
			$('#volunteer-phone').val('');
			$('#volunteer-address').val('');
			$('#volunteer-message').val('');
		})
		.fail(function(data) {
			// Make sure that the formMessages div has the 'error' class.
			$(formMessages).removeClass('alert-danger');
			$(formMessages).addClass('alert-success');

			// Set the message text.
			if (data.responseText !== '') {
				$(formMessages).text(data.responseText);
			} else {
				$(formMessages).text('Thank You! Your submission has been sent.');
			}
		});

	});

});