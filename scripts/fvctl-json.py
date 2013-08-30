#!/usr/bin/env python
# Copyright (c) 2012-2013  The Board of Trustees of The Leland Stanford Junior University

import urllib2
import urlparse
import sys
import getpass
import functools
import json
import pprint
import re
from optparse import OptionParser

def getPassword(opts):
    if opts.no_passwd:
        return ""
    if opts.fv_passwdfile is None:
        passwd = getpass.getpass("Password: ")
    else:
        passwd = open(opts.fv_passwdfile, "r").read().strip()
    return passwd

def addCommonOpts (parser):
    parser.add_option("-h", "--hostname", dest="host", default="localhost",
                    help="Specify the FlowVisor host; default='localhost'")
    parser.add_option("-p", "--port", dest="port", default="8081",
                    help="Specify the FlowVisor web port; default=8081")
    parser.add_option("-u", "--user", dest="fv_user", default="fvadmin",
                    help="FlowVisor admin user; default='fvadmin'")
    parser.add_option("-n", "--no-passwd", action="store_true",  dest="no_passwd", default=False,
                    help="Run fvctl with no password; default false")
    parser.add_option("-f", "--passwd-file", dest="fv_passwdfile", default=None,
                    help="Password file; default=none")
    parser.add_option("-v", "--version", action="callback", callback=printVersion)
    parser.add_option("--help", action="callback", callback=printHelp)

def getUrl(opts):
    return URL % (opts.host, opts.port)

def buildRequest(data, url, cmd):
    j = { "id" : "fvctl", "method" : cmd, "jsonrpc" : "2.0" }
    h = {"Content-Type" : "application/json"}    
    if data is not None:
        j['params'] = data
    return urllib2.Request(url, json.dumps(j), h)

def getError(code):
    try:
        return ERRORS[code]
    except Exception, e:
        return "Unknown Error"
     

def pa_none(args, cmd):
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=USAGE.format(cmd), description=ldesc)
    (options, args) = parser.parse_args(args)
    return (options, args)

def do_listSlices(gopts, opts, args):
    passwd = getPassword(gopts)
    data = connect(gopts, "list-slices", passwd)
    print 'Configured slices:'
    for name in data:
        print '{0:15} --> {1:8}'.format(name['slice-name'], 'enabled' if name['admin-status'] else 'disabled')

def pa_addSlice(args, cmd):
    usage = ("%s [options] <slicename> <controller-url> " 
               "<admin-email>") % USAGE.format(cmd) 
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
     
    parser.add_option("-d", "--drop-policy", dest="drop", default="exact",
            help="Drop rule type; default='exact'")
    parser.add_option("-l", "--recv-lldp", action="store_true", default=False, dest="lldp",
            help="Slice to receive unknown LLDP; default=False")         
    parser.add_option("-f", "--flowmod-limit", type="int", default=-1, dest="flow",
            help="Slice tcam usage; default is none (-1)")
    parser.add_option("-r", "--rate-limit", type="int", default=-1, dest="rate",
            help="Slice control path rate limit; default is none (-1)")
    parser.add_option("-p", "--password", dest="passwd", default=None, 
            help="Slice password")
    parser.add_option( "--disabled", action="store_false", default=True, dest="isEnabled",
            help="Disable this slice initially; default=False")         

    (options, args) = parser.parse_args(args)
    return (options, args)

def do_addSlice(gopts, opts, args):
    if len(args) != 3:
        print "add-slice : Must specify the slice name, controller url and admin contact" 
        sys.exit()
    passwd = getPassword(gopts)
    req = { "slice-name" : args[0], "controller-url" : args[1], "admin-contact" : args[2] }
    if opts.passwd is None:
        req['password'] = getpass.getpass("Slice password: ")
    else:
        req['password'] = opts.passwd
    req['drop-policy'] = opts.drop
    req['recv-lldp'] = opts.lldp
    req['flowmod-limit'] = opts.flow
    req['rate-limit'] = opts.rate
    req['admin-status'] = opts.isEnabled
    ret = connect(gopts, "add-slice", passwd, data=req)
    if ret:
        print "Slice %s was successfully created" % args[0]

def pa_updateSlice(args, cmd):
    usage = "%s [options] <slicename>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    parser.add_option("-n", "--controller-hostname", dest="chost", default=None,
            help="Specify new controller hostname")
    parser.add_option("-p", "--controller-port", dest="cport", default=None, type='int',
            help="Specify new controller port")
    parser.add_option("-a", "--admin-contact", dest="admin", default=None,
            help="Specify new admin contact")
    parser.add_option("-d", "--drop-policy", dest="drop", default=None,
            help="Specify new drop policy")
     
    parser.add_option( "--enable-slice", action="store_true", default=None, dest="status",
            help="Enables the slice.")
    parser.add_option( "--disable-slice", action="store_false", default=None, dest="status",
            help="Disables the slice.")
    
    parser.add_option( "--enable-recv-lldp", action="store_true", default=None, dest="lldp",
            help="Enable reception of unknown LLDPs for this slice")
    parser.add_option( "--disable-recv-lldp", action="store_false", default=None, dest="lldp",
            help="Disable reception of unknown LLDPs for this slice")
    parser.add_option("-f", "--flowmod-limit", type="int", default=None, dest="flow",
            help="Specify new value for slice tcam usage")
    parser.add_option("-r", "--rate-limit", type="int", default=None, dest="rate",
            help="Specify new value for slice control path rate limit")
    (options, args) = parser.parse_args(args)
    return (options, args)

