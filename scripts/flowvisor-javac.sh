#!/bin/sh

#base=PREFIX


if [ -z $base ] ; then
    envs=`dirname $0`/../scripts/envs.sh
else
    envs=$base/etc/flowvisor/envs.sh
fi

if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi

# just in case we have to invoke things by hand
javac -cp $classpath "$@"
