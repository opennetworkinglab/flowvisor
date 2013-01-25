#!/usr/bin/env python
# Copyright (c) 2012-2013  The Board of Trustees of The Leland Stanford Junior University

import urllib2, urlparse, sys, getpass, functools, json, pprint
from optparse import OptionParser

def getPassword(opts):
    if opts.fv_passwdfile is None:
        passwd = getpass.getpass("Password: ")
    else:
        passwd = open(opts.fv_passwdfile, "r").read().strip()
    return passwd

def addCommonOpts (parser):
    parser.add_option("--hostname", dest="host", default="localhost",
                    help="Specify the FlowVisor host; default='localhost'")
    parser.add_option("--port", dest="port", default="8080",
                    help="Specify the FlowVisor web port; default=8080")
    parser.add_option("--user", dest="fv_user", default="fvadmin",
                    help="FlowVisor admin user; default='fvadmin'")
    parser.add_option("--passwd-file", dest="fv_passwdfile", default=None,
                    help="Password file; default=none")

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
    parser = OptionParser(usage=USAGE.format(cmd))
    addCommonOpts(parser)
    (options, args) = parser.parse_args(args)
    return (options, args)

def do_listSlices(opts, args):
    passwd = getPassword(opts)
    data = connect(opts, "list-slices", passwd)
    print 'Configured slices:'
    for (i, name) in enumerate(data):
        print '{0:3d} : {1:5}'.format(i+1, name)

def pa_addSlice(args, cmd):
    usage = ("%s <slicename> <controller-url> " 
               "<admin-email>") % USAGE.format(cmd) 
    parser = OptionParser(usage=usage)
    addCommonOpts(parser)
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
    (options, args) = parser.parse_args(args)
    return (options, args)

def do_addSlice(opts, args):
    if len(args) != 3:
        print "add-slice : Must specify the slice name, controller url and admin contact" 
        sys.exit()
    passwd = getPassword(opts)
    req = { "slice-name" : args[0], "controller-url" : args[1], "admin-contact" : args[2] }
    if opts.passwd is None:
        req['password'] = getpass.getpass("Slice password: ")
    else:
        req['password'] = opts.passwd
    req['drop-policy'] = opts.drop
    req['recv-lldp'] = opts.lldp
    req['flowmod-limit'] = opts.flow
    req['rate-limit'] = opts.rate
    ret = connect(opts, "add-slice", passwd, data=req)
    if ret:
        print "Slice %s was successfully created" % args[0]

def pa_updateSlice(args, cmd):
    usage = "%s <slicename>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    addCommonOpts(parser)
    parser.add_option("-n", "--controller-hostname", dest="chost", default=None,
            help="Specify new controller hostname")
    parser.add_option("-p", "--controller-port", dest="cport", default=None,
            help="Specify new controller port")
    parser.add_option("-a", "--admin-contact", dest="admin", default=None,
            help="Specify new admin contact")
    parser.add_option("-d", "--drop-policy", dest="drop", default=None,
            help="Specify new drop policy")
    parser.add_option("-l", "--recv-lldp", action="store_true", default=None, dest="lldp",
            help="Specify whether slice should receive unknown LLDPs")
    parser.add_option("-f", "--flowmod-limit", type="int", default=None, dest="flow",
            help="Specify new value for slice tcam usage")
    parser.add_option("-r", "--rate-limit", type="int", default=None, dest="rate",
            help="Specify new value for slice control path rate limit")
    (options, args) = parser.parse_args(args)
    return (options, args)

def do_updateSlice(opts,args):
    if len(args) != 1:
        print "update-slice : Must specify the slice that you want to update." 
        sys.exit()
    passwd = getPassword(opts)
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
    ret = connect(opts, "update-slice", passwd, data=req)
    if ret:
        print "Slice %s has been successfully updated" % args[0]

def pa_removeSlice(args, cmd):
    usage = "%s <slicename>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    addCommonOpts(parser)
    parser.add_option("-p", "--preserve-flowspace", action="store_true", default=None, dest="preserve", 
            help="Preserve flowspace; NOT YET IMPLEMENTED")
    return parser.parse_args(args)

def do_removeSlice(opts, args):
    if len(args) != 1:
        print "remove-slice : Must specify the slice that you want to remove."
        sys.exit()
    passwd = getPassword(opts)
    req = { "slice-name" : args[0] } 
    if opts.preserve is not None:
        req['preserve-flowspace'] = True
    ret = connect(opts, "remove-slice", passwd, data=req)
    if ret:
        print "Slice %s has been deleted" % args[0]


def pa_updateSlicePassword(args, cmd):
    usage = "%s <slicename>" % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    addCommonOpts(parser)
    parser.add_option("-p", "--password", default=None, dest="passwd", 
            help="New password for slice.")
    return parser.parse_args(args)

