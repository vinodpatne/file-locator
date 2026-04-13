echo off
cls
title Start Application

call java -jar target/file-locator-1.0.0.jar
rem --trace > app.log 2>&1

pause