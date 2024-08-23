@echo off
rem -------------------------------------------------------------------------
rem JMAC
rem Author: Dmitry Vaguine
rem Copyright:    Copyright (c) 2004
rem Developed under JDK1.4.2
rem
rem playJavaSound15.bat - Startup batch file for JavaSoundSimpleAudioPlayer
rem                     with jmacspi.jar.
rem
rem Environment Variable Prerequisites:
rem
rem   JAVA_HOME    Must point at your Java Development Kit installation.
rem -------------------------------------------------------------------------

set JAVA_HOME=D:\PROGRA~1\JAVA\JDK15~1.0

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
goto cleanup
:gotJavaHome

set RUNJAVA="%JAVA_HOME%\bin\java"

echo Using classpath: %CLASSPATH%
echo Starting JMAC
@echo on

cd ..

%RUNJAVA% -cp distributables/jmacspi15.jar;test/classes -Djmac.NATIVE=true -Djava.library.path=distributables -Djmac.DEBUG=true davaguine.jmac.test.JavaSoundSimpleAudioPlayer %1
:cleanup