def do_updateSlice(gopts, opts,args):
    if len(args) != 1:
        print "update-slice : Must specify the slice that you want to update." 
        sys.exit()
    passwd = getPassword(gopts)
    req = { "slice-name" : args[0] }
    if opts.chost is not None:
        req['controller-host'] = opts.chost
    if opts.cport is not None:
        req['controller-port'] = opts.cport
    if opts.admin is not None:
        req['admin-contact'] = opts.admin
    if opts.drop is not None:
        req['drop-policy'] = opts.drop
    if opts.lldp is not None:
        req['recv-lldp'] = opts.lldp
    if opts.flow is not None:
        req['flowmod-limit'] = opts.flow
    if opts.rate is not None:
        req['rate-limit'] = opts.rate
    if opts.status is not None:
        req['admin-status'] = opts.status
    ret = connect(gopts, "update-slice", passwd, data=req)
    if ret:
        print "Slice %s has been successfully updated" % args[0]

def pa_removeSlice(args, cmd):
    usage = "%s <slicename>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)   
 
    return parser.parse_args(args)

def do_removeSlice(gopts, opts, args):
    if len(args) != 1:
        print "remove-slice : Must specify the slice that you want to remove."
        sys.exit()
    passwd = getPassword(gopts)
    req = { "slice-name" : args[0] } 
    ret = connect(gopts, "remove-slice", passwd, data=req)
    if ret:
        print "Slice %s has been deleted" % args[0]


def pa_updateSlicePassword(args, cmd):
    usage = "%s [options] <slicename>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    parser.add_option("-p", "--password", default=None, dest="passwd", 
            help="New password for slice.")
    return parser.parse_args(args)

def do_updateSlicePassword(gopts, opts, args):
    if len(args) != 1:
        print "update-slice-password : Must specify the slice."
        sys.exit()
    passwd = getPassword(gopts)
    req = { "slice-name" : args[0] } 
    if opts.passwd is not None:
        req['password'] = opts.passwd
    else:
        req['password'] = getpass.getpass("New slice password: ")
    ret = connect(gopts, "update-slice-password", passwd, data=req)
    if ret:
        print "Slice password for %s has been updated." % args[0]


def pa_updateAdminPassword(args, cmd):
    usage = "%s [options] " % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    parser.add_option("-p", "--password", default=None, dest="passwd", 
            help="New password for admin.")
    return parser.parse_args(args)

def do_updateAdminPassword(gopts, opts, args):
    passwd = getPassword(gopts)
    req = {}
    if opts.passwd is not None:
        req['password'] = opts.passwd
    else:
        req['password'] = getpass.getpass("New admin password: ")
    ret = connect(gopts, "update-admin-password", passwd, data=req)
    if ret:
        print "Admin password has been updated."

def pa_listFlowSpace(args, cmd):
    usage = "%s [options] " % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    parser.add_option("-s", "--slice-name", default=None, dest="slice", 
            help="Fetch flowspace for specified slice.")
    parser.add_option("-x", "--output-hex", default=False, action="store_true", dest="hex",
            help="Displays relevant fields in hex")
    parser.add_option("-p", "--pretty-print", default=False, dest="pretty", action="store_true",
            help="Pretty print output")
    parser.add_option("--show-disabled", default=None, dest="show", action="store_true",
            help="Display flowspace for disabled slices")

    return parser.parse_args(args)

def toHex(match):
    for (field, value) in match.iteritems():
        if field in CONVFIELDS:
            match[field] = hex(value)
    return match

def prettify(fs):
    pass


def do_listFlowSpace(gopts, opts, args):
    passwd = getPassword(gopts)
    req = {}
    out = "Configured Flow entries:"
    if opts.slice is not None:
        req['slice-name'] = opts.slice
        out = "Configured Flow entries for slice %s:" % opts.slice
    if opts.show is not None:
        req['show-disabled'] = True
    ret = connect(gopts, "list-flowspace", passwd, data=req)
    if opts.hex:
        for fs in ret:
            fs['match'] = toHex(fs['match'])
    print out
    if len(ret) == 0:
        print "  None"
        sys.exit()
    for item in ret:
        prettify(item)
        if opts.pretty:
            print json.dumps(item, sort_keys=True, indent=1)
            print "\n\n"
        else:
            print json.dumps(item)


def pa_removeFlowSpace(args, cmd):
    usage = "%s <flowspace-name> [<flowspace-name>...]" % USAGE.format(cmd)

    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    return parser.parse_args(args)

def do_removeFlowSpace(gopts, opts, args):
    if len(args) < 1:
        print "remove-flowpace : Must specify the name of the flowspace to remove."
        sys.exit()
    passwd = getPassword(gopts)
    ret = connect(gopts, "remove-flowspace", passwd, data=args)  
    if ret:
        print "Flowspace entries have been removed."

def pa_addFlowSpace(args, cmd):
    usage = "%s [options] <flowspace-name> <dpid> <priority> <match> <slice-perm>" % USAGE.format(cmd)

    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    parser.add_option("-q", "--queues", default=None, dest="queues", type="string",
            action="callback", callback=list_callback, 
            help="Define list of queues permitted on this flowspace.")
    parser.add_option("-f", "--forced-queue", default=None, dest="fqueue", type="int",
            help="Force a queue id upon output action.")
    return parser.parse_args(args)

