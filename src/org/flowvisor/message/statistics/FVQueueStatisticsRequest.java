package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;

public class FVQueueStatisticsRequest extends OFQueueStatisticsRequest
		implements ClassifiableStatistic, SlicableStatistic, Cloneable {

	
	 @Override
     public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
             FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
                             + msg);
     }

     @Override
     public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
                     FVSlicer fvSlicer) {
             FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
     }


}