def do_updateSlicePassword(opts, args):
    if len(args) != 1:
        print "update-slice-password : Must specify the slice."
        sys.exit()
    passwd = getPassword(opts)
    req = { "slice-name" : args[0] } 
    if opts.passwd is not None:
        req['password'] = opts.passwd
    else:
        req['password'] = getpass.getpass("New slice password: ")
    ret = connect(opts, "update-slice-password", passwd, data=req)
    if ret:
        print "Slice password for %s has been updated." % args[0]


def pa_updateAdminPassword(args, cmd):
    usage = "%s " % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    addCommonOpts(parser)
    parser.add_option("-p", "--password", default=None, dest="passwd", 
            help="New password for admin.")
    return parser.parse_args(args)

def do_updateAdminPassword(opts, args):
    passwd = getPassword(opts)
    req = {}
    if opts.passwd is not None:
        req['password'] = opts.passwd
    else:
        req['password'] = getpass.getpass("New admin password: ")
    ret = connect(opts, "update-admin-password", passwd, data=req)
    if ret:
        print "Admin password has been updated."

def pa_listFlowSpace(args, cmd):
    usage = "%s " % USAGE.format(cmd)
    parser = OptionParser(usage=usage)
    addCommonOpts(parser)
    parser.add_option("-s", "--slice-name", default=None, dest="slice", 
            help="Fetch flowspace for specified slice.")
    parser.add_option("-p", "--pretty-print", default=False, dest="pretty", action="store_true",
            help="Pretty print output")
    return parser.parse_args(args)

def do_listFlowSpace(opts, args):
    passwd = getPassword(opts)
    req = {}
    out = "Configured Flow entries:"
    if opts.slice is not None:
        req['slice-name'] = opts.slice
        out = "Configured Flow entries for slice %s:" % opts.slice
    ret = connect(opts, "list-flowspace", passwd, data=req)
    print out
    for item in ret:
        if opts.pretty:
            print json.dumps(item, sort_keys=True, indent=1)
        else:
            print item

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
    except RuntimeError, e:
        print e

def parseResponse(data):
    j = json.loads(data)
    if 'error' in j:
        print "%s : %s" % (getError(j['error']['code']),j['error']['message'])
        sys.exit(1)
    return j['result']



CMDS = {
    'list-slices' : (pa_none, do_listSlices),
    'add-slice' : (pa_addSlice, do_addSlice),
    'update-slice' : (pa_updateSlice, do_updateSlice),
    'remove-slice' : (pa_removeSlice, do_removeSlice),
    'update-slice-password' : (pa_updateSlicePassword, do_updateSlicePassword),
    'update-admin-password' : (pa_updateAdminPassword, do_updateAdminPassword),
    'list-flowspace' : (pa_listFlowSpace, do_listFlowSpace)
#    'add-flowspace' : (pa_addFlowSpace, do_addFlowSpace),
#    'update-flowspace' : (pa_updateFlowSpace, do_updateFlowSpace),
#    'remove-flowspace' : (pa_removeFlowSpace, do_removeFlowSpace),
#    'list-version' : (pa_none, do_listVersion),
#    'save-config' : (pa_none, do_saveConfig),
#    'get-config' : (pa_getConfig, do_getConfig),
#    'set-config' : (pa_setConfig, do_setConfig),
#    'list-slice-info' : (pa_listSliceInfo, do_listSliceInfo),
#    'list-datapaths' : (pa_none, do_listDatapaths),
#    'list-datapath-info' : (pa_listDatapathInfo, do_listDatapathInfo),
#    'list-datapath-stats' : (pa_listDatapathStats, do_listDatapathStats),
#    'list-fv-health' : (pa_none, do_listFVHealth),
#    'list-links' : (pa_none, do_listLinks),
#    'list-slice-health' : (pa_listSliceHealth, do_listSliceHealth),
#    'list-slice-stats' : (pa_listSliceStats, do_listSliceStats)
}

ERRORS = {
    -32700 : "Parse Error",
    -32600 : "Invalid Request",
    -32601 : "Method not found",
    -32602 : "Invalid Params",
    -32603 : "Internal Error"
}

USAGE="%prog {} [options]"

URL = "https://%s:%s"

if __name__ == '__main__':
  try:
    if sys.argv[1] == "--help":
      raise IndexError
    (parse_args, do_func) = CMDS[sys.argv[1]]
    (opts, args) = parse_args(sys.argv[2:], sys.argv[1])
    do_func(opts, args)
  except KeyError, e:
    print e
    print "'%s' is not a valid command" % (sys.argv[1])
  except IndexError, e:
    print "Valid commands are:"
    cmds = [x for x in CMDS.iterkeys()]
    cmds.sort()
    for x in cmds:
      print x