def do_addFlowSpace(gopts, opts, args):
    if len(args) != 5:
        print "add-flowpace : Requires 5 arguments; only %d given" % len(args)
        print "add-flowspace: <flowspace-name> <dpid> <priority> <match> <slice-perm>"
        print "If range is specified for vlan, the format for add-flowspace is as below:(Currently only vlan ranges are supported)"
        print "add-flowspace <flowspace-name> <dpid> <priority> <dl_vlan=1-10> <slice-perm>"
        sys.exit()
    passwd = getPassword(gopts)
    if(args[3].find('-') != -1):   
        #Get the ranges
        ranges = getRange(args[3])
        if((int(ranges[0]) < 1) or (int(ranges[0]) > 4095) or (int(ranges[1]) < 1) or (int(ranges[1]) > 4095)):
            print "The vlan range should be within 1 and 4095"
            sys.exit()
        for r in range((int(ranges[0])), (int(ranges[1])+1)):
            match = makeMatchWithRanges(args[3],r)
            req = { "name" : args[0], "dpid" : args[1], "priority" : int(args[2]), "match" : match }
            if opts.queues is not None:
                req['queues'] = opts.queues
            if opts.fqueue is not None:
                req['force-enqueue'] = opts.fqueue
            actions = args[4].split(',')
            acts = []
            for action in actions:
                parts = action.split('=')
                act = { 'slice-name' : parts[0], "permission" : parts[1] }
                acts.append(act)
            req['slice-action'] = acts
            ret = connect(gopts, "add-flowspace", passwd, data=[req])
            if ret:
                print "FlowSpace %s was added with request id %s." % (args[0], ret)
    else:
        match = makeMatch(args[3])
        req = { "name" : args[0], "dpid" : args[1], "priority" : int(args[2]), "match" : match }
        if opts.queues is not None:
            req['queues'] = opts.queues
        if opts.fqueue is not None:
            req['force-enqueue'] = opts.fqueue
        actions = args[4].split(',')
        acts = []
        for action in actions:
            parts = action.split('=')
            act = { 'slice-name' : parts[0], "permission" : parts[1] }
            acts.append(act)
        req['slice-action'] = acts
        ret = connect(gopts, "add-flowspace", passwd, data=[req])  
        if ret:
            print "FlowSpace %s was added with request id %s." % (args[0], ret)

def pa_updateFlowSpace(args, cmd):
    usage = "%s [options] <flowspace-name>" % USAGE.format(cmd)

    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    parser.add_option("-d", "--dpid", default=None, dest="dpid", type="string",
            help="Set the dpid for flowspace entry.")
    parser.add_option("-p", "--priority", default=None, dest="prio", type="int",
            help="Set the priority for flowspace entry.")
    parser.add_option("-m", "--match", default=None, dest="match", type="string",
            help="Set the match for flowspace entry.")
    parser.add_option("-s", "--slice-action", default=None, dest="sact", type="string",
            help="Set the slice(s) for flowspace entry.")
    parser.add_option("-q", "--queues", default=None, dest="queues", type="string",
            action="callback", callback=list_callback, 
            help="Define list of queues permitted on this flowspace.")
    parser.add_option("-f", "--forced-queue", default=None, dest="fqueue", type="int",
            help="Force a queue id upon output action.")

    return parser.parse_args(args)

def do_updateFlowSpace(gopts, opts, args):
    if len(args) != 1:
        print "update-flowpace : Requires 1 argument; only %d given" % len(args)
        print "update-flowspace: <flowspace-name>"
        sys.exit()
    passwd = getPassword(gopts)
    req = {'name' : args[0]}
    if opts.match is not None:
        match = makeMatch(opts.match)
        req['match'] = match
    if opts.queues is not None:
        req['queues'] = opts.queues
    if opts.fqueue is not None:
        req['force-enqueue'] = opts.fqueue
    if opts.dpid is not None:
        req['dpid'] = opts.dpid
    if opts.prio is not None:
        req['priority'] = opts.prio
    if opts.sact is not None:       
        actions = opts.sact.split(',')
        acts = []
        for action in actions:
            parts = action.split('=')
            act = { 'slice-name' : parts[0], "permission" : int(parts[1]) }
            acts.append(act)
        req['slice-action'] = acts
    if len(req.keys()) <= 1:
        print "update-flowspace : You may want to actually specify something to update."
        sys.exit()
    ret = connect(gopts, "update-flowspace", passwd, data=[req])  
    if ret:
        print "Flowspace %s was updated with request id %s" % (args[0], ret)

def do_listVersion(gopts, opts, args):
    passwd = getPassword(gopts)
    ret = connect(gopts, "list-version", passwd)
    print "FlowVisor version : %s" % ret['flowvisor-version']
    print "FlowVisor DB version : %s" % ret['db-version']

def pa_saveConfig(args, cmd):
    usage = "%s <config-file>" % USAGE.format(cmd)
   
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
 
    return parser.parse_args(args)

def do_saveConfig(gopts, opts, args):
    if len(args) != 1:
        print "save-config : Need a config-file to save config to."
        sys.exit()
    passwd = getPassword(gopts)
    ret = connect(gopts, "save-config", passwd)
    output = open(args[0], 'w')
    print>>output, ret
    output.close()
    print "Config file written to %s." % args[0]

