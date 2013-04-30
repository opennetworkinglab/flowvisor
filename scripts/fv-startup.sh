#! /bin/sh

#----------------------------------------------------------------------
# Copyright (c) 2010 Raytheon BBN Technologies
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and/or hardware specification (the "Work") to
# deal in the Work without restriction, including without limitation the
# rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Work, and to permit persons to whom the Work
# is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Work.
#
# THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
# OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
# HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
# IN THE WORK.
#----------------------------------------------------------------------

### BEGIN INIT INFO
# Provides:          flowvisor
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Should-Start:      
# Default-Start:     2 3 4 5
# Default-Stop:      1
# Short-Description: FlowVisor
# Description:       FlowVisor is a special purpose OpenFlow controller that acts as a transparent proxy between OpenFlow switches and multiple OpenFlow controllers.
### END INIT INFO

FV_USER=FVUSER

## abend 
# 
# Abort and end. 
# First argument is the exit status to use, second argument is the message. 
 
abend () 
{ 
  echo "$0: $2" 
  exit $1 
} 

case "$1" in
start)
  echo Starting flowvisor with the configuration stored in DB >&2
  echo "If DB unpopulated, load config using 'fvconfig load config.json'" >&2

  sudo -u $FV_USER PREFIX/sbin/flowvisor >> /var/log/flowvisor/flowvisor-stderr.log 2>&1 &
  ;;

stop)
  pkill -f "org.flowvisor.FlowVisor"
  ;;

status)
  PID=`pgrep -f "org.flowvisor.FlowVisor"`
  if [ -z $PID ]; then
        echo "FlowVisor is not runnning"
  else
        echo "FlowVisor is running as PID $PID"
  fi
  ;;

restart)
  $0 stop
  echo Sleeping a bit to let FlowVisor shutdown
  sleep 1
  $0 start
  ;;

*)
  abend 1 "Usage: $0 [start | stop | restart]"
  ;;

esac
