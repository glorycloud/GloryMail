#!/usr/bin/bash -f
declare -a accounts
declare -a passwords
declare -a sids

accounts=("lmliu@cloudymail.mobi"	"liu_lele@139.com" "cloudymail@163.com")
passwords=("llm54321"				"mail2011"			"qhjy1436")

for (( i=0; i<${#accounts[@]}; i++ )) ; do
	u=${accounts[$i]}
	p=${passwords[$i]}
	echo $i " " $u
	
	iterate_attach.sh $u $p &
done 