def pa_getConfig(args, cmd):
    usage = "%s [options]" % USAGE.format(cmd)
    
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)

    parser.add_option("-s", "--slice-name", default=None, dest="slice", type="string",
            help="Fetch config for this slice.")
    parser.add_option("-d", "--dpid", default=None, dest="dpid", type="string",
            help="Fetch config for this dpid.")
    return parser.parse_args(args)

def do_getConfig(gopts, opts, args):
    passwd = getPassword(gopts)
    req = {}
    if opts.slice is not None:
        req['slice-name'] = opts.slice
    if opts.dpid is not None:
        req['dpid'] = opts.dpid
    ret = connect(gopts, "get-config", passwd, data=req)
    print json.dumps(ret, sort_keys=True, indent = 2)

def pa_setConfig(args, cmd):
    usage = "%s [options]" % USAGE.format(cmd)
    
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)

    parser.add_option("-f", "--flood-perm", default=None, dest="perm", type="string",
            help="Set the floodperm", metavar="SLICE[,DPID]")
    parser.add_option("-l", "--flowmod-limit", default=None, dest="limit", type="string",
            help="Set the flowmod limit.", metavar="SLICE,DPID,LIMIT")
    parser.add_option("--enable-tracking", default=None, dest="track", action="store_true",
            help="Enable flow tracking.")
    parser.add_option("--disable-tracking", default=None, dest="track", action="store_false",
            help="Disable flow tracking.")
    parser.add_option("--enable-stats-desc", default=None, dest="stats", action="store_true",
            help="Enable stats description hijacking.")
    parser.add_option("--disable-stats-desc", default=None, dest="stats", action="store_false",
            help="Disable stats description hijacking.")
    parser.add_option("--enable-topo-ctrl", default=None, dest="topo", action="store_true",
            help="Enable topology controller.")
    parser.add_option("--disable-topo-ctrl", default=None, dest="topo", action="store_false",
            help="Disable topology controller.")
    parser.add_option("-c", "--flow-stats-cache", default=None, dest="cache", type="int",
            help="Set the aging timer for the flow stats cache.")
    
    return parser.parse_args(args)

def do_setConfig(gopts, opts, args):
    passwd = getPassword(gopts)
    req = {}
    if opts.perm is not None:
        parts = opts.perm.split(',')
        if len(parts) > 2 or len(parts) < 1:
            print "Setting the flood permission requires only the slice and optionally the dpid, see help."
            sys.exit()
        elif len(parts) == 1:
            req['flood-perm'] = { 'slice-name' : parts[0] }
        else: 
            req['flood-perm'] = { 'slice-name' : parts[0], 'dpid' : parts[1] }

    if opts.limit is not None:
        parts = opts.limit.split(',')
        if len(parts) != 3:
            print "Need to specify SLICE, DPID, and LIMIT for flowmod limit, see help"
            sys.exit()
        req['flowmod-limit'] = { 'slice-name' : parts[0], 'dpid' : parts[1], 'limit' : int(parts[2]) }

    if opts.track is not None:
        req['track-flows'] = opts.track
    if opts.stats is not None:
        req['stats-desc'] = opts.stats
    if opts.topo is not None:
        req['enable-topo-ctrl'] = opts.topo
    if opts.cache is not None:
        req['flow-stats-cache'] = opts.cache
    ret = connect(gopts, "set-config", passwd, data=req)
    if ret:
        print "Configuration has been updated"


def pa_listSliceInfo(args, cmd):
    usage = "%s <slicename>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    return parser.parse_args(args)

def do_listSliceInfo(gopts, opts, args):
    if len(args) != 1:
        print "list-slice-info : Please specify the slice name"
        sys.exit()
    passwd = getPassword(gopts)
    req = { "slice-name" : args[0]}
    ret = connect(gopts, "list-slice-info", passwd, data=req)
    if 'msg' in ret:
        print "Note: %s" % ret['msg']
        del ret['msg']
    print json.dumps(ret, sort_keys=True, indent = 2)

def do_listDatapaths(gopts, opts, args):
    passwd = getPassword(gopts)
    ret = connect(gopts, "list-datapaths", passwd)
    if len(ret) <= 0:
       print "No switches connected"
       sys.exit()
    ret.sort()
    print "Connected switches: "
    for (i, sw) in enumerate(ret):
        print "  %d : %s" % (i+1,sw)

def pa_listDatapathInfo(args, cmd):
    usage = "%s <dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)   
 
    return parser.parse_args(args)

def do_listDatapathInfo(gopts, opts, args):
    if len(args) != 1:
        print "list-datapath-info : Please specify the dpid."
        sys.exit()
    passwd = getPassword(gopts)
    req = { "dpid" : args[0]}
    ret = connect(gopts, "list-datapath-info", passwd, data=req)
    print json.dumps(ret, sort_keys=True, indent = 2)

def pa_listDatapathStats(args, cmd):
    usage = "%s <dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    
    return parser.parse_args(args)

def do_listDatapathStats(gopts, opts, args):
    if len(args) != 1:
        print "list-datapath-stats : Please specify the dpid."
        sys.exit()
    passwd = getPassword(gopts)
    req = { "dpid" : args[0]}
    ret = connect(gopts, "list-datapath-stats", passwd, data=req)
    print json.dumps(ret, sort_keys=True, indent = 2)

