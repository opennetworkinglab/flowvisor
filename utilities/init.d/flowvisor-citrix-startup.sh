#! /bin/sh

# start/stop the OpenFlow flowvisor daemon
#
# processname: flowvisor
# description: OpenFlow flowvisor daemon
# chkconfig: 2345 85 15

DAEMON=/usr/local/sbin/flowvisor
NAME=flowvisor
FLOWVISOR_ARGS=""
FLOWVISOR_CONFIG_FILE=/usr/local/etc/flowvisor/flowvisor-config.xml
FLOWVISOR_USER=netop
FLOWVISOR_GROUP=netop
FLOWVISOR_PIDFILE=/var/run/flowvisor
DESC="OpenFlow daemon"

###test -x $DAEMON || exit 0

# Source function library.
. /etc/init.d/functions

# Get config.
. /etc/sysconfig/network

# Check that networking is up.
if [ ${NETWORKING} = "no" ]
then
	exit 0
fi

export PATH="${PATH:+$PATH:}/usr/local/sbin:/usr/local/bin:/usr/sbin:/sbin"

###set -e

start() {
	echo -n $"Starting $DESC: $NAME "

	if [ ! -s "$FLOWVISOR_CONFIG_FILE" ]; then
		echo "missing or empty config file $FLOWVISOR_CONFIG_FILE"
		exit 1
	fi

	daemon --user $FLOWVISOR_USER $DAEMON $FLOWVISOR_ARGS $FLOWVISOR_CONFIG_FILE
	RETVAL=$?
	FLOWVISOR_PID=`pgrep -f '^java -server .*flowvisor'`

	echo
	[ $RETVAL -eq 0 ] && touch /var/lock/subsys/$NAME && echo $FLOWVISOR_PID > $FLOWVISOR_PIDFILE
	return $RETVAL
}	

stop() {
	echo -n $"Stopping $DESC: $NAME "
	killproc -p $FLOWVISOR_PIDFILE
	RETVAL=$?

	echo
	[ $RETVAL -eq 0 ] && rm -f /var/lock/subsys/$NAME
	return $RETVAL
}

restart() {
	stop
	start
}	

# See how we were called.
case "$1" in
  start)
	start
	;;
  stop)
	stop
	;;
  status)
	status -p $FLOWVISOR_PIDFILE
	;;
  restart|reload)
	restart
	;;
  condrestart)
	[ -f /var/lock/subsys/$NAME ] && restart || :
	;;
  *)
	echo $"Usage: $0 {start|stop|status|restart}"
	exit 1
	;;
esac

exit $?
