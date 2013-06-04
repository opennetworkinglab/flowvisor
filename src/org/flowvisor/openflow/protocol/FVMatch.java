package org.flowvisor.openflow.protocol;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.flows.FlowEntry;
import org.openflow.protocol.OFMatch;
import org.openflow.util.HexString;
import org.openflow.util.U16;
import org.openflow.util.U32;
import org.openflow.util.U8;

public class FVMatch extends OFMatch {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/*
	 * TODO: Move this to the flowentry, once XMLRPC API goes away.
	 */
	public static final String STR_QUEUE = "queues";
	public static final String STR_FORCE = "force_enqueue";
	
	private List<Integer> queues = new LinkedList<Integer>();
	private long force_queue = -1;
	 

	public static long ANY_DPID = FlowEntry.ALL_DPIDS;

	/*
	 * ash: defining these here because I can't find them in OpenFlowj
	 * 

	
	/*
	 * Simple Constructor, initially all fields are wildcarded.
	 */
	public FVMatch() {
		super();
	}
	
	public FVMatch(OFMatch match) {
		super();
		this.wildcards = match.getWildcards();
	    inputPort = match.getInputPort();
	    dataLayerSource = match.getDataLayerSource();
	    dataLayerDestination = match.getDataLayerDestination();;
	    dataLayerVirtualLan = match.getDataLayerVirtualLan();
	    dataLayerVirtualLanPriorityCodePoint = match.getDataLayerVirtualLanPriorityCodePoint();
	    dataLayerType = match.getDataLayerType();
	    networkTypeOfService = match.getNetworkTypeOfService();
	    networkProtocol = match.getNetworkProtocol();
	    networkSource = match.getNetworkSource();
	    networkDestination = match.getNetworkDestination();
	    transportSource = match.getTransportSource();
	    transportDestination= match.getTransportDestination();
	    
	}
	
	public void setDataLayerSource(Long mac) {
		byte macArr[] = new byte[] {
				(byte)((mac >> 40) & 0xff),
				(byte)((mac >> 32) & 0xff),
				(byte)((mac >> 24) & 0xff),
				(byte)((mac >> 16) & 0xff),
				(byte)((mac >> 8 ) & 0xff),
				(byte)((mac >> 0) & 0xff),
		};
		
		
		setDataLayerSource(macArr);
	}
	
	public void setDataLayerDestination(Long mac) {
		byte macArr[] = new byte[] {
				(byte)((mac >> 40) & 0xff),
				(byte)((mac >> 32) & 0xff),
				(byte)((mac >> 24) & 0xff),
				(byte)((mac >> 16) & 0xff),
				(byte)((mac >> 8 ) & 0xff),
				(byte)((mac >> 0) & 0xff),
		};
		setDataLayerDestination(macArr);
	}
	