def pa_listSliceStats(args, cmd):
    usage = "%s <slicename>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_listSliceStats(gopts, opts, args):
    if len(args) != 1:
        print "list-slice-stats : Please specify the slice name"
        sys.exit()
    passwd = getPassword(gopts)
    req = { "slice-name" : args[0]}
    ret = connect(gopts, "list-slice-stats", passwd, data=req)
    print json.dumps(ret, sort_keys=True, indent = 2)

def pa_regEventCB(args, cmd):
    usage = "%s <url> <methodname> <eventtype> <name>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    parser.add_option("-d", "--dpid", dest="dpid", type="string", default=None,
            help="Set the dpid for registering for the flowtable information; default='all'")
    return parser.parse_args(args)

def do_regEventCB(gopts, opts, args):
    if len(args) != 4:
        print "register-event-callback : Must specify all the arguments"
        sys.exit()
    passwd = getPassword(gopts)
    req = { 'url' : args[0], 'method' : args[1], 'event-type' : args[2], 'cookie' : args[3]}
    if opts.dpid is not None:
        req['dpid'] = opts.dpid
    else:
        req['dpid'] = 'all'
    ret = connect(gopts, "register-event-callback", passwd, data=req)
    if ret:
        print "Callback %s successfully added" % args[3]

def pa_unregEventCB(args, cmd):
    usage = "%s <methodname> <eventtype> <name>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    parser.add_option("-d", "--dpid", dest="dpid", type="string", default=None,
        help="Set the dpid for unregistering for the flowtable information; default='all'")
    return parser.parse_args(args)

def do_unregEventCB(gopts, opts, args):
    if len(args) != 3:
        print "unregister-event-callback : Must specify all the arguments"
        sys.exit()
    passwd = getPassword(gopts)
    req = { 'method' : args[0], 'event-type' : args[1], 'cookie' : args[2]}
    if opts.dpid is not None:
        req['dpid'] = opts.dpid
    else:
        req['dpid'] = 'all'
    ret = connect(gopts, "unregister-event-callback", passwd, data=req)
    if ret:
        print "Callback %s successfully removed" % args[2]

def do_listFVHealth(gopts, opts, args):
    passwd = getPassword(gopts)
    ret = connect(gopts, "list-fv-health", passwd)
    print json.dumps(ret, sort_keys=True, indent = 2) 

def pa_listSliceHealth(args, cmd):
    usage = "%s <slicename>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_listSliceHealth(gopts, opts, args):
    if len(args) != 1:
        print "list-slice-health : Please specify the slice name"
        sys.exit()
    passwd = getPassword(gopts)
    req = { "slice-name" : args[0]}
    ret = connect(gopts, "list-slice-health", passwd, data=req)
    print json.dumps(ret, sort_keys=True, indent = 2)

def do_listLinks(gopts, opts, args):
    passwd = getPassword(gopts)
    ret = connect(gopts, "list-links", passwd)
    print json.dumps(ret, sort_keys=True, indent = 2)

def pa_listflowdb(args, cmd):
    usage = "%s <dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_listflowdb(gopts, opts, args):
    if len(args) != 1:
        print "list-datapath-flowdb : Please specify the dpid"
        sys.exit()
    passwd = getPassword(gopts)
    req = { 'dpid' : args[0]}
    ret = connect(gopts, "list-datapath-flowdb", passwd, data=req)
    print "Flows seen at FlowVisor: "
    for fbe in ret:
        print fbe

def pa_listrewritedb(args, cmd):
    usage = "%s <slicename> <dpid>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage=usage, description=ldesc)
    return parser.parse_args(args)

def do_listrewritedb(gopts, opts, args):
    if len(args) != 2:
        print "list-datapath-flowrewritedb : Please specify the slicename and dpid"
        sys.exit()
    passwd = getPassword(gopts)
    req = { 'dpid' : args[1], 'slice-name' : args[0]}
    ret = connect(gopts, "list-datapath-flowrewritedb", passwd, data=req)
    print "Rewrites applied by FlowVisor: "
    for fbe in ret:
        print fbe

def pa_listFSStatus(args, cmd):
    usage = "%s <fs-id>" % USAGE.format(cmd)
    (sdesc, ldesc) = DESCS[cmd]
    parser = OptionParser(usage, description=ldesc)
    return parser.parse_args(args)

def do_listFSStatus(gopts, opts, args):
    if len(args) != 1:
        print "list-fs-status : Please specify a flowspace id"
        sys.exit()
    passwd = getPassword(gopts)
    req = { 'fs-id' : int(args[0]) }
    ret = connect(gopts, "list-fs-status", passwd, data=req)
    print "FlowSpace Request id %s : %s" % (args[0], ret)


def pa_help(args, cmd):
    usage = "%s <cmd>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    return parser.parse_args(args)

def do_help(gopts, opts, args):
    if len(args) != 1:
        raise IndexError
    try:
        (pa, func) = CMDS[args[0]]
        pa(['--help'], args[0])
    except KeyError, e:
        print "Invalid command : %s is an unknown command." % args[0]
        sys.exit()
    

