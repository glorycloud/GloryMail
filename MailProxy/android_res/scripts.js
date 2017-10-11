    var sticked = false;
    function scrollHandler () {
    	
	var scrolls = body.scrollTop;
	if (!sticked && scrolls >  subjectDiv.scrollHeight) {
		headerDiv.style.position="fixed";
	    headerDiv.style.top="0px";
	    scrollDiv.style.marginTop=headerDiv.scrollHeight;
	    sticked = true;
	}else if(sticked && scrolls < subjectDiv.scrollHeight) {
		headerDiv.style.position="relative";
		scrollDiv.style.marginTop="0px";
		sticked = false;
	}
	}
	
	var allVisible=false;
    var subjectHeight = 0;

	function showAllButton()
	{
		if(!allVisible)
		{
			textReplyall.style.display="inline";
			textReply.style.display="inline";
			textFwd.style.display="inline";
			fwdTd.style.display="inline";
			replyTd.style.display="inline";
			arrowBtn.src="/MailProxy2/android_res/shrink.png";
			allVisible = true;
		}
		else
		{
			textReplyall.style.display="none";
			textReply.style.display="none";
			textFwd.style.display="none";
			fwdTd.style.display="none";
			replyTd.style.display="none";
			arrowBtn.src="/MailProxy2/android_res/expand.png";
			allVisible = false;
		}
	}
	
	var reciptDetail = false;
	function toggleRecipt()
	{
		if(reciptDetail)
		{
			reciptDetailTbl.style.display="none";
			reciptBriefTbl.style.display="block";
			reciptDetail = false;
			
		}
		else
		{
			reciptDetailTbl.style.display="block";
			reciptBriefTbl.style.display="none";
			reciptDetail=true;
		}
		window.event.returnValue=false;
	}
	
	var hasStar = false;
	function setStarOn()
	{
			starImg.src="/MailProxy2/android_res/btn_star_big_buttonless_on.png";
			window.cmail.setStar("1");
			hasStar = true;
	}
	
	function setStarOff()
	{
		starImg.src="/MailProxy2/android_res/btn_star_big_buttonless_off.png";
		window.cmail.setStar("0");
		hasStar = false;
	}

	function toggoleStar()
	{
		if(hasStar)
			setStarOff();
		else
			setStarOn();
	}
	