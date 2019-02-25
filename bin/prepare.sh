#!/bin/bash

# name with version? location of sh? win? structure?
dir=nlpcraft
file=${dir}.zip

rm ${file} 2> /dev/null
rm ${file}*.* 2> /dev/null
rm -R ${dir} 2> /dev/null

mkdir nlpcraft

rsync -avzq ../ ${dir} --exclude '../.DS_Store' --exclude .github --exclude .git --exclude .idea --exclude target --exclude bin/${dir} --exclude bin/${file}

zip -r -uq ${file} ${dir} 2> /dev/null

rm -R ${dir} 2> /dev/null

shasum -a 1 ${file} > ${file}.sha1
shasum -a 256 ${file} > ${file}.sha256
md5 ${file} > ${file}.md5
gpg --detach-sign ${file}
