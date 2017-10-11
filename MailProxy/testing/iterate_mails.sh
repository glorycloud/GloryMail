#!/usr/bin/bash -f
server=localhost
declare -a uids
if [ -z "$2" ]; then 
      echo usage: $0 user pass
      return
fi
	u=$1
	p=$2

	u=`echo $u | sed 's/@/%40/' ` 
	
# add argument '-s 2' can supress the time statistics
	
	sid=`curl -i "http://$server:8088/MailProxy2/Login?account=$u&password=$p" | /usr/bin/grep Set-Cookie | awk -F: '{ print $2 } ' | awk -F\; '{ print $1 }' | awk -F= '{ print $2 }'`
	if [ "$sid" = "" ]; then
   		echo Login error for $u
   		return
    fi
	
	echo "Session ID is "$sid
	fileName=${sid}_list.txt
	curl "http://$server:8088/MailProxy2/SyncupMail?nm=0&dm=0&cn=1&mc=50&sid=$sid" > $fileName

	declare -a uids
	declare -a subjects
	declare -a dates
	export uids=(`/usr/bin/grep 'uid=' $fileName | awk '{print $2}' | awk -F\" '{print $2}'`)
	export subjects=(`/usr/bin/grep 'uid=' $fileName | awk -F\" '{ gsub(/ /, '_', $4); print $4}'`)
	export dates=(`/usr/bin/grep 'uid=' $fileName | awk -F\" '{ gsub(/ /, '_', $6); print $6}'`)

	for (( i=0; i<${#uids[@]}; i++ )) ; do
		subj=${subjects[$i]}
		d=${dates[$i]}
		uid=${uids[$i]}
		echo 
		echo -n $subj, $d, $uid,
		result=`curl  -sL -w "%{http_code},\tTime(s):%{time_total},\tBytes Down:%{size_download}\\n" "http://$server:8088/MailProxy2/MailRender?multipage=false&pageNo=0&sid=$sid&folderName=INBOX" --data-urlencode "uid=$uid" -o /dev/null`
		code=`echo $result | awk '{print $1}'`
		if [ "$code" = "200," ]; then
			echo -e "${green}OK  ${endColor} $result" 
		else
			echo -e "${red}FAIL${endColor} $result"
		fi
	done 

	echo
	echo DONE. ${#uids[@]} mails iterated.

#	for uid in $uids ; do
#		echo $uid
#		curl  -sL -w "%{http_code} %{url_effective}\\n" "http://$server:8088/MailProxy2/MailRender?uid=$uid&multipage=false&pageNo=0&sid=$sid"  -o /dev/null
#	done
