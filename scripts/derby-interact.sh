#/bin/sh

#base=PREFIX

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

exec java $fv_defines -cp $classpath org.apache.derby.tools.ij $@
#exec java -cp $classpath org.apache.derby.tools.ij $@
