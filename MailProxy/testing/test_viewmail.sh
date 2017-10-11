#!/usr/bin/bash -f
declare -a accounts
declare -a passwords
declare -a sids

accounts=("cloudbug@163.com"	"cloudymail@163.com"	"liulele@ee.buaa.edu.cn"	"cocalele@msn.com"		"cocalele@hotmail.com"	"unittest2012@163.com"	"liulele@yahoo.com"		"wewang@cloudymail.mobi" "liu_lele@foxmail.com"  )
passwords=("qhjy1436"	        "qhjy1436"				"mail2011"					"mail2011"				"mail2011"				"abc2012"				"cloudmail"				"mail2011" 				"carrot"				)
for (( i=0; i<${#accounts[@]}; i++ )) ; do
	u=${accounts[$i]}
	p=${passwords[$i]}
	echo $i " " $u
	
	source iterate_mails.sh $u $p > $u.txt &
done 
wait

