<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>


<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<script type="text/javascript" >
function docload(){
	<%Object pageNumber = request.getAttribute("pageNumber");
	  String strpageNo = pageNumber.toString();
	  int pageNo =Integer.parseInt(strpageNo); 
	  int totalPageCount=Integer.parseInt(request.getAttribute("totalPageCount").toString());
	  %>
    window.cmail.saveAttach(document.getElementsByTagName('html')[0].innerHTML,<%=pageNo%>,<%=totalPageCount%>);
    }
</script>
</head>
<body onLoad="docload()"> 
  <% Object attachBody = request.getAttribute("attachBody");
     String html = attachBody.toString();
     %>  
     <%=html %>
</body>
</html>