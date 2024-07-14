# portscanner
Java based port scanner.    
A test for the new java non blocking io stuff in Java 1.4

## Build and Run
`./build.sh`   
`./pscan.sh 192.168.0.1 1 65535`   

## Java Property Values
If `autotune`is true, use more aggressive values if check for local network is true   
If `connectscan`is true, connect normally and try to read the banner   
```
-Dportscanner.autotune=true
-Dportscanner.tcp.autotune.same.fd=2048
-Dportscanner.tcp.autotune.same.wt=30
-Dportscanner.tcp.autotune.notsame.fd=512
-Dportscanner.tcp.autotune.notsame.wt=512
-Dportscanner.tcp.connectscan=true
-Dportscanner.tcp.connectscan.threads=16
-Dportscanner.tcp.sockbuf=1024
-Dportscanner.tcp.readtimeout=1900
-Dportscanner.tcp.connectdelay=100
-Dportscanner.loglevel=1
```

[Blog](https://www.codecoverage.de/posts/java/portscanner/)