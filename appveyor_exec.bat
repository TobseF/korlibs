echo on

REM required to mkdir parents
setlocal enableextensions

set build_kotlin_native=true

set PROJECT_DIR=%CD%
set ATOMICFU_DIR=%PROJECT_DIR%\..\kotlinx.atomicfu
set XCOROUTINES_DIR=%PROJECT_DIR%\..\kotlinx.coroutines
set KONAN_ROOT=%HOMEDRIVE%%HOMEPATH%\.konan
set KONAN_WIN_HOME=%KONAN_ROOT%\kotlin-native-windows-0.9-dev-3210
set KONAN_BIN=%KONAN_WIN_HOME%\bin
set LOCAL_KLIB=%HOMEDRIVE%%HOMEPATH%\.konan\klib
set GLOBAL_KLIB=%KONAN_WIN_HOME%\klib\platform\mingw_x64

echo KONAN_BIN=%KONAN_BIN%


mkdir %ATOMICFU_DIR%
pushd %ATOMICFU_DIR%
	git clone https://github.com/korlibs/kotlinx.atomicfu.git %ATOMICFU_DIR%
	git pull
	git checkout master
	echo "gradle.taskGraph.whenReady { graph -> graph.allTasks.findAll { it.name ==~ /(.*ReleaseMacos.*|.*Ios.*)/ }*.enabled = false }" >> build.gradle
	call gradlew.bat publishToMavenLocal -x test -x check || exit /b
popd


REM tree %KONAN_ROOT%
echo kotlin-native-macos-0.9-dev-3210 doesn't have zlib on mingw yet
echo Fixed here: https://github.com/JetBrains/kotlin-native/commit/3ad52b8736482231d86d472e92c609a03d166cee
mkdir %LOCAL_KLIB%

rd /s /q %LOCAL_KLIB%\zlib\
rd /s /q %GLOBAL_KLIB%\zlib\

echo %KONAN_BIN%\cinterop.bat -def zlib.def -o zlib
call %KONAN_BIN%\cinterop.bat -def zlib.def -o zlib || exit /b

echo %KONAN_BIN%\klib.bat install zlib
call %KONAN_BIN%\klib.bat install zlib || exit /b

echo xcopy /S /Y %LOCAL_KLIB%\zlib %GLOBAL_KLIB%\zlib
xcopy /S /Y %LOCAL_KLIB%\zlib %GLOBAL_KLIB%\zlib\

mkdir %XCOROUTINES_DIR%
pushd %XCOROUTINES_DIR%
	git clone https://github.com/korlibs/kotlinx.coroutines.git %XCOROUTINES_DIR%
	git pull
	git checkout master
	echo "gradle.taskGraph.whenReady { graph -> graph.allTasks.findAll { it.name ==~ /(.*ReleaseMacos.*|.*Ios.*)/ }*.enabled = false }" >> build.gradle
	call gradlew.bat publishToMavenLocal -x dokka -x dokkaJavadoc -x test -x check || exit /b
popd


call gradlew.bat -s check install || exit /b
pushd samples
	echo Not bulding sample, because dragonbones is not built because out of memory on CI
	REM call ..\gradlew.bat -s check || exit /b
popd
