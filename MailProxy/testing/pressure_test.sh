#!/usr/bin/bash -f
declare -a accounts
declare -a passwords
declare -a sids

accounts=("wewang@cloudymail.mobi"	"liulele@yahoo.com" "cloudymail@163.com")
passwords=("mail2011"				"cloudmail"			"qhjy1436")

for (( i=0; i<${#accounts[@]}; i++ )) ; do
	accounts[$i]=`echo ${accounts[$i]} | sed 's/@/%40/' ` 
	u=${accounts[$i]}
	p=${passwords[$i]}
	echo $i " " $u
	
# add argument '-s 2' can supress the time statistics
	
	sid=`curl -i "http://cloudymail.mobi:8088/MailProxy2/Login?account=$u&password=$p" | /usr/bin/grep Set-Cookie | awk -F: '{ print $2 } ' | awk -F\; '{ print $1 }' | awk -F= '{ print $2 }'`
	if [ "$sid" = "" ]; then
   		echo Login error for $u
   		exit 1
    fi
	sids[$i]=$sid
	echo "Session ID is "${sids[$i]}
done 

for (( i=0; i<${#accounts[@]}; i++ )) ; do
	sid=${sids[$i]}
	curl "http://cloudymail.mobi:8088/MailProxy2/SyncupMail?nm=0&dm=0&cn=1&mc=50&sid=$sid"
done 
