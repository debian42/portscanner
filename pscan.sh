#!/bin/bash
PARAS="-Dportscanner.autotune=true"
PARAS="$PARAS -Dportscanner.tcp.autotune.same.fd=2048"
PARAS="$PARAS -Dportscanner.tcp.autotune.same.wt=40" # Set it to 1000, if you scan a slow device
PARAS="$PARAS -Dportscanner.tcp.autotune.notsame.fd=512"
PARAS="$PARAS -Dportscanner.tcp.autotune.notsame.wt=512"
PARAS="$PARAS -Dportscanner.tcp.connectscan=true"
PARAS="$PARAS -Dportscanner.tcp.connectscan.threads=16"
PARAS="$PARAS -Dportscanner.tcp.sockbuf=1024"
PARAS="$PARAS -Dportscanner.tcp.readtimeout=1500"
PARAS="$PARAS -Dportscanner.tcp.connectdelay=10"
PARAS="$PARAS -Dportscanner.loglevel=2"

java $PARAS -jar portscanner.jar "$@"
if ! [[ $? -eq 0 ]]; then    
    echo "./pscan.sh 192.168.0.1 1 10000"
fi