def makeMatch(matchStr):
    if matchStr == 'any' or matchStr == 'all':
        return {}
    matchItems = matchStr.split(',')
    match = {}
    for item in matchItems:
        it = item.split('=')
        if len(it) != 2:
            print it
            print "Match items must be of the form =, not %s" % it
            sys.exit()
        try:
            (mstr, func) = MATCHSTRS[it[0].lower()]
            match[mstr] = func(it[1])
        except KeyError, e:
            print "Unknown match item %s" % it[0]
            sys.exit()
    return match

def makeMatchWithRanges(matchStr,rng):
    if matchStr == 'any' or matchStr == 'all':
        return {}
    matchItems = matchStr.split(',')
    match = {}
    for item in matchItems:
        it = item.split('=')
        if len(it) != 2:
            print it
            print "Match items must be of the form <key>=<val>"
            sys.exit()
        try:
            if it[0].lower() != 'dl_vlan':
                (mstr, func) = MATCHSTRS[it[0].lower()]
                match[mstr] = func(it[1])
            else:
                match['dl_vlan'] = rng
        except KeyError, e:
            print "Unknown match item %s" % it[0]
            sys.exit()
    return match

def getRange(matchStr):
    matchItems = matchStr.split(',')
    match = {}
    ranges = []
    for item in matchItems:
        it = item.split('=')
        if len(it) != 2:
            print "Match items must be of the form <key>=<val>"
            sys.exit()
        if it[0].lower() == 'dl_vlan':
            ranges = it[1].split('-')
            if len(ranges) !=2:
                print "The vlan range should be given in the form <from>-<to>, for eg. dl_vlan=10-100"
    return ranges
               
def connect(opts, cmd, passwd, data=None):
    try:
        url = getUrl(opts)
        passman = urllib2.HTTPPasswordMgrWithDefaultRealm()
        passman.add_password(None, url, opts.fv_user, passwd)
        authhandler = urllib2.HTTPBasicAuthHandler(passman)
        opener = urllib2.build_opener(authhandler)

        req = buildRequest(data, url, cmd)
        ph = opener.open(req)
        return parseResponse(ph.read())
    except urllib2.HTTPError, e:
        if e.code == 401:
            print "Authentication failed: invalid password"
            sys.exit(1)
        elif e.code == 504:
            print "HTTP Error 504: Gateway timeout"
            sys.exit(1)
        else:
            print e
    except urllib2.URLError, e:
        print "Could not reach a FlowVisor RPC server at %s:%s." % (opts.host, opts.port) 
        print "Please check that FlowVisor is running and try again."
        sys.exit(1)
    except RuntimeError, e:
        print e

def parseResponse(data):
    j = json.loads(data)
    if 'error' in j:
        print "%s -> %s" % (getError(j['error']['code']),j['error']['message'])
        sys.exit(1)
    return j['result']

def toInt(val):
    if val is None:
        return
    if val is not None and val.find('0x') != -1:
        return int(val,16) 
    return int(val)

def toMacInt(val):
    if val is None:
        return
    if val.find(":") != -1:
        return int(val.replace(':', ''),16)
    if val.find("-") != -1:
        int(val.replace('-', ''),16)

def toStr(val):
    return str(val)

def list_callback(option, opt, value, parser):
    vals = []
    for val in value.split(','):
        vals.append(int(val))
    setattr(parser.values, option.dest, vals)

def printVersion(option, opt, value, parser):
    print "fvctl-0.1"
    sys.exit()

def printHelp(option, opt, value, parser):
    cmds = [x for x in CMDS.iterkeys()]
    cmds.remove('help')
    cmds.sort()
    print(parser.format_help().strip())
    print "\n Available commands are: "
    for x in cmds:
      (sdesc, ldesc) = DESCS[x]
      print "   {0:25}     {1:10}".format(x, sdesc)
    print "\n See '%s help <command>' for more info on a specific command." % sys.argv[0]
    sys.exit()



def print_help(option, opt, value, parser):
    print "%s %s %s %s" % (option, opt, value, parser)

CONVFIELDS = [ 'dl_type', 'dl_vlan', 'dl_vpcp' ]

MATCHSTRS = {
    'in_port' : ('in_port', toInt),
    'input_port' : ('in_port', toInt),
    'dl_dst' : ('dl_dst', toStr),
    'eth_dst' : ('dl_dst', toStr),
    'dl_src' : ('dl_src', toStr),
    'eth_src' : ('dl_src',toStr),
    'dl_type' : ('dl_type',toInt),
    'eth_type' : ('dl_type',toInt),
    'dl_vlan' : ('dl_vlan', toInt),
    'dl_vpcp' : ('dl_vpcp', toInt),
    'dl_vlan_pcp' : ('dl_vpcp',toStr),
    'nw_dst' : ('nw_dst',toStr),
    'nw_src' : ('nw_src',toStr),
    'nw_proto' : ('nw_proto',toInt),
    'nw_tos' : ('nw_tos',toInt),
    'tp_src' : ('tp_src',toInt),
    'tp_dst' : ('tp_dst', toInt)
}


