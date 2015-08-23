@echo OFF
REM D:\AMD\apitrace-msvc\x64\bin\apitrace
REM "C:\Program Files\Java\jdk1.7.0_05\bin\java.exe"
REM  -Djava.library.path=D:/lwjgl-source-2.8.5/libs/natives 
REM -cp bin;D:\lwjgl-source-2.8.5\libs\lwjgl.jar;D:\lwjgl-source-2.8.5\libs\lwjgl_util.jar;D:\lwjgl-source-2.8.5\libs\jinput.jar nidefawl.game.Main
set APITR="D:\AMD\apitrace-msvc\x64\bin\apitrace" trace --api gl
set JAVAEXE="C:\Program Files\Java\jdk1.7.0_05\bin\java.exe"
set JVMARGS=-Xms128M -Xmx1G
set JVMNATIVE=-Djava.library.path=D:/lwjgl-source-2.8.5/libs/natives
set JVMCLASSPATH=-cp bin;D:\lwjgl-source-2.8.5\libs\lwjgl.jar;D:\lwjgl-source-2.8.5\libs\lwjgl_util.jar;D:\lwjgl-source-2.8.5\libs\jinput.jar
set JAVA_MAIN="nidefawl.game.Main"

%APITR% %JAVAEXE% %JVMARGS% %JVMNATIVE% %JVMCLASSPATH% %JAVA_MAIN%