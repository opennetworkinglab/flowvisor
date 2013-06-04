package org.flowvisor.message.statistics;

import java.util.HashMap;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFTableStatistics;
import org.openflow.util.HexString;

public class FVTableStatistics extends OFTableStatistics implements
		SlicableStatistic, ClassifiableStatistic {
	
	private HashMap <String, Object> statsMap = new HashMap<String, Object>();

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			FVClassifier fvClassifier) {
		//statsMap = toMap(msg);
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer ==  null) {
			FVLog.log(LogLevel.WARN, fvClassifier, "Dropping unclassifiable message: ", msg);
			return;
		}
		int currentMax = fvClassifier.getMaxAllowedFlowMods(fvSlicer.getSliceName());
		int currentFMs = fvClassifier.getCurrentFlowModCounter(fvSlicer.getSliceName());
		if (currentMax != -1)
			this.setMaximumEntries(currentMax);
		this.setActiveCount(currentFMs);
		fvSlicer.sendMsg(msg, fvClassifier);
		
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ msg);
		
	}

	private HashMap<String, Object> toMap(FVStatisticsReply msg) {
		List<OFStatistics> stats = msg.getStatistics();
		HashMap <String,Object> cache = new HashMap<String,Object>();
		
		for (int i=0; i<stats.size(); i++){
			OFTableStatistics reply = (OFTableStatistics) stats.get(i);
			cache.put("table_id", HexString.toHexString(reply.getTableId()));
			cache.put("name", reply.getName());
			cache.put("wildcards", reply.getWildcards());
			cache.put("max_entries", reply.getMaximumEntries());
			cache.put("active_count", reply.getActiveCount());
			cache.put("lookup_count", reply.getLookupCount());
			cache.put("matched_count", reply.getMatchedCount());
		}
		return cache;
	}
	
	public HashMap<String,Object> getMap(){
		return statsMap;
	}
	
}
