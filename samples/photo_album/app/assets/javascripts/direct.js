$(document).ready(function() {
// (see http://blueimp.github.io/jQuery-File-Upload/).
// Cloudinary jQuery integration library uses jQuery File Upload widget
// Any file input field with cloudinary-fileupload class is automatically
// wrapped using the File Upload widget and configured for Cloudinary uploads.
// You can further customize the configuration using .fileupload method
// as we do below.
$(".cloudinary-fileupload")
  .cloudinary_fileupload({
    // Uncomment the following lines to enable client side image resizing and valiation.
    // Make sure cloudinary/processing is included the js file
    //disableImageResize: false,
    //imageMaxWidth: 800,
    //imageMaxHeight: 600,
    //acceptFileTypes: /(\.|\/)(gif|jpe?g|png|bmp|ico)$/i,
    //maxFileSize: 20000000, // 20MB
    dropZone: "#direct_upload",
    start: function (e) {
      $(".status").text("Starting upload...");
    },
    progress: function (e, data) {
      $(".status").text("Uploading... " + Math.round((data.loaded * 100.0) / data.total) + "%");
    },
    fail: function (e, data) {
      $(".status").text("Upload failed");
    }
  })
  .off("cloudinarydone").on("cloudinarydone", function (e, data) {
    var cl = cloudinary.Cloudinary.new();
    cl.fromDocument();
    $("[name=bytes]").val(data.result.bytes);
    $(".status").text("");
    $(".preview").html(
      cl.image(data.result.public_id, {
        format: data.result.format, width: 50, height: 50, crop: "fit"
      })
    );
    view_upload_details(data.result);
  });
});

function view_upload_details(upload) {
  // Build an html table out of the upload object
  var rows = [];
  $.each(upload, function(k,v){
    rows.push(
      $("<tr>")
        .append($("<td>").text(k))
        .append($("<td>").text(JSON.stringify(v))));
  });
  $("#info").html(
    $("<div class=\"upload_details\">")
      .append("<h2>Upload metadata:</h2>")
      .append($("<table>").append(rows)));
}