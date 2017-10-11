#!/usr/bin/bash -f
server=cloudymail.mobi
u=unittest2012@163.com
p=abc2012

#Step 1, do prepare, ask server to delete old account
result=`curl  -sL -w "%{http_code},\tTime(s):%{time_total},\tBytes Down:%{size_download}\\n"  "http://$server:8088/MailProxy2/unittest/Prepare"  -o /dev/null`
code=`echo $result | awk '{print $1}'`
if [ "$code" = "200," ]; then
	echo -e "Prepare: ${green}OK  ${endColor}" 
else
	echo -e "Prepare: ${red}FAIL${endColor}"
	return 1;
fi


u=`echo $u | sed 's/@/%40/' ` 

#step 2, login, to create account	
# add argument '-s 2' can supress the time statistics
	
sid=`curl  -H "Content-Type: text/xml" -i "http://$server:8088/MailProxy2/Login?account=$u" -d '
<account smtpServer="smtp.163.com" serverType="imap" loginName="unittest2012" mailServer="imap.163.com" name="unittest2012@163.com" password="abc2012" smtpPort="465" mailPort="993" useSSL="true"/>
' | /usr/bin/grep Set-Cookie | awk -F: '{ print $2 } ' | awk -F\; '{ print $1 }' | awk -F= '{ print $2 }'`
if [ "$sid" = "" ]; then
	echo -e "Login as New: ${red}FAIL${endColor}"
	return 1
else
	echo -e "Login as New: ${green}OK  ${endColor}"
fi

echo "Session ID is "$sid
#step 3, login as existing account, same as step 2, but server has already has that account
sid=`curl  -H "Content-Type: text/xml" -i "http://$server:8088/MailProxy2/Login?account=$u" -d '
<account smtpServer="smtp.163.com" serverType="imap" loginName="unittest2012" mailServer="imap.163.com" name="unittest2012@163.com" password="abc2012" smtpPort="465" mailPort="993" useSSL="true"/>
' | /usr/bin/grep Set-Cookie | awk -F: '{ print $2 } ' | awk -F\; '{ print $1 }' | awk -F= '{ print $2 }'`
if [ "$sid" = "" ]; then
	echo -e "Login as old: ${red}FAIL${endColor}"
	return 1
else
	echo -e "Login as old: ${green}OK  ${endColor}"
fi
echo "Session ID is "$sid

#step4 send mail use the exiting account
sid=`curl  -H "Content-Type: text/xml" -i "http://$server:8088/MailProxy2/SendMail?sid=$sid" -d '
<dataPacket packetType="newMail" refMailId="" quoteOld="false" forwardAttach="false">
   <attachments class="java.util.ArrayList">
      <attachmentInfo size="84B" canPreview="false">
         <body></body>
         <fileName>version.json</fileName>
         <index>-1</index>
      </attachmentInfo>
   </attachments>
   <bccList>cloudbug@163.com</bccList>
   <bodyText>Zhengwen </bodyText>
   <ccList>liu_lele@139.com</ccList>
   <toList>lmliu@cloudymail.mobi</toList>
   <subject>123 </subject>
</dataPacket>' | /usr/bin/grep Set-Cookie | awk -F: '{ print $2 } ' | awk -F\; '{ print $1 }' | awk -F= '{ print $2 }'`
if [ "$sid" = "" ]; then
	echo -e "SendMail : ${red}FAIL${endColor}"
	return 1
else
	echo -e "SendMail : ${green}OK  ${endColor}"
fi

echo "Session ID is "$sid
for folder in "INBOX" "ceshi" 
do
	#step 5, suncup mails	
	fileName=${sid}_list.txt
	curl "http://$server:8088/MailProxy2/SyncupMail?nm=0&dm=0&cn=1&mc=10&sid=$sid&foldername=$folder" > $fileName
		
	declare -a uids
	declare -a subjects
	declare -a dates
	export uids=(`/usr/bin/grep 'uid=' $fileName | awk '{print $2}' | awk -F\" '{print $2}'`)
	export subjects=(`/usr/bin/grep 'uid=' $fileName | awk -F\" '{ gsub(/ /, '_', $4); print $4}'`)
	export dates=(`/usr/bin/grep 'uid=' $fileName | awk -F\" '{ gsub(/ /, '_', $6); print $6}'`)
	declare -a unit_case_uids

	#step 6, open all mails
	for (( i=0; i<${#uids[@]}; i++ )) ; do
		subj=${subjects[$i]}
		d=${dates[$i]}
		uid=${uids[$i]}
		
		echo 
		echo -n $subj, $d, $uid,
		result=`curl  -sL -w "%{http_code},\tTime(s):%{time_total},\tBytes Down:%{size_download}\\n" -G --data-urlencode "uid=$uid" --data-urlencode "multipage=false" --data-urlencode "pageNo=0" --data-urlencode "folderName=$folder" --data-urlencode "sid=$sid" "http://$server:8088/MailProxy2/MailRender"  -o /dev/null`
		code=`echo $result | awk '{print $1}'`
		if [ "$code" = "200," ]; then
			echo -e "Open Mail: ${green}OK  ${endColor} $result" 
		else
			echo -e "Open Mail: ${red}FAIL${endColor} $result"
		fi
	
	#step 7, test attachment preview
		fileName=${sid}_${subj}_attch.txt
		curl "http://$server:8088/MailProxy2/AttachList?uid=$uid&sid=$sid&folderName=$folder"  > $fileName
		declare -a attach_indexs
		declare -a attach_names
		export attach_indexs=(`/usr/bin/grep '<attach' $fileName | awk '{print $2}' | awk -F\" '{print $2}'`)
		export attach_names=(`/usr/bin/grep '<attach' $fileName | awk -F\" '{ gsub(/ /, '_', $4); print $4}'`)
		
		
		for (( j=0; j<${#attach_indexs[@]}; j++ )) ; do
			aindex=${attach_indexs[$j]}
			aname=${attach_names[$j]}
			result=`curl  -sL -w "%{http_code}\tTime(s):%{time_total}\tBytes Down:%{size_download}\\n" "http://$server:8088/MailProxy2/ViewPart?uid=$uid&index=$aindex&pageNo=0&sid=$sid&folderName=$folder"  -o /dev/null`
			code=`echo $result | awk '{print $1}'`
			if [ "$code" = "200" ]; then
				echo -e "$aname page 1 :${green}OK  ${endColor} $result" 
			else
				echo -e "$aname page 1 :${red}FAIL${endColor} $result"
			fi
			result=`curl  -sL -w "%{http_code}\tTime(s):%{time_total}\tBytes Down:%{size_download}\\n" "http://$server:8088/MailProxy2/ViewPart?uid=$uid&index=$aindex&pageNo=1&sid=$sid&folderName=$folder"  -o /dev/null`
			code=`echo $result | awk '{print $1}'`
			if [ "$code" = "200" ]; then
				echo -e "$aname page 2 :${green}OK  ${endColor} $result" 
			else
				echo -e "$aname page 2 :${red}FAIL${endColor} $result"
			fi
		done
	done 
done
echo
echo DONE. ${#uids[@]} mails iterated.


