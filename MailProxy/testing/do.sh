#!/usr/bin/bash

./test_viewmail.sh   # do test, save result to $SESSION_ID.txt

rm *_list.txt #delete tempory file
for f in `ls *.txt`; do iconv -f UTF-8 -t GB18030 $f > ${f/.txt/.csv} ;done #convert UTF8 to GB18030 and save to csv
for f in `ls *.csv`;do sed -i 's/Time(s):/Time(s):,/' $f;done #separate time lable and valut in different column
for f in `ls *.csv`;do sed -i '/^$/d' $f;done #delete empty line
for f in `ls *.csv`;do sed -i 's/^[^,]*//' $f;done #delete mail subject for privacy reason

