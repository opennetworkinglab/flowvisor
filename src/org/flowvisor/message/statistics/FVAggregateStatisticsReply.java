package org.flowvisor.message.statistics;

import java.util.HashMap;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVAggregateStatisticsReply extends OFAggregateStatisticsReply
		implements SlicableStatistic, ClassifiableStatistic {

	private HashMap <String, Object> statsMap = new HashMap<String, Object>();

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ msg);
		//statsMap = toMap(msg);
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ msg);
		
	}
	
	private HashMap<String, Object> toMap(FVStatisticsReply msg) {
		List<OFStatistics> stats = msg.getStatistics();
		HashMap <String,Object> cache = new HashMap<String,Object>();
		
		for (int i=0; i<stats.size(); i++){
			OFAggregateStatisticsReply reply = (OFAggregateStatisticsReply) stats.get(i);
			cache.put("packet_count", reply.getPacketCount());
			cache.put("byte_count", reply.getByteCount());
			cache.put("flow_count", reply.getFlowCount());			
		}
		return cache;
	}
	
	public HashMap<String,Object> getMap(){
		return statsMap;
	}
	
}
