#!/bin/bash
if [ $# -lt 2 ] ; then
	echo "Usage: $0 outFile server1 server2 ..."
	exit 1
fi

oFile=$1
args=("$@")
if [ ! -f $oFile ] ; then
	echo "File not exist $1"
	count=1
	echo -n "	," > $oFile
	while [[ $count < $# ]]
	do
		echo -n "	${args[${count}]}, " >> $oFile
		(( count++ ))
	done
	echo >> $oFile
fi
count=1
echo -n "`date +%F` `date +%T`,">> $oFile
while [[ $count < $# ]]
do
	delay=`ping ${args[${count}]} -l 4096 |  awk -F= 'BEGIN{OFS=""; ORS=""} END{printf "%s", $4}' | tr -d '\r'`

	echo -n "	$delay, " >> $oFile
	(( count++ ))
done
echo >> $oFile
