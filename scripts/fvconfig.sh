#!/bin/sh
    
#base=PREFIX

usage() {
    cat << "EOF"  
USAGE: fvconfig cmd config.json [options]
    match config.json <dpid> <match>
    load config.json
    chpasswd config.json <slicename>
    query config.json <dpid> [slicename]
    convert config.json
    generate newconfig.json [hostname] [admin_passwd] [of_port] [api_port]
    generateCert hostname
EOF
    exit 1
}

if [ -z $base ] ; then
    envs=`dirname $0`/../scripts/envs.sh
else 
    envs=$install_root/etc/flowvisor/envs.sh
fi

if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi

if [ "x$1" = "x" ] ; then
    usage
    exit 1
fi

makeSSL() {
        echo "Trying to generate SSL Server Key with passwd from scripts/envs.sh" >&2
        if [ "X$1" != "X" ] ; then
            cn=$1
        else
            cn=`hostname`
        fi
        echo Generating cert with common name == $cn
        dname="-dname cn=$cn"
        keytool -genkey -keystore $SSL_KEYSTORE -storepass $SSL_KEYPASSWD -keypass $SSL_KEYPASSWD -keyalg DSA $dname
}

cmd=$1
shift
case "X$cmd" in 
    Xmatch)
        exec java -cp $classpath $fv_defines org.flowvisor.message.FVPacketIn "$@"
    ;;
    Xload)
        exec java -cp $classpath $fv_defines org.flowvisor.config.LoadConfig "$@"
    ;;
    Xchpasswd)
        exec java -cp $classpath $fv_defines org.flowvisor.api.APIAuth "$@"
    ;;
    Xquery)
        exec java -cp $classpath $fv_defines org.flowvisor.flows.FlowSpaceUtil "$@"
    ;;
    Xconvert)
         exec java -cp $classpath $fv_defines org.flowvisor.config.convertor.Convertor "$@"
    ;;
    XgenerateCert)
        makeSSL $1
    ;;
    Xgenerate)
        java $fv_defines -cp $classpath org.apache.derby.tools.ij $logfile/FlowVisorDB.sql > /dev/null
        makeSSL $2
        exec java -cp $classpath $fv_defines org.flowvisor.config.FVConfig $1 $3 $4 $5
    ;;
      
    X)
        usage
    ;;
    *)
        echo "Unknown command '$cmd' : $@" >&2
        usage
    ;;
esac


