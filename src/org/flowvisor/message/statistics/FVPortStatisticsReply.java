package org.flowvisor.message.statistics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.util.U16;

public class FVPortStatisticsReply extends OFPortStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {

	private HashMap <String, Object> statsMap = new HashMap<String, Object>();

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			FVClassifier fvClassifier) {
		//statsMap = toMap(msg);
		
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
        if (fvSlicer == null) {
                FVLog.log(LogLevel.WARN, fvClassifier,
                                "dropping unclassifiable port stats reply: " + this);
                return;
        }
        for (Iterator<OFStatistics> it = msg.getStatistics().iterator(); it
                        .hasNext();) {
                OFStatistics stat = it.next();
                if (stat instanceof OFPortStatisticsReply) {
                        OFPortStatisticsReply portStat = (OFPortStatisticsReply) stat;
                        if (!fvSlicer.portInSlice(portStat.getPortNumber())) {
                        		FVLog.log(LogLevel.DEBUG, fvClassifier, "Dropping port ", portStat.getPortNumber(), 
                        				" because it is not in slice ", fvSlicer.getSliceName());
                                it.remove();
                                msg.setLengthU(msg.getLengthU() - portStat.computeLength());
                        }
                }
        }
        if (msg.getStatistics().size() == 0) {
        	FVLog.log(LogLevel.DEBUG, fvClassifier, "Dropping emptied port stats reply");
        	return;
        }
        	
       
        fvSlicer.sendMsg(msg, fvClassifier);
		
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + this);
		
	}
	
	private HashMap<String, Object> toMap(FVStatisticsReply msg) {
		List<OFStatistics> stats = msg.getStatistics();
		HashMap <String,Object> cache = new HashMap<String,Object>();
		
		for (int i=0; i<stats.size(); i++){
			OFPortStatisticsReply reply = (OFPortStatisticsReply) stats.get(i);
			cache.put("port_no", U16.f(reply.getPortNumber()));
			cache.put("rx_packets", reply.getreceivePackets());
			cache.put("tx_packets", reply.getTransmitPackets());
			cache.put("rx_bytes", reply.getReceiveBytes());
			cache.put("tx_bytes", reply.getTransmitBytes());
			cache.put("rx_dropped", reply.getReceiveDropped());
			cache.put("tx_dropped", reply.getTransmitDropped());
			cache.put("rx_errors", reply.getreceiveErrors());
			cache.put("tx_errors", reply.getTransmitErrors());
			cache.put("rx_frame_err", reply.getReceiveFrameErrors());
			cache.put("rx_over_err", reply.getReceiveOverrunErrors());
			cache.put("rx_crc_err", reply.getReceiveCRCErrors());
			cache.put("collisions", reply.getCollisions());
		}
		return cache;
	}
	
	public HashMap<String,Object> getMap(){
		return statsMap;
	}
	
	
	
}