	/**
     * Output a dpctl-styled string, i.e., only list the elements that are not
     * wildcarded
     * 
     * A match-everything OFMatch outputs "OFMatch[]"
     * 
     * @return 
     *         "OFMatch[dl_src:00:20:01:11:22:33,nw_src:192.168.0.0/24,tp_dst:80]"
     */
    @Override
    public String toString() {
    	
        String str = "";

        // l1
        if ((wildcards & OFPFW_IN_PORT) == 0)
            str += "," + STR_IN_PORT + "=" + U16.f(this.inputPort);

        // l2
        if ((wildcards & OFPFW_DL_DST) == 0)
            str += "," + STR_DL_DST + "="
                    + HexString.toHexString(this.dataLayerDestination);
        if ((wildcards & OFPFW_DL_SRC) == 0)
            str += "," + STR_DL_SRC + "="
                    + HexString.toHexString(this.dataLayerSource);
        if ((wildcards & OFPFW_DL_TYPE) == 0)
            str += "," + STR_DL_TYPE + "=0x"
                    + Integer.toHexString(U16.f(this.dataLayerType));
        if ((wildcards & OFPFW_DL_VLAN) == 0)
            str += "," + STR_DL_VLAN + "="
                    + U16.f(this.dataLayerVirtualLan);
        if ((wildcards & OFPFW_DL_VLAN_PCP) == 0)
            str += ","
                    + STR_DL_VLAN_PCP
                    + "="
                    + U8.f(this.dataLayerVirtualLanPriorityCodePoint);
               

        // l3
        if (getNetworkDestinationMaskLen() > 0)
            str += ","
                    + STR_NW_DST
                    + "="
                    + cidrToIp.cidrToString(networkDestination,
                            getNetworkDestinationMaskLen());
        if (getNetworkSourceMaskLen() > 0)
            str += "," + STR_NW_SRC + "="
                    + cidrToIp.cidrToString(networkSource, getNetworkSourceMaskLen());
        if ((wildcards & OFPFW_NW_PROTO) == 0)
            str += "," + STR_NW_PROTO + "=" + this.networkProtocol;
        if ((wildcards & OFPFW_NW_TOS) == 0)
            str += "," + STR_NW_TOS + "=" + this.networkTypeOfService;

        // l4
        if ((wildcards & OFPFW_TP_DST) == 0)
            str += "," + STR_TP_DST + "=" + this.transportDestination;
        if ((wildcards & OFPFW_TP_SRC) == 0)
            str += "," + STR_TP_SRC + "=" + this.transportSource;
        
        /*
         * TODO: queue related stuff should go once XMLRPC API goes away.
         */
        if (this.queues != null && this.queues.size() > 0)
        	str += "," + STR_QUEUE + " = " + this.queues;
        if (this.force_queue != -1) {
        	str += "," + STR_FORCE + " = " + this.force_queue;
        }
        if ((str.length() > 0) && (str.charAt(0) == ','))
            str = str.substring(1); // trim the leading ","
        // done
        return "OFMatch[" + str + "]";
    }

    /*private String cidrToString(int ip, int prefix) {
        String str;
        if (prefix >= 32) {
            str = ipToString(ip);
        } else {
            // use the negation of mask to fake endian magic
            int mask = ~((1 << (32 - prefix)) - 1);
            str = ipToString(ip & mask) + "/" + prefix;
        }

        return str;
    }*/
    
    public static class cidrToIp{
    	public static String cidrToString(int ip, int prefix) {
    		String str;
    		if (prefix >= 32) {
    			str = ipToString(ip);
    	    } else {
    	    	// use the negation of mask to fake endian magic
    	    	int mask = ~((1 << (32 - prefix)) - 1);
    	    	str = ipToString(ip & mask) + "/" + prefix;
    	    }

    		return str;
    	}
    }

    /**
     * Set this OFMatch's parameters based on a comma-separated key=value pair
     * dpctl-style string, e.g., from the output of OFMatch.toString() <br>
     * <p>
     * Supported keys/values include <br>
     * <p>
     * <TABLE border=1>
     * <TR>
     * <TD>KEY(s)
     * <TD>VALUE
     * </TR>
     * <TR>
     * <TD>"in_port","input_port"
     * <TD>integer
     * </TR>
     * <TR>
     * <TD>"dl_src","eth_src", "dl_dst","eth_dst"
     * <TD>hex-string
     * </TR>
     * <TR>
     * <TD>"dl_type", "dl_vlan", "dl_vlan_pcp"
     * <TD>integer
     * </TR>
     * <TR>
     * <TD>"nw_src", "nw_dst", "ip_src", "ip_dst"
     * <TD>CIDR-style netmask
     * </TR>
     * <TR>
     * <TD>"tp_src","tp_dst"
     * <TD>integer (max 64k)
     * </TR>
     * </TABLE>
     * <p>
     * The CIDR-style netmasks assume 32 netmask if none given, so:
     * "128.8.128.118/32" is the same as "128.8.128.118"
     * 
     * @param match
     *            a key=value comma separated string, e.g.
     *            "in_port=5,ip_dst=192.168.0.0/16,tp_src=80"
     * @throws IllegalArgumentException
     *             on unexpected key or value
     */