CMDS = {
    'list-slices' : (pa_none, do_listSlices),
    'add-slice' : (pa_addSlice, do_addSlice),
    'update-slice' : (pa_updateSlice, do_updateSlice),
    'remove-slice' : (pa_removeSlice, do_removeSlice),
    'update-slice-password' : (pa_updateSlicePassword, do_updateSlicePassword),
    'update-admin-password' : (pa_updateAdminPassword, do_updateAdminPassword),
    'list-flowspace' : (pa_listFlowSpace, do_listFlowSpace),
    'add-flowspace' : (pa_addFlowSpace, do_addFlowSpace),
    'update-flowspace' : (pa_updateFlowSpace, do_updateFlowSpace),
    'remove-flowspace' : (pa_removeFlowSpace, do_removeFlowSpace),
    'list-version' : (pa_none, do_listVersion),
    'save-config' : (pa_saveConfig, do_saveConfig),
    'get-config' : (pa_getConfig, do_getConfig),
    'set-config' : (pa_setConfig, do_setConfig),
    'list-fs-status' : (pa_listFSStatus, do_listFSStatus),
    'list-slice-info' : (pa_listSliceInfo, do_listSliceInfo),
    'list-datapaths' : (pa_none, do_listDatapaths),
    'list-datapath-info' : (pa_listDatapathInfo, do_listDatapathInfo),
    'list-datapath-stats' : (pa_listDatapathStats, do_listDatapathStats),
    'list-fv-health' : (pa_none, do_listFVHealth),
    'list-links' : (pa_none, do_listLinks),
    'list-slice-health' : (pa_listSliceHealth, do_listSliceHealth),
    'list-slice-stats' : (pa_listSliceStats, do_listSliceStats),
    'register-event-callback' : (pa_regEventCB, do_regEventCB),
    'unregister-event-callback' : (pa_unregEventCB, do_unregEventCB),
    'list-datapath-flowdb' : (pa_listflowdb, do_listflowdb),
    'list-datapath-flowrewritedb' : (pa_listrewritedb, do_listrewritedb),
    'help' : (pa_help, do_help)
}

ERRORS = {
    -32700 : "Parse Error",
    -32600 : "Invalid Request",
    -32601 : "Method not found",
    -32602 : "Invalid Params",
    -32603 : "Internal Error"
}

USAGE="%prog {0}"

URL = "https://%s:%s"


