/**
 * 图片上传插件
 *
 */
(function($){
	$.extend($.fn,{
		uploadimage: function(setting){
			$.fn.uploadimage.default = {
				wrapWidth: 300,
				wrapHeight: 80,
				imgWidth: 60,
				imgHeight: 60
			};
			
			$.extend({},$.fn.uploadimage.default,setting);
			
			var uploadDom = $('<div class="uploadimage-wrap"><div class="image-view"><img src="./static/images/add-img.png"><input type="file"><a href="javascript:void(0)" class="reload-img">重选</a><a href="javascript:void(0)" class="del-img"></a></div></div>');
			$(this).hide();
			$(this).prev(uploadDom);
			return $(this);
		}
	});
})(jQuery);

