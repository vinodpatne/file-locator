echo off
cls
title Build Application

rem call mvn clean install -Dproc:full
rem -T 4  (uses 4 threads)
rem -T 1C (uses 1 thread per CPU core)
rem  -e error stack trace
call mvn clean package -Dmaven.test.skip=true -T 4C

pause