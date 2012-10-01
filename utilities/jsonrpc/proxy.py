
"""
  Copyright (c) 2007 Jan-Klaas Kollhof

  This file is part of jsonrpc.

  jsonrpc is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 2.1 of the License, or
  (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this software; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
"""

import urllib
import json 

class JSONRPCException(Exception):
    # The Constant COLLECTION_ERROR.
    INVALID_REQUEST = -32600;
        
    # The Constant JSON_FORMAT_ERROR. 
    PARSE_ERROR = -3;
        
    # The Constant NETWORK.
    METHOD_NOT_FOUND = -4;
        
    # The Constant NOT_FOUND. 
    INVALID_PARAMETERS = -32602;
        
    # The Constant INTERNAL_ERROR.
    INTERNAL_ERROR = -5;

    def __init__(self, rpcError, data, message):
        Exception.__init__(self)
        self.error = rpcError
        self.data = data
        self.message = message

    def printMessage(self):
      if self.error == PARSE_ERROR:
        codestr = "Parse Error"
      elif self.error == METHOD_NOT_FOUND:
        codestr = "Method Not Found"
      else:
        codestr = "Internal Server Error"
        print "JSON RPC Exception: %s (%s)" % (codestr, self.message)
        print self.data
       
class JSONParam(object):
    def __init__(self, valueAsJson):
      self.__valueAsJson = valueAsJson

    def getValueAsJson(self):
      return {'valueAsJson' : self.__valueAsJson}

class ServiceProxy(object):
    def __init__(self, serviceURL, paramEncoder, serviceName=None):
      self.__serviceURL = serviceURL
      self.__serviceName = serviceName
      self.__paramEncoder = paramEncoder

    def __getattr__(self, name):
      if self.__serviceName != None:
        name = "%s.%s" % (self.__serviceName, name)
      return ServiceProxy(self.__serviceURL, self.__paramEncoder, name)

    def __call__(self, *args):
      # astruble: updated to spec 2.0
      jsonArgs = []
      for arg in args:
        jsonArgs.append(JSONParam(json.dumps(arg, cls=self.__paramEncoder)))

      postdata = json.dumps({"method": self.__serviceName, 'params': jsonArgs, 'id':1, 'jsonrpc':'2.0'}, cls=self.__paramEncoder)

      try:
        respdata = urllib.urlopen(self.__serviceURL, postdata).read()
      except IOError:
        print "IOError (is flowvisor running?)"
        return ""
         
      resp = json.loads(respdata)
      resp.setdefault('error_code', 0)
      if resp['error_code'] != 0:
        raise JSONRPCException(resp['error_code'], resp['message'], resp['data'])
      else:
        return resp['result']

