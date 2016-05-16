<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
	String exam_info = (String)request.getAttribute(com.weizhu.webapp.mobile.exam.ExamInfoServlet.HTTP_REQUEST_ATTR_GET_EXAM_INFO_RESPONSE);
	String exam_share = (String)request.getAttribute(com.weizhu.webapp.mobile.exam.ExamInfoServlet.HTTP_REQUEST_ATTR_USER_INFO_RESPONSE);
	String contextPath = request.getContextPath();
%>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=0">
	<meta name="apple-mobile-web-app-capable" content="yes">
	<meta name="apple-mobile-web-app-status-bar-style" content="black">
	<meta name="format-detection" content="telephone=no">
	<title>考试试卷</title>
	<style type="text/css">
		body{font-family: arial, tahoma, 'Microsoft Yahei', '\5b8b\4f53', sans-serif;color: #4a4a4a;}
        body,span,h1,h2,h3,h4,h5,h6,ul,li,div,p{padding: 0;margin: 0;;}
        .main_wrap{width: 100%;position: relative;overflow: hidden;background-color: #e4e4e4;}
        .result-score{width: 100%;background-color: #fff;text-align: center;}
        .result-score-wrap{display: inline-block;margin: 15px;}
        .score-info{width: 100px;height: 100px;display: inline-block;-webkit-border-radius: 50%;background-color: #0078fd;color: #fff;margin-bottom: 10px;}
        .score-info span{font-size: 40px;display: block;margin-top: 25px;line-height: 40px;}
        .result-score-wrap .other-info{color: #ff0000;}
        .item-list{margin: 0px 15px;}
        .item-list .item{padding: 15px 5px 10px 15px;background-color: #fcfbd9;margin: 26px 0px;position: relative;-webkit-box-shadow: 0 5px 5px rgba(0, 0, 0, 0.1);}
        .item .questions-tip{height: 16px;line-height:16px;position: absolute;width: 120px;background: url(<%=contextPath%>/static/images/exam-icons.png);background-position: -128px 0;text-align: center;left: 50%;margin-top: -23px;margin-left: -60px;}
        .questions{line-height: 20px;word-break: break-all;}
        .question-error{color:#e83f3f;}
        .answers{margin-left:10px;}
        .answers .options{color:#8f8f8f;}
        .answers .options a{display: block;text-decoration: none;color: #8f8f8f;}
        .answers li{min-height: 30px;line-height: 30px;list-style: none;}
        .answers .checkbox span{display: inline-block;background: url(<%=contextPath%>/static/images/exam-icons.png) no-repeat -64px center;width: 16px;height: 30px;margin-right: 10px;}
        .answers .checkbox-checked span{background-position: -80px center;}
        .answers .checkbox-suc span{background-position: -96px center; color:#83cb3d;}
        .answers .checkbox-error span{background-position: -112px center;color:#e83f3f;}
        .answers .radio span{display: inline-block;background: url(<%=contextPath%>/static/images/exam-icons.png) no-repeat left center;width: 16px;height: 30px;margin-right: 10px;}
        .answers .radio-checked span{background-position: -16px center;}
        .answers .radio-suc span{background-position: -32px center;}
        .answers .radio-error span{background-position: -48px center;}
        .answers .radio-error,.answers .checkbox-error{color:#e83f3f;}
        .answers .radio-suc,.answers .checkbox-suc{ color:#83cb3d;}
        
        .answers .tf span{display: inline-block;background: url(<%=contextPath%>/static/images/exam-icons.png) no-repeat left center;width: 16px;height: 30px;margin-right: 10px;}
        .answers .tf-checked span{background-position: -16px center;}
        .answers .tf-suc span{background-position: -32px center;}
        .answers .tf-error span{background-position: -48px center;}
        .answers .tf-error,.answers .checkbox-error{color:#e83f3f;}
        .answers .tf-suc,.answers .checkbox-suc{ color:#83cb3d;}
        
        .answers .option-item .right-answer{background-position:-246px;margin-left:4px;}
        .answers .textarea{margin-top:5px;resize: none;width: 100%;height: 80px;line-height: 20px;font-size: 14px;-webkit-box-sizing: border-box;padding: 6px 12px;border: 1px solid #ccc;-webkit-border-radius: 5px;}
        .btn-wrap{text-align: center;}
        .btn-wrap .btn{background-color: #0379fe;border: none;border-radius: 50%;font-size: 16px;color: #fff;font-weight: 700;display: inline-block;width: 60px;height: 60px;line-height: 60px;margin-bottom: 20px;text-decoration: none;}
        .btn-wrap .btn-disable{background-color: #ccc;}
        .loading-wrap{width: 100%;height:100%;top:0;left: 0;position: fixed;background-color: RGBA(0,0,0,0.4);z-index: 1;}
        .loading-wrap .loader{width:50px;height: 50px;top: 50%;left:50%;margin-top:-80px;margin-left: -35px;font-size: 10px;position: absolute;text-indent: -9999em;border-top: 10px solid rgba(255, 255, 255, 0.2);border-right: 10px solid rgba(255, 255, 255, 0.2);border-bottom: 10px solid rgba(255, 255, 255, 0.2);border-left: 10px solid #ffffff;-webkit-animation: loading .8s infinite linear;animation: loading .8s infinite linear;}
        .loading-wrap .loader, .loading-wrap .loader:after{border-radius: 50%;width: 50px;height: 50px;}
        @-webkit-keyframes loading{
            0% {-webkit-transform: rotate(0deg);transform: rotate(0deg);}
            100% {-webkit-transform: rotate(360deg);transform: rotate(360deg);}
        }
        .tip-alert{width:100%;position:fixed;z-index: 1;text-align: center;bottom: 50px;}
        .tip-alert .alert-msg{padding: 10px 20px;background-color:RGBA(0,0,0,.4);color: #ffffff;-webkit-border-radius: 5px;display: inline-block;max-width: 80%;}
        .wait_wrap{width:80%;margin:auto;}
        .wait_wrap .word img{width:100%;margin:50px 0px 20px;}
        .wait_wrap .clock img{width:60%;margin-left:20%;}
        .wait_wrap .btn{width:50%;height:40px;text-align: center;line-height: 40px;margin:auto;background-color:#317f8c;-webkit-border-radius: 10px;margin-top:25px;}
        .wait_wrap .btn span{color:#fff;font-size:21px;font-weight: bold;}
        .lose-tip{text-align: center;height: 40px;line-height: 40px;font-size: 18px;background-color: #EFCC1C;color: #fff;}
        .warning-border{-webkit-box-shadow:0px 0px 8px #f00 !important;}
        
        .share_body{width:100%;height:100%;position:absolute;background-color:#FFF;top:0px;display:none}
        .share_main{width:80%;margin:auto;}
        .shareuser{margin:auto;padding:15px;height:40px;text-align:center;}
        .shareuser img{width:40px;border-radius:4px;float:left;max-height:40px;}
        .shareuser span{font-size:.8em;color:#4cba91;line-height:40px;display:inline-block;margin-left:10px;letter-spacing:1px;}
        .shareimg{width:100%;margin:auto;position:relative}
        .renwuimg{width:100%;margin:auto;}
        .shareword{position:relative;}
        .sharewordimg{width:100%;margin:20px 0px 10px;}
        .deng{position:absolute;left:-20px;bottom:10px;width:40px;}
        .showquestion{
        margin:auto;
        display:block;
        width:196px;
        padding:0px;
        font-size:14px;
        color:#d8aa5a;
        }
        .sharebtn{margin:auto;padding:0px;}
        .sharebtn .shares{display:block;}
        .sharebtn .shares img{width:40%;float:left;}
        .sharebtn .lookover{display:block;margin:auto;width:40%;}
 		.sharebtn .lookover img{width:100%;margin:auto;}
	</style>
</head>
<body>
	<div class="main_wrap">
        <div class="result-score" style="display: none;">
            <div class="result-score-wrap">
                <div class="score-info">
                    <span>0</span>分
                </div>
                <div class="other-info"></div>
            </div>
        </div>
        <div class="item-list">
        </div>
        <div class="btn-wrap" style="display:none;">
            <a class="btn" href="javascript:void(0)">交卷</a>
        </div>
    </div>
	<div class="wait_wrap" style="display:none;">
	    <div class="word">
	        <img src="<%=contextPath%>/static/images/word.png">
	    </div>
	    <div class="clock">
	        <img src="<%=contextPath%>/static/images/clock.png">
	    </div>
	    <div class="btn">
	        <span>请稍后再来</span>
	    </div>
	</div>
	
	<div class="share_body">
	<div class="share_main">
	<div class="shareuser">
		<div style="display:inline-block">
		<img src="../static/images/wz_contact_icon_user.png">
		<span>刘燕雪：100分</span>
		</div>
	</div>
	<div class="shareimg">
		<img class="renwuimg" src="../static/images/xueminrenwu.png">
	</div>
	<div class="shareword">
		<img src="../static/images/xuezha.png" class="sharewordimg">
		<img src="../static/images/deng.png" class="deng">
	</div>
	<span class="showquestion"  style="display:none">
	整场考试结束后才会公布答案哦
	</span>
	<div class="sharebtn">
		<a class="lookover" href="javascript:void(0)">
		<img src="../static/images/chakandaan.png">
		</a>
	</div>
	</div>
	</div>
	
	<div class="lose-tip" style="display:none;">
		Sorry! 您不需要参加本次考试 ^_^
	</div>
    <div class="loading-wrap" style="display:none;">
        <div class="loader">Loading...</div>
    </div>
    <div class="tip-alert" style="display:none;">
        <div class="alert-msg">网络连接失败，请稍后再试</div>
    </div>
<script type="text/javascript">
/* Zepto v1.0rc1 - polyfill zepto event detect fx ajax form touch - zeptojs.com/license */
(function(a){String.prototype.trim===a&&(String.prototype.trim=function(){return this.replace(/^\s+/,"").replace(/\s+$/,"")}),Array.prototype.reduce===a&&(Array.prototype.reduce=function(b){if(this===void 0||this===null)throw new TypeError;var c=Object(this),d=c.length>>>0,e=0,f;if(typeof b!="function")throw new TypeError;if(d==0&&arguments.length==1)throw new TypeError;if(arguments.length>=2)f=arguments[1];else do{if(e in c){f=c[e++];break}if(++e>=d)throw new TypeError}while(!0);while(e<d)e in c&&(f=b.call(a,f,c[e],e,c)),e++;return f})})();var Zepto=function(){function A(a){return v.call(a)=="[object Function]"}function B(a){return a instanceof Object}function C(b){var c,d;if(v.call(b)!=="[object Object]")return!1;d=A(b.constructor)&&b.constructor.prototype;if(!d||!hasOwnProperty.call(d,"isPrototypeOf"))return!1;for(c in b);return c===a||hasOwnProperty.call(b,c)}function D(a){return a instanceof Array}function E(a){return typeof a.length=="number"}function F(b){return b.filter(function(b){return b!==a&&b!==null})}function G(a){return a.length>0?[].concat.apply([],a):a}function H(a){return a.replace(/::/g,"/").replace(/([A-Z]+)([A-Z][a-z])/g,"$1_$2").replace(/([a-z\d])([A-Z])/g,"$1_$2").replace(/_/g,"-").toLowerCase()}function I(a){return a in i?i[a]:i[a]=new RegExp("(^|\\s)"+a+"(\\s|$)")}function J(a,b){return typeof b=="number"&&!k[H(a)]?b+"px":b}function K(a){var b,c;return h[a]||(b=g.createElement(a),g.body.appendChild(b),c=j(b,"").getPropertyValue("display"),b.parentNode.removeChild(b),c=="none"&&(c="block"),h[a]=c),h[a]}function L(b,d){return d===a?c(b):c(b).filter(d)}function M(a,b,c,d){return A(b)?b.call(a,c,d):b}function N(a,b,d){var e=a%2?b:b.parentNode;e?e.insertBefore(d,a?a==1?e.firstChild:a==2?b:null:b.nextSibling):c(d).remove()}function O(a,b){b(a);for(var c in a.childNodes)O(a.childNodes[c],b)}var a,b,c,d,e=[],f=e.slice,g=window.document,h={},i={},j=g.defaultView.getComputedStyle,k={"column-count":1,columns:1,"font-weight":1,"line-height":1,opacity:1,"z-index":1,zoom:1},l=/^\s*<(\w+|!)[^>]*>/,m=[1,3,8,9,11],n=["after","prepend","before","append"],o=g.createElement("table"),p=g.createElement("tr"),q={tr:g.createElement("tbody"),tbody:o,thead:o,tfoot:o,td:p,th:p,"*":g.createElement("div")},r=/complete|loaded|interactive/,s=/^\.([\w-]+)$/,t=/^#([\w-]+)$/,u=/^[\w-]+$/,v={}.toString,w={},x,y,z=g.createElement("div");return w.matches=function(a,b){if(!a||a.nodeType!==1)return!1;var c=a.webkitMatchesSelector||a.mozMatchesSelector||a.oMatchesSelector||a.matchesSelector;if(c)return c.call(a,b);var d,e=a.parentNode,f=!e;return f&&(e=z).appendChild(a),d=~w.qsa(e,b).indexOf(a),f&&z.removeChild(a),d},x=function(a){return a.replace(/-+(.)?/g,function(a,b){return b?b.toUpperCase():""})},y=function(a){return a.filter(function(b,c){return a.indexOf(b)==c})},w.fragment=function(b,d){d===a&&(d=l.test(b)&&RegExp.$1),d in q||(d="*");var e=q[d];return e.innerHTML=""+b,c.each(f.call(e.childNodes),function(){e.removeChild(this)})},w.Z=function(a,b){return a=a||[],a.__proto__=arguments.callee.prototype,a.selector=b||"",a},w.isZ=function(a){return a instanceof w.Z},w.init=function(b,d){if(!b)return w.Z();if(A(b))return c(g).ready(b);if(w.isZ(b))return b;var e;if(D(b))e=F(b);else if(C(b))e=[c.extend({},b)],b=null;else if(m.indexOf(b.nodeType)>=0||b===window)e=[b],b=null;else if(l.test(b))e=w.fragment(b.trim(),RegExp.$1),b=null;else{if(d!==a)return c(d).find(b);e=w.qsa(g,b)}return w.Z(e,b)},c=function(a,b){return w.init(a,b)},c.extend=function(c){return f.call(arguments,1).forEach(function(d){for(b in d)d[b]!==a&&(c[b]=d[b])}),c},w.qsa=function(a,b){var c;return a===g&&t.test(b)?(c=a.getElementById(RegExp.$1))?[c]:e:a.nodeType!==1&&a.nodeType!==9?e:f.call(s.test(b)?a.getElementsByClassName(RegExp.$1):u.test(b)?a.getElementsByTagName(b):a.querySelectorAll(b))},c.isFunction=A,c.isObject=B,c.isArray=D,c.isPlainObject=C,c.inArray=function(a,b,c){return e.indexOf.call(b,a,c)},c.trim=function(a){return a.trim()},c.uuid=0,c.map=function(a,b){var c,d=[],e,f;if(E(a))for(e=0;e<a.length;e++)c=b(a[e],e),c!=null&&d.push(c);else for(f in a)c=b(a[f],f),c!=null&&d.push(c);return G(d)},c.each=function(a,b){var c,d;if(E(a)){for(c=0;c<a.length;c++)if(b.call(a[c],c,a[c])===!1)return a}else for(d in a)if(b.call(a[d],d,a[d])===!1)return a;return a},c.fn={forEach:e.forEach,reduce:e.reduce,push:e.push,indexOf:e.indexOf,concat:e.concat,map:function(a){return c.map(this,function(b,c){return a.call(b,c,b)})},slice:function(){return c(f.apply(this,arguments))},ready:function(a){return r.test(g.readyState)?a(c):g.addEventListener("DOMContentLoaded",function(){a(c)},!1),this},get:function(b){return b===a?f.call(this):this[b]},toArray:function(){return this.get()},size:function(){return this.length},remove:function(){return this.each(function(){this.parentNode!=null&&this.parentNode.removeChild(this)})},each:function(a){return this.forEach(function(b,c){a.call(b,c,b)}),this},filter:function(a){return c([].filter.call(this,function(b){return w.matches(b,a)}))},add:function(a,b){return c(y(this.concat(c(a,b))))},is:function(a){return this.length>0&&w.matches(this[0],a)},not:function(b){var d=[];if(A(b)&&b.call!==a)this.each(function(a){b.call(this,a)||d.push(this)});else{var e=typeof b=="string"?this.filter(b):E(b)&&A(b.item)?f.call(b):c(b);this.forEach(function(a){e.indexOf(a)<0&&d.push(a)})}return c(d)},eq:function(a){return a===-1?this.slice(a):this.slice(a,+a+1)},first:function(){var a=this[0];return a&&!B(a)?a:c(a)},last:function(){var a=this[this.length-1];return a&&!B(a)?a:c(a)},find:function(a){var b;return this.length==1?b=w.qsa(this[0],a):b=this.map(function(){return w.qsa(this,a)}),c(b)},closest:function(a,b){var d=this[0];while(d&&!w.matches(d,a))d=d!==b&&d!==g&&d.parentNode;return c(d)},parents:function(a){var b=[],d=this;while(d.length>0)d=c.map(d,function(a){if((a=a.parentNode)&&a!==g&&b.indexOf(a)<0)return b.push(a),a});return L(b,a)},parent:function(a){return L(y(this.pluck("parentNode")),a)},children:function(a){return L(this.map(function(){return f.call(this.children)}),a)},siblings:function(a){return L(this.map(function(a,b){return f.call(b.parentNode.children).filter(function(a){return a!==b})}),a)},empty:function(){return this.each(function(){this.innerHTML=""})},pluck:function(a){return this.map(function(){return this[a]})},show:function(){return this.each(function(){this.style.display=="none"&&(this.style.display=null),j(this,"").getPropertyValue("display")=="none"&&(this.style.display=K(this.nodeName))})},replaceWith:function(a){return this.before(a).remove()},wrap:function(a){return this.each(function(){c(this).wrapAll(c(a)[0].cloneNode(!1))})},wrapAll:function(a){return this[0]&&(c(this[0]).before(a=c(a)),a.append(this)),this},unwrap:function(){return this.parent().each(function(){c(this).replaceWith(c(this).children())}),this},clone:function(){return c(this.map(function(){return this.cloneNode(!0)}))},hide:function(){return this.css("display","none")},toggle:function(b){return(b===a?this.css("display")=="none":b)?this.show():this.hide()},prev:function(){return c(this.pluck("previousElementSibling"))},next:function(){return c(this.pluck("nextElementSibling"))},html:function(b){return b===a?this.length>0?this[0].innerHTML:null:this.each(function(a){var d=this.innerHTML;c(this).empty().append(M(this,b,a,d))})},text:function(b){return b===a?this.length>0?this[0].textContent:null:this.each(function(){this.textContent=b})},attr:function(c,d){var e;return typeof c=="string"&&d===a?this.length==0||this[0].nodeType!==1?a:c=="value"&&this[0].nodeName=="INPUT"?this.val():!(e=this[0].getAttribute(c))&&c in this[0]?this[0][c]:e:this.each(function(a){if(this.nodeType!==1)return;if(B(c))for(b in c)this.setAttribute(b,c[b]);else this.setAttribute(c,M(this,d,a,this.getAttribute(c)))})},removeAttr:function(a){return this.each(function(){this.nodeType===1&&this.removeAttribute(a)})},prop:function(b,c){return c===a?this[0]?this[0][b]:a:this.each(function(a){this[b]=M(this,c,a,this[b])})},data:function(b,c){var d=this.attr("data-"+H(b),c);return d!==null?d:a},val:function(b){return b===a?this.length>0?this[0].value:a:this.each(function(a){this.value=M(this,b,a,this.value)})},offset:function(){if(this.length==0)return null;var a=this[0].getBoundingClientRect();return{left:a.left+window.pageXOffset,top:a.top+window.pageYOffset,width:a.width,height:a.height}},css:function(c,d){if(d===a&&typeof c=="string")return this.length==0?a:this[0].style[x(c)]||j(this[0],"").getPropertyValue(c);var e="";for(b in c)typeof c[b]=="string"&&c[b]==""?this.each(function(){this.style.removeProperty(H(b))}):e+=H(b)+":"+J(b,c[b])+";";return typeof c=="string"&&(d==""?this.each(function(){this.style.removeProperty(H(c))}):e=H(c)+":"+J(c,d)),this.each(function(){this.style.cssText+=";"+e})},index:function(a){return a?this.indexOf(c(a)[0]):this.parent().children().indexOf(this[0])},hasClass:function(a){return this.length<1?!1:I(a).test(this[0].className)},addClass:function(a){return this.each(function(b){d=[];var e=this.className,f=M(this,a,b,e);f.split(/\s+/g).forEach(function(a){c(this).hasClass(a)||d.push(a)},this),d.length&&(this.className+=(e?" ":"")+d.join(" "))})},removeClass:function(b){return this.each(function(c){if(b===a)return this.className="";d=this.className,M(this,b,c,d).split(/\s+/g).forEach(function(a){d=d.replace(I(a)," ")}),this.className=d.trim()})},toggleClass:function(b,d){return this.each(function(e){var f=M(this,b,e,this.className);(d===a?!c(this).hasClass(f):d)?c(this).addClass(f):c(this).removeClass(f)})}},["width","height"].forEach(function(b){c.fn[b]=function(d){var e,f=b.replace(/./,function(a){return a[0].toUpperCase()});return d===a?this[0]==window?window["inner"+f]:this[0]==g?g.documentElement["offset"+f]:(e=this.offset())&&e[b]:this.each(function(a){var e=c(this);e.css(b,M(this,d,a,e[b]()))})}}),n.forEach(function(a,b){c.fn[a]=function(){var a=c.map(arguments,function(a){return B(a)?a:w.fragment(a)});if(a.length<1)return this;var d=this.length,e=d>1,f=b<2;return this.each(function(c,g){for(var h=0;h<a.length;h++){var i=a[f?a.length-h-1:h];O(i,function(a){a.nodeName!=null&&a.nodeName.toUpperCase()==="SCRIPT"&&(!a.type||a.type==="text/javascript")&&window.eval.call(window,a.innerHTML)}),e&&c<d-1&&(i=i.cloneNode(!0)),N(b,g,i)}})},c.fn[b%2?a+"To":"insert"+(b?"Before":"After")]=function(b){return c(b)[a](this),this}}),w.Z.prototype=c.fn,w.camelize=x,w.uniq=y,c.zepto=w,c}();window.Zepto=Zepto,"$"in window||(window.$=Zepto),function(a){function f(a){return a._zid||(a._zid=d++)}function g(a,b,d,e){b=h(b);if(b.ns)var g=i(b.ns);return(c[f(a)]||[]).filter(function(a){return a&&(!b.e||a.e==b.e)&&(!b.ns||g.test(a.ns))&&(!d||f(a.fn)===f(d))&&(!e||a.sel==e)})}function h(a){var b=(""+a).split(".");return{e:b[0],ns:b.slice(1).sort().join(" ")}}function i(a){return new RegExp("(?:^| )"+a.replace(" "," .* ?")+"(?: |$)")}function j(b,c,d){a.isObject(b)?a.each(b,d):b.split(/\s/).forEach(function(a){d(a,c)})}function k(b,d,e,g,i,k){k=!!k;var l=f(b),m=c[l]||(c[l]=[]);j(d,e,function(c,d){var e=i&&i(d,c),f=e||d,j=function(a){var c=f.apply(b,[a].concat(a.data));return c===!1&&a.preventDefault(),c},l=a.extend(h(c),{fn:d,proxy:j,sel:g,del:e,i:m.length});m.push(l),b.addEventListener(l.e,j,k)})}function l(a,b,d,e){var h=f(a);j(b||"",d,function(b,d){g(a,b,d,e).forEach(function(b){delete c[h][b.i],a.removeEventListener(b.e,b.proxy,!1)})})}function p(b){var c=a.extend({originalEvent:b},b);return a.each(o,function(a,d){c[a]=function(){return this[d]=m,b[a].apply(b,arguments)},c[d]=n}),c}function q(a){if(!("defaultPrevented"in a)){a.defaultPrevented=!1;var b=a.preventDefault;a.preventDefault=function(){this.defaultPrevented=!0,b.call(this)}}}var b=a.zepto.qsa,c={},d=1,e={};e.click=e.mousedown=e.mouseup=e.mousemove="MouseEvents",a.event={add:k,remove:l},a.proxy=function(b,c){if(a.isFunction(b)){var d=function(){return b.apply(c,arguments)};return d._zid=f(b),d}if(typeof c=="string")return a.proxy(b[c],b);throw new TypeError("expected function")},a.fn.bind=function(a,b){return this.each(function(){k(this,a,b)})},a.fn.unbind=function(a,b){return this.each(function(){l(this,a,b)})},a.fn.one=function(a,b){return this.each(function(c,d){k(this,a,b,null,function(a,b){return function(){var c=a.apply(d,arguments);return l(d,b,a),c}})})};var m=function(){return!0},n=function(){return!1},o={preventDefault:"isDefaultPrevented",stopImmediatePropagation:"isImmediatePropagationStopped",stopPropagation:"isPropagationStopped"};a.fn.delegate=function(b,c,d){var e=!1;if(c=="blur"||c=="focus")a.iswebkit?c=c=="blur"?"focusout":c=="focus"?"focusin":c:e=!0;return this.each(function(f,g){k(g,c,d,b,function(c){return function(d){var e,f=a(d.target).closest(b,g).get(0);if(f)return e=a.extend(p(d),{currentTarget:f,liveFired:g}),c.apply(f,[e].concat([].slice.call(arguments,1)))}},e)})},a.fn.undelegate=function(a,b,c){return this.each(function(){l(this,b,c,a)})},a.fn.live=function(b,c){return a(document.body).delegate(this.selector,b,c),this},a.fn.die=function(b,c){return a(document.body).undelegate(this.selector,b,c),this},a.fn.on=function(b,c,d){return c==undefined||a.isFunction(c)?this.bind(b,c):this.delegate(c,b,d)},a.fn.off=function(b,c,d){return c==undefined||a.isFunction(c)?this.unbind(b,c):this.undelegate(c,b,d)},a.fn.trigger=function(b,c){return typeof b=="string"&&(b=a.Event(b)),q(b),b.data=c,this.each(function(){"dispatchEvent"in this&&this.dispatchEvent(b)})},a.fn.triggerHandler=function(b,c){var d,e;return this.each(function(f,h){d=p(typeof b=="string"?a.Event(b):b),d.data=c,d.target=h,a.each(g(h,b.type||b),function(a,b){e=b.proxy(d);if(d.isImmediatePropagationStopped())return!1})}),e},"focusin focusout load resize scroll unload click dblclick mousedown mouseup mousemove mouseover mouseout change select keydown keypress keyup error".split(" ").forEach(function(b){a.fn[b]=function(a){return this.bind(b,a)}}),["focus","blur"].forEach(function(b){a.fn[b]=function(a){if(a)this.bind(b,a);else if(this.length)try{this.get(0)[b]()}catch(c){}return this}}),a.Event=function(a,b){var c=document.createEvent(e[a]||"Events"),d=!0;if(b)for(var f in b)f=="bubbles"?d=!!b[f]:c[f]=b[f];return c.initEvent(a,d,!0,null,null,null,null,null,null,null,null,null,null,null,null),c}}(Zepto),function(a){function b(a){var b=this.os={},c=this.browser={},d=a.match(/WebKit\/([\d.]+)/),e=a.match(/(Android)\s+([\d.]+)/),f=a.match(/(iPad).*OS\s([\d_]+)/),g=!f&&a.match(/(iPhone\sOS)\s([\d_]+)/),h=a.match(/(webOS|hpwOS)[\s\/]([\d.]+)/),i=h&&a.match(/TouchPad/),j=a.match(/Kindle\/([\d.]+)/),k=a.match(/Silk\/([\d._]+)/),l=a.match(/(BlackBerry).*Version\/([\d.]+)/);if(c.webkit=!!d)c.version=d[1];e&&(b.android=!0,b.version=e[2]),g&&(b.ios=b.iphone=!0,b.version=g[2].replace(/_/g,".")),f&&(b.ios=b.ipad=!0,b.version=f[2].replace(/_/g,".")),h&&(b.webos=!0,b.version=h[2]),i&&(b.touchpad=!0),l&&(b.blackberry=!0,b.version=l[2]),j&&(b.kindle=!0,b.version=j[1]),k&&(c.silk=!0,c.version=k[1]),!k&&b.android&&a.match(/Kindle Fire/)&&(c.silk=!0)}b.call(a,navigator.userAgent),a.__detect=b}(Zepto),function(a,b){function l(a){return a.toLowerCase()}function m(a){return d?d+a:l(a)}var c="",d,e,f,g={Webkit:"webkit",Moz:"",O:"o",ms:"MS"},h=window.document,i=h.createElement("div"),j=/^((translate|rotate|scale)(X|Y|Z|3d)?|matrix(3d)?|perspective|skew(X|Y)?)$/i,k={};a.each(g,function(a,e){if(i.style[a+"TransitionProperty"]!==b)return c="-"+l(a)+"-",d=e,!1}),k[c+"transition-property"]=k[c+"transition-duration"]=k[c+"transition-timing-function"]=k[c+"animation-name"]=k[c+"animation-duration"]="",a.fx={off:d===b&&i.style.transitionProperty===b,cssPrefix:c,transitionEnd:m("TransitionEnd"),animationEnd:m("AnimationEnd")},a.fn.animate=function(b,c,d,e){return a.isObject(c)&&(d=c.easing,e=c.complete,c=c.duration),c&&(c/=1e3),this.anim(b,c,d,e)},a.fn.anim=function(d,e,f,g){var h,i={},l,m=this,n,o=a.fx.transitionEnd;e===b&&(e=.4),a.fx.off&&(e=0);if(typeof d=="string")i[c+"animation-name"]=d,i[c+"animation-duration"]=e+"s",o=a.fx.animationEnd;else{for(l in d)j.test(l)?(h||(h=[]),h.push(l+"("+d[l]+")")):i[l]=d[l];h&&(i[c+"transform"]=h.join(" ")),!a.fx.off&&typeof d=="object"&&(i[c+"transition-property"]=Object.keys(d).join(", "),i[c+"transition-duration"]=e+"s",i[c+"transition-timing-function"]=f||"linear")}return n=function(b){if(typeof b!="undefined"){if(b.target!==b.currentTarget)return;a(b.target).unbind(o,arguments.callee)}a(this).css(k),g&&g.call(this)},e>0&&this.bind(o,n),setTimeout(function(){m.css(i),e<=0&&setTimeout(function(){m.each(function(){n.call(this)})},0)},0),this},i=null}(Zepto),function($){function triggerAndReturn(a,b,c){var d=$.Event(b);return $(a).trigger(d,c),!d.defaultPrevented}function triggerGlobal(a,b,c,d){if(a.global)return triggerAndReturn(b||document,c,d)}function ajaxStart(a){a.global&&$.active++===0&&triggerGlobal(a,null,"ajaxStart")}function ajaxStop(a){a.global&&!--$.active&&triggerGlobal(a,null,"ajaxStop")}function ajaxBeforeSend(a,b){var c=b.context;if(b.beforeSend.call(c,a,b)===!1||triggerGlobal(b,c,"ajaxBeforeSend",[a,b])===!1)return!1;triggerGlobal(b,c,"ajaxSend",[a,b])}function ajaxSuccess(a,b,c){var d=c.context,e="success";c.success.call(d,a,e,b),triggerGlobal(c,d,"ajaxSuccess",[b,c,a]),ajaxComplete(e,b,c)}function ajaxError(a,b,c,d){var e=d.context;d.error.call(e,c,b,a),triggerGlobal(d,e,"ajaxError",[c,d,a]),ajaxComplete(b,c,d)}function ajaxComplete(a,b,c){var d=c.context;c.complete.call(d,b,a),triggerGlobal(c,d,"ajaxComplete",[b,c]),ajaxStop(c)}function empty(){}function mimeToDataType(a){return a&&(a==htmlType?"html":a==jsonType?"json":scriptTypeRE.test(a)?"script":xmlTypeRE.test(a)&&"xml")||"text"}function appendQuery(a,b){return(a+"&"+b).replace(/[&?]{1,2}/,"?")}function serializeData(a){isObject(a.data)&&(a.data=$.param(a.data)),a.data&&(!a.type||a.type.toUpperCase()=="GET")&&(a.url=appendQuery(a.url,a.data))}function serialize(a,b,c,d){var e=$.isArray(b);$.each(b,function(b,f){d&&(b=c?d:d+"["+(e?"":b)+"]"),!d&&e?a.add(f.name,f.value):(c?$.isArray(f):isObject(f))?serialize(a,f,c,b):a.add(b,f)})}var jsonpID=0,isObject=$.isObject,document=window.document,key,name,rscript=/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi,scriptTypeRE=/^(?:text|application)\/javascript/i,xmlTypeRE=/^(?:text|application)\/xml/i,jsonType="application/json",htmlType="text/html",blankRE=/^\s*$/;$.active=0,$.ajaxJSONP=function(a){var b="jsonp"+ ++jsonpID,c=document.createElement("script"),d=function(){$(c).remove(),b in window&&(window[b]=empty),ajaxComplete("abort",e,a)},e={abort:d},f;return a.error&&(c.onerror=function(){e.abort(),a.error()}),window[b]=function(d){clearTimeout(f),$(c).remove(),delete window[b],ajaxSuccess(d,e,a)},serializeData(a),c.src=a.url.replace(/=\?/,"="+b),$("head").append(c),a.timeout>0&&(f=setTimeout(function(){e.abort(),ajaxComplete("timeout",e,a)},a.timeout)),e},$.ajaxSettings={type:"GET",beforeSend:empty,success:empty,error:empty,complete:empty,context:null,global:!0,xhr:function(){return new window.XMLHttpRequest},accepts:{script:"text/javascript, application/javascript",json:jsonType,xml:"application/xml, text/xml",html:htmlType,text:"text/plain"},crossDomain:!1,timeout:0},$.ajax=function(options){var settings=$.extend({},options||{});for(key in $.ajaxSettings)settings[key]===undefined&&(settings[key]=$.ajaxSettings[key]);ajaxStart(settings),settings.crossDomain||(settings.crossDomain=/^([\w-]+:)?\/\/([^\/]+)/.test(settings.url)&&RegExp.$2!=window.location.host);var dataType=settings.dataType,hasPlaceholder=/=\?/.test(settings.url);if(dataType=="jsonp"||hasPlaceholder)return hasPlaceholder||(settings.url=appendQuery(settings.url,"callback=?")),$.ajaxJSONP(settings);settings.url||(settings.url=window.location.toString()),serializeData(settings);var mime=settings.accepts[dataType],baseHeaders={},protocol=/^([\w-]+:)\/\//.test(settings.url)?RegExp.$1:window.location.protocol,xhr=$.ajaxSettings.xhr(),abortTimeout;settings.crossDomain||(baseHeaders["X-Requested-With"]="XMLHttpRequest"),mime&&(baseHeaders.Accept=mime,mime.indexOf(",")>-1&&(mime=mime.split(",",2)[0]),xhr.overrideMimeType&&xhr.overrideMimeType(mime));if(settings.contentType||settings.data&&settings.type.toUpperCase()!="GET")baseHeaders["Content-Type"]=settings.contentType||"application/x-www-form-urlencoded";settings.headers=$.extend(baseHeaders,settings.headers||{}),xhr.onreadystatechange=function(){if(xhr.readyState==4){clearTimeout(abortTimeout);var result,error=!1;if(xhr.status>=200&&xhr.status<300||xhr.status==304||xhr.status==0&&protocol=="file:"){dataType=dataType||mimeToDataType(xhr.getResponseHeader("content-type")),result=xhr.responseText;try{dataType=="script"?(1,eval)(result):dataType=="xml"?result=xhr.responseXML:dataType=="json"&&(result=blankRE.test(result)?null:JSON.parse(result))}catch(e){error=e}error?ajaxError(error,"parsererror",xhr,settings):ajaxSuccess(result,xhr,settings)}else ajaxError(null,"error",xhr,settings)}};var async="async"in settings?settings.async:!0;xhr.open(settings.type,settings.url,async);for(name in settings.headers)xhr.setRequestHeader(name,settings.headers[name]);return ajaxBeforeSend(xhr,settings)===!1?(xhr.abort(),!1):(settings.timeout>0&&(abortTimeout=setTimeout(function(){xhr.onreadystatechange=empty,xhr.abort(),ajaxError(null,"timeout",xhr,settings)},settings.timeout)),xhr.send(settings.data?settings.data:null),xhr)},$.get=function(a,b){return $.ajax({url:a,success:b})},$.post=function(a,b,c,d){return $.isFunction(b)&&(d=d||c,c=b,b=null),$.ajax({type:"POST",url:a,data:b,success:c,dataType:d})},$.getJSON=function(a,b){return $.ajax({url:a,success:b,dataType:"json"})},$.fn.load=function(a,b){if(!this.length)return this;var c=this,d=a.split(/\s/),e;return d.length>1&&(a=d[0],e=d[1]),$.get(a,function(a){c.html(e?$(document.createElement("div")).html(a.replace(rscript,"")).find(e).html():a),b&&b.call(c)}),this};var escape=encodeURIComponent;$.param=function(a,b){var c=[];return c.add=function(a,b){this.push(escape(a)+"="+escape(b))},serialize(c,a,b),c.join("&").replace("%20","+")}}(Zepto),function(a){a.fn.serializeArray=function(){var b=[],c;return a(Array.prototype.slice.call(this.get(0).elements)).each(function(){c=a(this);var d=c.attr("type");this.nodeName.toLowerCase()!="fieldset"&&!this.disabled&&d!="submit"&&d!="reset"&&d!="button"&&(d!="radio"&&d!="checkbox"||this.checked)&&b.push({name:c.attr("name"),value:c.val()})}),b},a.fn.serialize=function(){var a=[];return this.serializeArray().forEach(function(b){a.push(encodeURIComponent(b.name)+"="+encodeURIComponent(b.value))}),a.join("&")},a.fn.submit=function(b){if(b)this.bind("submit",b);else if(this.length){var c=a.Event("submit");this.eq(0).trigger(c),c.defaultPrevented||this.get(0).submit()}return this}}(Zepto),function(a){function d(a){return"tagName"in a?a:a.parentNode}function e(a,b,c,d){var e=Math.abs(a-b),f=Math.abs(c-d);return e>=f?a-b>0?"Left":"Right":c-d>0?"Up":"Down"}function h(){g=null,b.last&&(b.el.trigger("longTap"),b={})}function i(){g&&clearTimeout(g),g=null}var b={},c,f=750,g;a(document).ready(function(){var j,k;a(document.body).bind("touchstart",function(e){j=Date.now(),k=j-(b.last||j),b.el=a(d(e.touches[0].target)),c&&clearTimeout(c),b.x1=e.touches[0].pageX,b.y1=e.touches[0].pageY,k>0&&k<=250&&(b.isDoubleTap=!0),b.last=j,g=setTimeout(h,f)}).bind("touchmove",function(a){i(),b.x2=a.touches[0].pageX,b.y2=a.touches[0].pageY}).bind("touchend",function(a){i(),b.isDoubleTap?(b.el.trigger("doubleTap"),b={}):b.x2&&Math.abs(b.x1-b.x2)>30||b.y2&&Math.abs(b.y1-b.y2)>30?(b.el.trigger("swipe")&&b.el.trigger("swipe"+e(b.x1,b.x2,b.y1,b.y2)),b={}):"last"in b&&(b.el.trigger("tap"),c=setTimeout(function(){c=null,b.el.trigger("singleTap"),b={}},250))}).bind("touchcancel",function(){c&&clearTimeout(c),g&&clearTimeout(g),g=c=null,b={}})}),["swipe","swipeLeft","swipeRight","swipeUp","swipeDown","doubleTap","tap","singleTap","longTap"].forEach(function(b){a.fn[b]=function(a){return this.bind(b,a)}})}(Zepto);
</script>
<script type="text/javascript">
	$(function(){
		var g_exam_info = <%=exam_info%> || {};
		var g_exam_share = <%=exam_share%> || {};
		var g_questions = {};
		var sharescore;
		var g_baseroot = '<%=contextPath%>' || '';
		g_exam_info.user_answer = g_exam_info.user_answer || [];
		g_exam_info.question = g_exam_info.question || [];
		g_exam_info.user_answer = g_exam_info.user_answer || {};
		g_exam_info.user_result = g_exam_info.user_result || {};
		if(!!!g_exam_info.is_join){
			$(".main_wrap").hide();
			$(".lose-tip").show();
		}
		
		if(g_exam_info.state === 'EXAM_RUNNING'){
			loadQuestions();
			if(!!g_exam_info.user_result && !!g_exam_info.user_result.submit_time){
				loadResults(g_exam_info.user_answer,g_exam_info.user_result);
			}else{
				var answers = g_exam_info.user_answer;
				for(var i=0,len=answers.length;i<len;i++){
					var opts = answers[i].answer_option_id;
					for(var j=0;j<opts.length;j++){
						var item = $(".item-list .item[qid='"+answers[i].question_id+"']").find(".option-item[opid='"+opts[j]+"']");
						item.parents(".item").attr("do","true");
						if(item.hasClass("radio")){
							item.addClass("radio-checked");
						}else if(item.hasClass("checkbox")){
							item.addClass("checkbox-checked");
						}else{
							item.addClass("tf-checked");
						}
					}
				}
				/*if($(".item[do=false]").length>0){
		        	$(".btn-wrap .btn").addClass("btn-disable");
		        }else{
		        	$(".btn-wrap .btn").removeClass("btn-disable");
		        }*/
				$(".btn-wrap").show();
				$(document).on('click','.radio',function(){
					var $this = $(this);
			        if(!$this.hasClass('radio-checked')){
				        var answer = $(this).parents(".item").attr("qid") + ',' + $(this).attr("opid");
				        saveAnswer(answer,function(){
				        	// SUCC TO DO
				        });
				        $this.siblings('.radio-checked').removeClass('radio-checked');
			            $this.addClass('radio-checked');
			            $this.parents(".item").attr("do","true").removeClass('warning-border');
			        }
			    });
				$(document).on('click','.tf',function(){
					var $this = $(this);
			        if(!$this.hasClass('tf-checked')){
				        var answer = $(this).parents(".item").attr("qid") + ',' + $(this).attr("opid");
				        saveAnswer(answer,function(){
				        	// SUCC TO DO
				        });
				        $this.siblings('.tf-checked').removeClass('tf-checked');
			            $this.addClass('tf-checked');
			            $this.parents(".item").attr("do","true").removeClass('warning-border');
			        }
			    });
			    $(document).on('click','.checkbox',function(){
			    	var $this = $(this);
			        var answer = [$(this).parents(".item").attr("qid")];
			        $(this).siblings(".checkbox-checked").each(function(){
			        	answer.push($(this).attr("opid"));
			        });
			        if(!$this.hasClass("checkbox-checked")){
			        	answer.push($this.attr("opid"));	
			        }	        
			        saveAnswer(answer.join(","),function(){
				        //SUCC TO DO
			        });
			        if(!$this.hasClass('checkbox-checked')){
			            $this.addClass('checkbox-checked');
			        }else{
			            $this.removeClass('checkbox-checked');
			        }
			        if(answer.length == 1){
			        	$this.parents(".item").attr("do","false");
			        }else{
			        	$this.parents(".item").attr("do","true").removeClass('warning-border');
			        }
			    });
			    
			    $(document).on("click",".btn-wrap .btn",function(){
			    	if($(this).hasClass("btn-disable")) return;
			    	if($(".item[do=false]").length>0){
			    		showMsg('您还有未答完的考题，请完成后交卷~');
			    		$(".item[do=false]").addClass('warning-border');
			    		document.body.scrollTop = $(".item[do='false']").first().offset().top;
			    		return false;
			        }
			    	$(this).addClass("btn-disable");
			    	var answers = [];
			    	$(".item").each(function(){
			    		var istr = [$(this).attr("qid")];
			    		$(this).find(".radio-checked,.checkbox-checked,.tf-checked").each(function(){
			    			istr.push($(this).attr("opid"));
			    		});
			    		answers.push(istr.join(','));
			    	});
			    	
			    	$.ajax({
			    		type: 'post',
			    		url: g_baseroot + '/api/exam/submit_exam.json',
			    		data: {
			    			exam_id: g_exam_info.exam.exam_id,
			    			answer: answers.join('|')
			    		},
			    		dataType: 'json',
			    		success: function(result){
			    			if(result.result === 'SUCC'){
			    				$(".main_wrap").hide();
			    				sharescore=result.score;
			    				location.href = location.href;
			    				sharespage();
			    			}else if(result.result === "FAIL_EXAM_CLOSED"){
			    				showMsg(result.fail_text,function(){
			    					location.href = location.href;
			    				});
			    			}else{
			    				showMsg(result.fail_text);
			    				$(this).removeClass("btn-disable");
			    			}
			    		},
						error: function(e){
			    			showMsg('网络连接失败，请稍后再试');
			    			$(this).removeClass("btn-disable");
			    		}
			    	});
			    });
			}
		}else if(g_exam_info.state === 'EXAM_NOT_START'){
			$(".main_wrap").hide();
			$("body").css("background-color","#3e98ad");
			$(".wait_wrap").show();
		}else if(g_exam_info.state === 'EXAM_FINISH'){
			$(".main_wrap").hide();
			loadQuestions();
			loadResults(g_exam_info.user_answer,g_exam_info.user_result);
			sharespage();
		}
		$(document).on('click','.lookover',function(){
			$('.share_body').hide();
			$('.main_wrap').show();
		})
		$(document).on('click','.shares',function(){
			location.href="../static/ceshishare.html?name=123,sharescore=100"
		})
		function GetQueryString(name) {
   			var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)","i");
   			var r = window.location.search.substr(1).match(reg);
   			if (r!=null) return (r[2]); return null;
		}
		function sharespage(){
			var pass_mark;
			//=GetQueryString("passscore")
			var show_result=g_exam_info.exam.show_result || 'NONE';
			var states=g_exam_info.state;
			var pass;
			$('.showquestion').hide();
			$('.sharebtn').show();
			if(show_result=="AFTER_EXAM_END")
			{
			if(states=="EXAM_FINISH"){
				$('.showquestion').hide();
				$('.sharebtn').show();
				}
			else{
				$('.showquestion').show();
				$('.sharebtn').hide();
			}
			}
			else{
				$('.showquestion').hide();
				$('.sharebtn').show();
			}
			
			$('.shareuser img')[0].src=g_exam_share.avatar_url || "../static/images/wz_contact_icon_user.png";
			$('.shareuser img').one("error",function(e){
				$(this).attr('src','../static/images/wz_contact_icon_user.png');
			})
			if(typeof(sharescore)=="undefined"){
				$('.shareuser span').html(g_exam_share.user_name+":缺考&nbsp(未通过)");
			}
			else{
				pass_mark=g_exam_info.exam.pass_mark;
				if(sharescore>=pass_mark){
					pass="(通过)";
				}
				else{
					pass="(未通过)"
				}
				$('.shareuser span').html(g_exam_share.user_name+":"+sharescore+"分&nbsp"+pass);
			}
			
			//alert(sharescore);
			if(typeof(sharescore)=="undefined"){
				$('.renwuimg')[0].src="../static/images/renwuquekao.png";
				$('.sharewordimg')[0].src="../static/images/quekao.png";
			}
			else if(sharescore==100){
				$('.renwuimg')[0].src="../static/images/xueshenrenwu1.png";
				$('.sharewordimg')[0].src="../static/images/xueshen.png";
			}
			else if(sharescore>=80){
				$('.renwuimg')[0].src="../static/images/xuebarenwu.png";
				$('.sharewordimg')[0].src="../static/images/xueba.png";
			}
			else if(sharescore>=60){
				$('.renwuimg')[0].src="../static/images/xueminrenwu1.png";
				$('.sharewordimg')[0].src="../static/images/xuemin.png";
			}
			else{
				$('.renwuimg')[0].src="../static/images/xuezharenwu.png";
				$('.sharewordimg')[0].src="../static/images/xuezha.png";
			}
			

			$('.share_body').show();
		}
		function loadQuestions(){
			var html = [];
			var show_result=g_exam_info.exam.show_result || 'NONE';
			var states=g_exam_info.state;
			var questions = g_exam_info.question || [];
			for(var i=0,len=questions.length;i<len;i++){
				var question = questions[i];
				var qtype;  
				if(question.type == "OPTION_TF"){
					qtype = "tf";
				}else if(question.type == "OPTION_MULTI"){
					qtype = "checkbox";
				}else {
					qtype = "radio";
				}
				var options = question.option;
				var optHtml = [];
				g_questions["_"+question.question_id] = {};
				for(var n=0;n<options.length;n++){
					var succFlag = !!options[n].is_right;
					var rightAns = "";
					if(succFlag){
						if(show_result=="AFTER_EXAM_END")
							{
							if(states=="EXAM_FINISH"){
								rightAns = '<span class="right-answer">&nbsp;</span>';
								}
							}
						else if(show_result=="NONE"){
								rightAns="";
							}
						else{
								rightAns = '<span class="right-answer">&nbsp;</span>';
							}
					}
					if(qtype == "tf"){
						optHtml.push('<li class="option-item tf" opid="'+options[n].option_id+'"><a href="javascript:void(0)"><span>&nbsp;</span>'+options[n].option_name+rightAns+'</a></li>');
						g_questions["_"+question.question_id]["_"+options[n].option_id] = !!options[n].is_right;
					}else{
						optHtml.push('<li class="option-item '+qtype+'" opid="'+options[n].option_id+'"><a href="javascript:void(0)"><span>&nbsp;</span>'+options[n].option_name+rightAns+'</a></li>');
						g_questions["_"+question.question_id]["_"+options[n].option_id] = !!options[n].is_right;
					}
				}
				if(qtype == "tf"){
					html.push(['<div class="item" qid="',question.question_id,'" do="false"><div class="questions-tip">',(i+1),'</div><div class="questions">',("(判断)"),question.question_name,
						          '</div><div class="answers"><ul class="options">',optHtml.join(''),'</ul></div></div>'].join(''));
				}else{
				html.push(['<div class="item" qid="',question.question_id,'" do="false"><div class="questions-tip">',(i+1),'</div><div class="questions">',(qtype=="checkbox"?"(多选)":""),question.question_name,
				          '</div><div class="answers"><ul class="options">',optHtml.join(''),'</ul></div></div>'].join(''));
				}		
				}
			$(".item-list").html(html.join(''));
		}
		
		function loadResults(answers,result,show_result){
			var answers = g_exam_info.user_answer;
			//var show_result=g_exam_info.exam.show_result;
			var states=g_exam_info.state;
			//var abc=1;
			var questions = g_exam_info.question || [];
			var succFlag,abc=0;
			for(var z=0,lens=questions.length;z<lens;z++){
				var options=questions[z].option;
				
				succFlag = typeof(options[0].is_right);
				if(succFlag!="undefined"){
					abc=1;
				}
			}
				
					if(abc==0){
						$(".item").find(".questions").addClass("question-error");
						for(var i=0,len=answers.length;i<len;i++){
						var opts = answers[i].answer_option_id;
						
						var item = $(".item-list .item[qid='"+answers[i].question_id+"']");
						item.find(".questions").removeClass("question-error");
						if(!!!answers[i].is_right){
							item.find(".questions").addClass("question-error");
						}
						item.attr("do","true");
						for(var j=0;j<opts.length;j++){
							var li = item.find(".option-item[opid='"+opts[j]+"']");
							if(li.hasClass("radio")){
									li.addClass("radio-checked");
							}else if(li.hasClass("checkbox")){
									li.addClass("checkbox-checked");	
							}else{
								li.addClass("tf-checked");
							}
						}}
					}else{
						$(".item").find(".questions").addClass("question-error");
						for(var i=0,len=answers.length;i<len;i++){
						var opts = answers[i].answer_option_id;
						//$(".item").find(".questions").addClass("question-error");
						var item = $(".item-list .item[qid='"+answers[i].question_id+"']");
						item.find(".questions").removeClass("question-error");
						if(!!!answers[i].is_right){
							item.find(".questions").addClass("question-error");
						}
						item.attr("do","true");
						for(var j=0;j<opts.length;j++){
							var li = item.find(".option-item[opid='"+opts[j]+"']");
							if(li.hasClass("radio")){
								if(!!g_questions["_"+answers[i].question_id] && !!g_questions["_"+answers[i].question_id]["_"+opts[j]]){
									li.addClass("radio-suc");
								}else{
									li.addClass("radio-error");	
								}
							}else if(li.hasClass("checkbox")){
								if(!!g_questions["_"+answers[i].question_id] && !!g_questions["_"+answers[i].question_id]["_"+opts[j]]){
									li.addClass("checkbox-suc");
								}else{
									li.addClass("checkbox-error");	
								}
							}else{
								if(!!g_questions["_"+answers[i].question_id] && !!g_questions["_"+answers[i].question_id]["_"+opts[j]]){
									li.addClass("tf-suc");
								}else{
									li.addClass("tf-error");	
								}
							}
						}
					}}

			if(g_exam_info.state === 'EXAM_FINISH' && answers.length == 0 && typeof(result.score)=="undfine"){
				//alert(result.score);
				$(".main_wrap").hide();
				$(".result-score .score-info").html('<span>缺考</span>');
				sharescore=result.score;
				sharespage();
			}else{
				$(".main_wrap").hide();
				$(".item-list .item[do=false]").find(".questions").addClass("question-error");
				$(".result-score .score-info span").html(result.score || 0);
				sharescore=result.score;
				$(".result-score .other-info").html(!!result.submit_time?'':'未交卷');
				sharespage();
			}
			
			$(".result-score").show();
			$(".btn-wrap").hide();
		}
		
		function saveAnswer(answer,fun){
			fun = fun || function(){};
			$.ajax({
				type: 'post',
				url: g_baseroot + '/api/exam/save_answer.json',
				data: {
					exam_id: g_exam_info.exam.exam_id,
					answer: answer
				},
				dataType: 'json',
				success: function(result){
					if(result.result === "SUCC"){
						fun();
					}else if(result.result === "FAIL_EXAM_CLOSED"){
	    				showMsg(result.fail_text,function(){
	    					location.href = location.href;
	    				});
	    			}else{
						showMsg(result.fail_text);
					}
				},
				error:function(){
					showMsg('网络连接失败，请稍后再试');
				}
			});
		}
		
		function showMsg(msg,fun){
			fun = fun || function(){};
			$(".tip-alert .alert-msg").html(msg);
			$(".tip-alert").show();
			setTimeout(function(){
				$(".tip-alert").hide();
				fun();
			},2000)
		}
	});
</script>
</body>
</html>