DESCS = {
    'list-slices' : ("Displays the configured slices",
                    "Displays currently configured slices."
                    ),
    'add-slice' :   ("Creates a new slice", 
                    ("Creates a new slice. The slicename can contain any character except "
                    "except the newline (Note: slicenames are case insensitive). " 
                    "The controller url is of the form tcp:hostname:port, "
                    "so for example tcp:example.com:12345 is a valid controller url. The admin "
                    "email is used for administrative purposes if there is a problem with the slice "
                    "The remaining parameters are optional. The drop policy, defines what kind of rule "
                    "should be pushed to the switch if there is no response from the controller "
                    "(this includes if there is no controller connected). Currently two modes are supported, "
                    " 'exact' and 'rule', 'exact' matches the packet exactly and 'rule' matches the rule that "
                    "the packet triggered. Flowmod limit limits the number of flowmods this slice can emit. "
                    "The rate limits the number of OpenFlow messages that can be sent to the switches from this "
                    "slice. You may also set the slice to receive unknown LLDP messages. Optionally, you may set "
                    "this slice as disabled initially, this is then changed via an update-slice call. "
                    "Finally, the password is the slice password." 
                     )
                    ),
    'update-slice' :("Changes slice parameters", 
                    ("Allows a slice or admin user to change the parameters of a slice. "
                    "Currently all the parameters of a slice are changeable, except the slicename"
                    )
                    ),
    'remove-slice' :("Deletes a slice", 
                    ("Deletes a slices and removes all the associated flowspace. "
                    )),
    'update-slice-password' : ("Updates slice password", 
                    ("Updates the slice password for the specified slice."
                    )),
    'update-admin-password' : ("Update admin password", 
                    ("Update the admin password.")
                    ),
    'list-flowspace' : ("Displays the flowspace",
                    ("Lists the flow-based slice policy rules, ie. the flowspace. Optionally, "
                    "list the flowspace only for the slice given as an option. If a slicename is given, "
                    "all flowspace for this slice is shown regardless of its admin status. If no slicename "
                    "is given, then only flowspace for enabled slices is shown, unless the show-disabled option "
                    "is specified."
                    )),
    'add-flowspace' : ("Creates a flowspace rule",
                    ("Creates a new rule in the flowspace with the given name. Queues can be assigned to the slice by "
                    "passing a comma seperated list of queue ids, for example '-q 1,2,3'. If a forced "
                    "queue is defined, then any flowmod matching this rule will see its OUTPUT actions "
                    "replaced with the queue id given in the forced queue option. Note: The forced queue "
                    "should be defined in the queue option and all these queue ids should be defined with "
                    "the appropriate port on the switch."
                    "A range can be specified only for the dl_vlan match field."
                    "To specify the range for dl_vlan match field, the add-flowspace api has to be used as in the below example:" 
                    "Eg. ./fvctl.py add-flowspace flow1 all 1000 dl_vlan=1-10 slice1=6"
                    "See the fvctl man page for information "
                    "on the format of <dpid>, <match>, and <slice-perm>." 
                    )),
    'update-flowspace' : ("Changes flowspace rule parameters",
                    ("Updates a parameter of a flowspace rule with a given name. The parameters follow "
                    "the same format as in add-flowspace."
                    )),
    'remove-flowspace' : ("Remove a flowspace rule", 
                    ("Removes the specified flowspace rule, given the name or a list of names of "
                    "flowspace entries."
                    )),
    'list-version' : ("Displays FlowVisor version",
                    ("Fetches the FlowVisor's version and it's database version."
                    )),
    'save-config' : ("Saves Flowvisor's configuration",
                    ("Saves the running configuration for the FlowVisor."
                    )),
    'get-config' : ("Displays the general purpose FlowVisor config params",
                   ("Displays the general purpose FlowVisor configuration parameters. These parameter "
                    "include flow tracking (also known as flow db), the topology controller, statistics "
                    "description modification status. The FlowVisor flow statistics cache timeout value, when "
                    "this timeout expires FlowVisor will refresh the stats. Flood permissions for a dpid are given "
                    "if the dpid is given, otherwise it returns the slice which has global flood permissions."
                   )),
    'set-config' : ("Sets general purpose FlowVisor config parameters",
                   ("Sets general purpose FlowVisor configuration parameters. The parameters are the same as "
                    "in get-config. For flood permissions, if both the dpid and the slice are given, then "
                    "that slice is given permission for that dpid. Otherwise, the given slice is set as the slice "
                    "global flood permissions. For flowmod limits, the limit is set per slice per dpid. The dpid in "
                    "case could be 'any'."
                   )),
    'list-fs-status' : ("Check the insertion status of a add-flowspace or update-flowspace request.",
                   ("In some cases, inserting FlowSpace can take a long time. This command allows you to check "
                    "whether the FlowSpace request has been processed or rejected. Return values are: UNKNOWN, PENDING, "
                    "SUCCESS, or an error message. PENDING means the request has not been handled yet, UNKNOWN means the "
                    "given id is not known to the system and SUCCESS means the request was successfully handled. An error "
                    "is returned when a request fails."
                   )),

    'list-slice-info' : ("Displays slice information",
                    ("Displays slice information about the configured slices at the FlowVisor, "
                    "including the current message rate and the number of currently installed "
                    "flowmods."
                    )),
    'list-datapaths' : ("Displays the devices",
                    ("Displays the devices, ie. switches, currently connected to FlowVisor by DPID."
                    )),
    'list-datapath-info' : ("Displays information for a connected device", 
                    ("Displays information about a connected device, including ports and "
                    "connection information and the current number of flowmods installed "
                    "by each slice."
                    )),
    'list-datapath-stats' : ("Display statistics for a connected device",
                    ("Displays statistics about a connected device, these include sent and "
                    "received messages sorted by OpenFlow message type."
                    )),
    'list-links' : ("Display overall topology", 
                    ("Displays the overall (ie. unsliced) network topology provided that "
                    "the topology controller is running."
                   )),
    'list-slice-stats' : ("Displays statistics about a slice",
                    ("Displays statistics about a slice, these include send and receive "
                    "messages sorted by OpenFlow message type."
                   )),
    'list-fv-health' : ("Reports overall FlowVisor health" ,
                    ("Reports on some health parameters of FlowVisor, such as message "
                    "processing delay and active database sessions."
                    )),
    'list-slice-health' : ("Reports overall slice health",
                    ("Reports the overall slice health by returning whether the slice is "
                    "connected to a controller or who is connected to it, or the number "
                    "flowspace entries it is made up of. "
                    )),
    'register-event-callback' : ("Registers your server, for events from FlowVisor",
                    ("Registers for events from FlowVisor. Possible events are: DEVICE_CONNECTED, "
                    "SLICE_CONNECTED, SLICE_DISCONNECTED and FLOWTABLE_CALLBACK. For FLOWTABLE_CALLBACK"
                    " event type dpid has to be input with -d option. More events may be added later."
                    )),
    'unregister-event-callback' : ("Unregisters your server from FlowVisor",
                    ("Unregisters your server from FlowVisor thereby deactivating event "
                    "callbacks for you."
                    )),
    'list-datapath-flowdb' : ("Displays the contents of the flow db if flow tracking is enabled",
                        ("When flow tracking is enabled, FlowVisor tracks flows which are pushed "
                        "to switches from slices. This command displays these flows."
                        )),
    'list-datapath-flowrewritedb' : ("Displays the rewrites (or expansions) FlowVisor has applied",
                        ("When a controller pushes flows through FlowVisor, FlowVisor may rewrite or "
                        "expand the flow. This command displays the expansion for a flow, if any. "
                        "Note: Flow tracking must be enabled."
                        ))
}

def parse_global_args(arglist):
    usage = "%s [options] command [command_args]" % sys.argv[0]
    args = []
    while (len(arglist) != 0 and arglist[0] not in CMDS):
        args.append(arglist[0])
        arglist.pop(0) 
    parser = OptionParser(add_help_option=False, usage=usage)
    addCommonOpts(parser)
    (opts, pargs) = parser.parse_args(args)
    return (opts, arglist, parser)


if __name__ == '__main__':
  try:
    (gopts, rargs, parser) = parse_global_args(sys.argv[1:])
    if len(rargs) < 1:
        raise IndexError
    (parse_args, do_func) = CMDS[rargs[0]]
    (opts, args) = parse_args(rargs[1:], rargs[0])
    do_func(gopts, opts, args)
  except IndexError, e:
    print "%s is an unknown command" % sys.argv[-1]
    printHelp(None, None, None, parser)


