<%@ page import="java.util.Base64" %>
<%@ page import="java.util.UUID" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
	UUID uuid = UUID.randomUUID();
	String token = Base64.getUrlEncoder().encodeToString(com.google.common.primitives.Bytes.concat(
			com.google.common.primitives.Longs.toByteArray(uuid.getLeastSignificantBits()), 
			com.google.common.primitives.Longs.toByteArray(uuid.getMostSignificantBits())
			));
%>
<html xmlns:ng="http://angularjs.org" ng-app="wehelpuApp" class="ng-app:wehelpuApp" id="ng-app"> <head> <meta charset="utf-8"> <title>吉利学院</title> <meta name="description" content=""> <link rel="shortcut icon" href="static/images/favicon.5b870c2d.ico"> <meta name="viewport" content="width=device-width"> <!-- Place favicon.ico and apple-touch-icon.png in the root directory --> <link rel="stylesheet" href="static/styles/main.94899f64.css">
      <script>
      	var g_qr_code = 'api/qr_code.jpg?content=<%=URLEncoder.encode("http://" +  request.getServerName() +  "/mobile/confirm_web_login.jsp?token=" + URLEncoder.encode(token, "UTF-8"), "UTF-8")%>&size=430&';
		var g_token = '<%=token %>';
      </script>
 </head> <body> <!--[if lte IE 8]>
      <script src="scripts/IE8.js"></script>
      <script src="http://cdnjs.cloudflare.com/ajax/libs/html5shiv/3.6.2pre/html5shiv.js"></script>
      <script src="http://cdnjs.cloudflare.com/ajax/libs/json2/20121008/json2.js"></script>
    <![endif]--> <!-- Add your site or application content here --> <div ng-view class="whu-main"> </div> <!-- Google Analytics: change UA-XXXXX-X to be your site's ID --> <script src="static/scripts/vendor.47481fcc.js"></script> <script src="static/scripts/scripts.ea322a04.js"></script> <!-- gallery.js start --> <link rel="stylesheet" href="static/scripts/gallery/css/blueimp-gallery.css"> <link rel="stylesheet" href="static/scripts/gallery/css/blueimp-gallery-indicator.css"> <link rel="stylesheet" href="static/scripts/gallery/css/blueimp-gallery-video.css"> <script src="static/scripts/gallery/js/blueimp-helper.js"></script> <script src="static/scripts/gallery/js/blueimp-gallery.js"></script> <script src="static/scripts/gallery/js/blueimp-gallery-fullscreen.js"></script> <script src="static/scripts/gallery/js/blueimp-gallery-indicator.js"></script> <script src="static/scripts/gallery/js/blueimp-gallery-video.js"></script> <script src="static/scripts/gallery/js/blueimp-gallery-vimeo.js"></script> <script src="static/scripts/gallery/js/blueimp-gallery-youtube.js"></script> <script src="static/scripts/gallery/js/jquery.blueimp-gallery.js"></script> <!-- gallery.js end --> </body> </html>