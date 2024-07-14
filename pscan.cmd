@echo off
set PARAS=-Dportscanner.autotune=true
set PARAS=%PARAS% -Dportscanner.tcp.autotune.same.fd=2048
set PARAS=%PARAS% -Dportscanner.tcp.autotune.same.wt=40
set PARAS=%PARAS% -Dportscanner.tcp.autotune.notsame.fd=512
set PARAS=%PARAS% -Dportscanner.tcp.autotune.notsame.wt=512
set PARAS=%PARAS% -Dportscanner.tcp.connectscan=true
set PARAS=%PARAS% -Dportscanner.tcp.connectscan.threads=16
set PARAS=%PARAS% -Dportscanner.tcp.sockbuf=1024
set PARAS=%PARAS% -Dportscanner.tcp.readtimeout=1900
set PARAS=%PARAS% -Dportscanner.tcp.connectdelay=100
set PARAS=%PARAS% -Dportscanner.loglevel=2
java %PARAS%  -jar portscanner.jar %*
if %ERRORLEVEL% neq 0 (
	echo Example: pscan.cmd localhost 1 1024
)