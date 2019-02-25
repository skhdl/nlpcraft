#
#  “Commons Clause” License, https://commonsclause.com/
#
#  The Software is provided to you by the Licensor under the License,
#  as defined below, subject to the following condition.
#
#  Without limiting other conditions in the License, the grant of rights
#  under the License will not include, and the License does not grant to
#  you, the right to Sell the Software.
#
#  For purposes of the foregoing, “Sell” means practicing any or all of
#  the rights granted to you under the License to provide to third parties,
#  for a fee or other consideration (including without limitation fees for
#  hosting or consulting/support services related to the Software), a
#  product or service whose value derives, entirely or substantially, from
#  the functionality of the Software. Any license notice or attribution
#  required by the License must also include this Commons Clause License
#  Condition notice.
#
#  Software:    NLPCraft
#  License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
#  Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
#
#      _   ____      ______           ______
#     / | / / /___  / ____/________ _/ __/ /_
#    /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
#   / /|  / / /_/ / /___/ /  / /_/ / __/ /_
#  /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
#         /_/
#
#!/bin/bash

if [[ $1 = "" ]] ; then
    echo "Version must be set as input parameter."
    exit -1
fi

dir=nlpcraft
zipDir=zips
zipPath=../${zipDir}
tmpDirPath=${zipPath}/${dir}
zipFilePath=${zipPath}/${dir}-$1.zip

rm -R ${zipPath} 2> /dev/null

mkdir ${zipPath}
mkdir ${tmpDirPath}

rsync -avzq ../ ${tmpDirPath} --exclude '**/.DS_Store' --exclude .github --exclude .git --exclude .idea --exclude target --exclude ${zipDir}

curDir=$(pwd)
cd ${zipPath}
zip -rq ${zipFilePath} ${dir} 2> /dev/null
cd ${curDir}
rm -R ${tmpDirPath} 2> /dev/null

shasum -a 1 ${zipFilePath} > ${zipFilePath}.sha1
shasum -a 256 ${zipFilePath} > ${zipFilePath}.sha256
md5 ${zipFilePath} > ${zipFilePath}.md5
gpg --detach-sign ${zipFilePath}

echo "Files prepared in folder: ${zipDir}"
