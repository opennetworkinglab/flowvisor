#!/bin/sh

#base=PREFIX

echo "Warning: The XMLRPC interface is deprecated, and will be removed in future versions"

if [ -z $base ] ; then
    envs=`dirname $0`/../scripts/envs.sh
else
    envs=/etc/flowvisor/envs.sh
fi

if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi

exec java -cp $classpath $fv_defines org.flowvisor.api.FVCtl "$@"
