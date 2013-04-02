<%@ page contentType="text/html;charset=UTF-8"%>
<%@include file="/common/taglibs.jsp" %>
<% response.setHeader("remember", "true"); %>
<!DOCTYPE HTML>
<html lang="en">
	<head>
	 	<meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=2.0; user-scalable=0;">
	 	<meta name="apple-mobile-web-app-capable" content="yes"> 
	  	<meta name="apple-mobile-web-app-status-bar-style" content="black">  
	  	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  		<!--[if lt IE 9]><script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script><![endif]-->
 
		<title>Fun主题-商店更多</title>
        <link rel="stylesheet" href="${ctx}/static/styles/site.min.css" media="screen"/>
	</head>
	<body>
	
		<form action="home.action" method="get">
			<div class="categoryback">
				<h3><a href="home!category.action?${queryString}"><span style="margin-left: 30px;height: 53px;line-height: 53px;">${categoryName}</span></a></h3>
			</div>
			<div id="container"> 
				<s:iterator value="catePage.result">
					<div class="contents_info" id="content1" onclick="location.href='home!details.action?id=${theme.id}&${queryString}';">
						<div class="contents_image">						
							<img alt="${title}" onerror="this.src='${ctx}/static/images/default.png'" src="${ctx}/image.action?path=${theme.iconPath}" width="72" height="72" style="margin: 3px;">							
						</div>
						<div class="contents_txt">
							<div style="margin-top: 10px;">
								<span class="title">${title}</span>
								<p>${shortDescription}</p>
							</div>
						</div>
					</div>
				</s:iterator>
			</div>
			<nav id="page-nav">
  				<a href="?catePage.pageNo=${catePage.next}&cid=${categoryId}"></a>
			</nav>
		</form>
		<script src="${ctx}/static/jquery/1.7.2/jquery.min.js"></script>
		<script src="${ctx}/js/jquery/jquery.infinitescroll.min.js"></script>

  		<script>
		  $(function(){

              $(".contents_info").click(function(){
                  $(this).css("backgroundColor","#e7e6c8");
              });
		    var $container = $('#container');
		    $container.infinitescroll({
		      navSelector  : '#page-nav',    
		      nextSelector : '#page-nav a',  
		      itemSelector : '.contents_info',     
		      loading: {
		    	 	finished: undefined,
		         	finishedMsg:"<s:text name='home.finishedMsg'/>",
		         	msgText: "<s:text name='home.msgText'/>",
                    img: "data:image/gif;base64,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",
		            msg: null,
		            selector: null,
		            start: undefined
		        }
		      });
		  });
	</script>
	</body>
</html>