    public void fromString(String match) throws IllegalArgumentException {
        if (match.equals("") || match.equalsIgnoreCase("any")
                || match.equalsIgnoreCase("all") || match.equals("[]"))
            match = "OFMatch[]";
        String[] tokens = match.split("[\\[,\\]]");
        String[] values;
        int initArg = 0;
        if (tokens[0].equals("OFMatch"))
            initArg = 1;
        this.wildcards = OFPFW_ALL;
        int i;
        for (i = initArg; i < tokens.length; i++) {
            values = tokens[i].split("=");
            if (values.length != 2)
                throw new IllegalArgumentException("Token " + tokens[i]
                        + " does not have form 'key=value' parsing " + match);
            values[0] = values[0].toLowerCase(); // try to make this case insens
            if (values[0].equals(STR_IN_PORT) || values[0].equals("input_port")) {
                this.inputPort = U16.t(Integer.valueOf(values[1]));
                this.wildcards &= ~OFPFW_IN_PORT;
            } else if (values[0].equals(STR_DL_DST)
                    || values[0].equals("eth_dst")) {
                this.dataLayerDestination = HexString.fromHexString(values[1]);
                this.wildcards &= ~OFPFW_DL_DST;
            } else if (values[0].equals(STR_DL_SRC)
                    || values[0].equals("eth_src")) {
                this.dataLayerSource = HexString.fromHexString(values[1]);
                this.wildcards &= ~OFPFW_DL_SRC;
            } else if (values[0].equals(STR_DL_TYPE)
                    || values[0].equals("eth_type")) {
                if (values[1].startsWith("0x"))
                    this.dataLayerType = U16.t(Integer.valueOf(
                            values[1].replaceFirst("0x", ""), 16));
                else
                    this.dataLayerType = U16.t(Integer.valueOf(values[1]));
                this.wildcards &= ~OFPFW_DL_TYPE;
            } else if (values[0].equals(STR_DL_VLAN)) {
            	if (values[1].startsWith("0x")) 
            		this.dataLayerVirtualLan = U16.t(Integer.valueOf(values[1].replaceFirst("0x", ""), 16));
            	else
            		this.dataLayerVirtualLan = U16.t(Integer.valueOf(values[1]));
                this.wildcards &= ~OFPFW_DL_VLAN;
            } else if (values[0].equals(STR_DL_VLAN_PCP)) {
            	if (values[1].startsWith("0x"))
            		this.dataLayerVirtualLanPriorityCodePoint = U8.t(Short
                            .valueOf(values[1].replaceFirst("0x", ""), 16));
            	else
            		this.dataLayerVirtualLanPriorityCodePoint = U8.t(Short
                        .valueOf(values[1]));
                this.wildcards &= ~OFPFW_DL_VLAN_PCP;
            } else if (values[0].equals(STR_NW_DST)
                    || values[0].equals("ip_dst"))
                setFromCIDR(values[1], STR_NW_DST);
            else if (values[0].equals(STR_NW_SRC) || values[0].equals("ip_src"))
                setFromCIDR(values[1], STR_NW_SRC);
            else if (values[0].equals(STR_NW_PROTO)) {
                this.networkProtocol = U8.t(Short.valueOf(values[1]));
                this.wildcards &= ~OFPFW_NW_PROTO;
            } else if (values[0].equals(STR_NW_TOS)) {
                this.networkTypeOfService = U8.t(Short.valueOf(values[1]));
                this.wildcards &= ~OFPFW_NW_TOS;
            } else if (values[0].equals(STR_TP_DST)) {
                this.transportDestination = U16.t(Integer.valueOf(values[1]));
                this.wildcards &= ~OFPFW_TP_DST;
            } else if (values[0].equals(STR_TP_SRC)) {
                this.transportSource = U16.t(Integer.valueOf(values[1]));
                this.wildcards &= ~OFPFW_TP_SRC;
            } else if (values[0].equals(STR_QUEUE)) {
            	this.queues = queueList(values[1]);
            } else if (values[0].equals(STR_FORCE)) {
            	this.force_queue = U32.t(Integer.valueOf(values[1]));  
            } else

                throw new IllegalArgumentException("unknown token " + tokens[i]
                        + " parsing " + match);
        }
    }
    
