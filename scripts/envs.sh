#!/bin/sh

SSL_KEYPASSWD=CHANGEME_PASSWD

#configbase=PREFIX

#install_root is for installing to a new directory, e.g., for chroot()

if [ -z $configbase ] ; then
    configbase=`dirname $0`/..
    #configbase=`pwd`/`dirname $0`/..
    install_dir=$configbase/dist
    jars=$configbase/lib
    config_dir=$configbase
    SSL_KEYSTORE=$configbase/mySSLKeyStore
    dbhome=$configbase
    logfile=$config_dir/scripts
else
    install_dir=$install_root$configbase/libexec/flowvisor
    jars=$install_dir
    config_dir=$install_root/etc/flowvisor
    SSL_KEYSTORE=$config_dir/mySSLKeyStore
    dbhome=$install_root$configbase/share/db/flowvisor
    logfile=$config_dir
fi


fv_defines="-Dorg.flowvisor.config_dir=$config_dir -Dorg.flowvisor.install_dir=$install_dir -Dderby.system.home=$dbhome -Dfvlog.configuration=$logfile/fvlog.config"


# Setup some environmental variables
classpath=$jars/openflow.jar:\
$jars/xmlrpc-client-3.1.3.jar:\
$jars/xmlrpc-common-3.1.3.jar:\
$jars/xmlrpc-server-3.1.3.jar:\
$jars/commons-logging-1.1.jar:\
$jars/ws-commons-util-1.0.2.jar:\
$jars/jsse.jar:\
$jars/asm-3.0.jar:\
$jars/cglib-2.2.jar:\
$jars/commons-codec-1.4.jar:\
$jars/commons-collections-3.2.1.jar:\
$jars/commons-dbcp-1.4.jar:\
$jars/commons-pool-1.5.6.jar:\
$jars/gson-2.0.jar:\
$jars/jetty-continuation-7.0.2.v20100331.jar:\
$jars/jetty-http-7.0.2.v20100331.jar:\
$jars/jetty-io-7.0.2.v20100331.jar:\
$jars/jetty-security-7.0.2.v20100331.jar:\
$jars/jetty-server-7.0.2.v20100331.jar:\
$jars/jetty-util-7.0.2.v20100331.jar:\
$jars/servlet-api-2.5.jar:\
$jars/derby.jar:\
$jars/derbytools.jar:\
$jars/jna.jar:\
$jars/syslog4j-0.9.46-bin.jar:\
$jars/log4j-1.2.16.jar:\
$jars/jsonrpc2-base-1.30.jar:\
$jars/jsonrpc2-server-1.8.jar:\
$install_dir/flowvisor.jar

emmajar=$jars/emma/emma.jar

# ssl options for the jvm

sslopts="-Djavax.net.ssl.keyStore=$SSL_KEYSTORE -Djavax.net.ssl.keyStorePassword=$SSL_KEYPASSWD"

# for ssl debugging options
#sslopts="$sslopts -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol -Djavax.net.debug=ssl"

 test -f /etc/default/flowvisor && . /etc/default/flowvisor
 test -f /etc/sysconfig/flowvisor && . /etc/sysconfig/flowvisor
