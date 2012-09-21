#!/bin/sh

#base=PREFIX

if [ -z $base ] ; then
    envs=`dirname $0`/../scripts/envs.sh
    DEBUG=yes
else
    envs=/etc/flowvisor/envs.sh
fi

if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi

default_jvm_args="-server -Xms100M -Xmx2000M -XX:OnError=flowvisor-crash-logger -XX:+UseConcMarkSweepGC $fv_defines"
emma_jvm_args="-Demma.coverage.out.file=$configbase/coverage/coverage.emma -Demma.coverage.out.merge=true"

if [ -z $FV_JVM_ARGS ]; then
    export FV_JVM_ARGS="$default_jvm_args"
fi

if [ ! -z $FV_DEBUG_PORT ] ; then
# Checkout http://java.dzone.com/articles/how-debug-remote-java-applicat for 
# remote debugging details in java
    FV_JVM_ARGS="$FV_JVM_ARGS -Xdebug -Xrunjdwp:transport=dt_socket,suspend=n,address=$FV_DEBUG_PORT,server=y"
fi

echo Starting FlowVisor >&2 
#echo Running with FV_JVM_ARGS=$FV_JVM_ARGS >&2
exec java $FV_JVM_ARGS $emma_jvm_args $fv_defines $sslopts -cp $configbase/inst:$emmajar:$classpath org.flowvisor.FlowVisor "$@" 
