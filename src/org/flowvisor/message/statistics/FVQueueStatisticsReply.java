package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFQueueStatisticsReply;

public class FVQueueStatisticsReply extends OFQueueStatisticsReply implements
		ClassifiableStatistic, SlicableStatistic {


	
    @Override
    public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
                    FVSlicer fvSlicer) {
            FVLog.log(LogLevel.WARN, fvSlicer, "dropping unexpected msg: " + msg);
    }

    /**
     * No need to rewrite response
     */

    @Override
    public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
            FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
           /* if (!fvSlicer.portInSlice(this.portNumber)) {
    			throw new StatDisallowedException("Port " + this.portNumber + 
    					" is not in slice " + fvSlicer.getSliceName(), OFBadRequestCode.OFPBRC_EPERM);
    		}
    			
    		FVMatch testMatch = new FVMatch();
    		testMatch.setInputPort(this.portNumber);
    		List<FlowIntersect> intersections = 
    				fvSlicer.getFlowSpace().intersects(fvClassifier.getDPID(), testMatch);
    		for (FlowIntersect inter : intersections) {
    			if (inter.getFlowEntry().getRuleMatch().getQueues().contains(this.queueId)) {
    				for (OFAction act : inter.getFlowEntry().getActionsList()) {
    					assert(act instanceof SliceAction);
    					SliceAction sa = (SliceAction) act;
    					if (sa.getSliceName().equals(fvSlicer.getSliceName())) 
    							approvedStats.add(this);
    					
    				}
    			}
    		}
    		if (approvedStats.size() == 0)
    			throw new StatDisallowedException("QueueId " + this.queueId + 
    					" is not in slice " + fvSlicer.getSliceName(), OFBadRequestCode.OFPBRC_EPERM);*/
    }


}