	public static List<Integer> queueList(String qstr) throws IllegalArgumentException {
		List<Integer> qlist = new LinkedList<Integer>();
		String[] tmp = qstr.split(":");
		for (int i = 0 ; i < tmp.length ; i++) {
			try {
				qlist.add(Integer.parseInt(tmp[i]));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Queue id " + tmp[i] + 
						" is not a valid queue identifier.");
			}
		}
		return qlist;
	}

    /*private void clear() {
    	inputPort = ANY_IN_PORT;
	    dataLayerSource = toByteArray(ANY_MAC);
	    dataLayerDestination = toByteArray(ANY_MAC);;
	    dataLayerVirtualLan = ANY_VLAN_ID;
	    dataLayerVirtualLanPriorityCodePoint = ANY_VLAN_PCP;
	    dataLayerType = ANY_ETHER;
	    networkTypeOfService = ANY_NW_PROTO_TOS;
	    networkProtocol = ANY_NW_PROTO_TOS;
	    networkSource = ANY_NW;
	    networkDestination = ANY_NW;
	    transportSource = ANY_TP;
	    transportDestination= ANY_TP;
	}*/

	/**
     * Set the networkSource or networkDestionation address and their wildcards
     * from the CIDR string
     * 
     * @param cidr
     *            "192.168.0.0/16" or "172.16.1.5"
     * @param which
     *            one of STR_NW_DST or STR_NW_SRC
	 * @return 
     * @throws IllegalArgumentException
     */
    public void setFromCIDR(String cidr, String which)
            throws IllegalArgumentException {
        String values[] = cidr.split("/");
        String[] ip_str = values[0].split("\\.");
        int ip = 0;
        ip += Integer.valueOf(ip_str[0]) << 24;
        ip += Integer.valueOf(ip_str[1]) << 16;
        ip += Integer.valueOf(ip_str[2]) << 8;
        ip += Integer.valueOf(ip_str[3]);
        int prefix = 32; // all bits are fixed, by default

        if (values.length >= 2)
            prefix = Integer.valueOf(values[1]);
        int mask = 32 - prefix;
        if (which.equals(STR_NW_DST)) {
            this.networkDestination = ip;
            this.wildcards = (wildcards & ~OFPFW_NW_DST_MASK)
                    | (mask << OFPFW_NW_DST_SHIFT);
        } else if (which.equals(STR_NW_SRC)) {
            this.networkSource = ip;
            this.wildcards = (wildcards & ~OFPFW_NW_SRC_MASK)
                    | (mask << OFPFW_NW_SRC_SHIFT);
        }
    }


	
	public static long toLong(byte[] byteArray) {
		return new BigInteger(byteArray).longValue();
	}
	

	
    /**
     * Implement clonable interface
     */
    @Override
    public FVMatch clone() {
            FVMatch ret = (FVMatch) super.clone();
            ret.dataLayerDestination = this.dataLayerDestination.clone();
            ret.dataLayerSource = this.dataLayerSource.clone();
            ret.queues = new LinkedList<Integer>(this.queues);
            return ret;
    }
    
    /**
     * Set wildcards
     * 
     * @param wildcards
     */
    public FVMatch setWildcards(int wildcards) {
        this.wildcards = wildcards;
        return this;
    }
    
    public List<Integer> getQueues() {
    	return this.queues;
    }
    
    public void setQueues(List<Integer> qlist) {
    	this.queues = qlist;
    }
    
    public long getForcedQueue() {
    	return this.force_queue;
    }
    
    public void setForcedQueue(long queue) {
    	this.force_queue = queue;
    }
    
    public boolean equals(FVMatch other) {
    	if (this == other)
    		return true;
    	if (other == null)
    		return false;
    	
    	if (canonicalizeWildcards(this.wildcards) != 
    			canonicalizeWildcards(other.wildcards))
    		return false;
    	
    	 if ((wildcards & OFPFW_IN_PORT) == 0 && this.inputPort != other.inputPort)
             return false;

         // l2
         if ((wildcards & OFPFW_DL_DST) == 0 && 
        		 !Arrays.equals(dataLayerDestination, other.dataLayerDestination))
            return false;
         
         if ((wildcards & OFPFW_DL_SRC) == 0 && 
        		 !Arrays.equals(dataLayerSource, other.dataLayerSource))
             return false;
             
         if ((wildcards & OFPFW_DL_TYPE) == 0 && 
        		 this.dataLayerType != other.dataLayerType)
             return false;
         
         if ((wildcards & OFPFW_DL_VLAN) == 0 && 
        		 this.dataLayerVirtualLan != other.dataLayerVirtualLan)
             return false;
         
         if ((wildcards & OFPFW_DL_VLAN_PCP) == 0 && 
        		 this.dataLayerVirtualLanPriorityCodePoint != other.dataLayerVirtualLanPriorityCodePoint)
             return false;                

         // l3
      
         if ((wildcards & OFPFW_NW_PROTO) == 0 && 
        		 this.networkProtocol != other.networkProtocol)
             return false;
         
         if ((wildcards & OFPFW_NW_TOS) == 0 && 
        		 this.networkTypeOfService != other.networkTypeOfService)
        	 return false;
         
         // l4
         if ((wildcards & OFPFW_TP_DST) == 0 && 
        		 this.transportDestination != other.transportDestination)
        	 return false;
         
         
         if ((wildcards & OFPFW_TP_SRC) == 0 && 
         	this.transportSource != other.transportSource)
             return false;
             
         
    	
    	return true;
    }
    
    
    public HashMap<String,Object> toMap() {
    	
        HashMap<String, Object> ret = new HashMap<String, Object>();

        // l1
        if ((wildcards & OFPFW_IN_PORT) == 0)
            ret.put(STR_IN_PORT,U16.f(this.inputPort));

        // l2
        if ((wildcards & OFPFW_DL_DST) == 0)
        	ret.put(STR_DL_DST, HexString.toHexString(this.dataLayerDestination));
           
        if ((wildcards & OFPFW_DL_SRC) == 0)
        	ret.put(STR_DL_SRC, HexString.toHexString(this.dataLayerSource));
           
        if ((wildcards & OFPFW_DL_TYPE) == 0)
        	ret.put(STR_DL_TYPE, U16.f(this.dataLayerType));
        	
   
        if ((wildcards & OFPFW_DL_VLAN) == 0)
        	ret.put(STR_DL_VLAN, U16.f(this.dataLayerVirtualLan));
        	
       
        if ((wildcards & OFPFW_DL_VLAN_PCP) == 0)
        	ret.put(STR_DL_VLAN_PCP, U8.f(this.dataLayerVirtualLanPriorityCodePoint));
        	

        // l3
        if (getNetworkDestinationMaskLen() > 0)
        	ret.put(STR_NW_DST, cidrToIp.cidrToString(networkDestination,
                    getNetworkDestinationMaskLen()));
            
        
        if (getNetworkSourceMaskLen() > 0)
        	ret.put(STR_NW_SRC, cidrToIp.cidrToString(networkSource, getNetworkSourceMaskLen()));
        	
        
        if ((wildcards & OFPFW_NW_PROTO) == 0)
        	ret.put(STR_NW_PROTO, this.networkProtocol);
        	
           
        if ((wildcards & OFPFW_NW_TOS) == 0)
           ret.put(STR_NW_TOS,this.networkTypeOfService);

        // l4
        if ((wildcards & OFPFW_TP_DST) == 0)
        	ret.put(STR_TP_DST,this.transportDestination);
        
        if ((wildcards & OFPFW_TP_SRC) == 0)
            ret.put(STR_TP_SRC,this.transportSource);
        
        return ret;
    }
    
}
