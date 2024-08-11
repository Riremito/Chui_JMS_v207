@echo off
set MS_VERSION=207
set MS_SUBVERSION=0
set WZ_XML_PATH=wz\
@title かえでサーバー v%MS_VERSION%.%MS_SUBVERSION%
set CLASSPATH=.;dist\*
java -server -Dnet.sf.odinms.wzpath=wz\ server.Start
pause
