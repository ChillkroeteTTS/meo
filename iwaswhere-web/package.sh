#!/bin/sh

lein build

$JAVA_HOME/bin/javapackager -deploy -native \
                            -outdir packages \
                            -outfile iWasWhere \
                            -srcdir target \
                            -srcfiles iwaswhere-web-0.1.7-standalone.jar \
                            -appclass iwaswhere_web.core \
                            -name "iWasWhere" \
                            -title "iWasWhere"
