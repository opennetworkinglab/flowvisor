package org.flowvisor.message;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFQueueConfigReply;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.queue.OFPacketQueue;


public class FVQueueConfigReply extends OFQueueConfigReply implements
		Classifiable, Slicable  {

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping unclassifiable xid in QueueConfigReply: " + this);
			return;
		}
		FVMatch match = new FVMatch();
		match.setInputPort(this.port);
		FVLog.log(LogLevel.INFO, fvSlicer, "matching FS");
		List<FlowEntry> entries = fvSlicer.getFlowSpace().matches(fvClassifier.getDPID(), match);
		Iterator<FlowEntry> it = entries.iterator();
		while (it.hasNext()) {
			FVLog.log(LogLevel.INFO, fvSlicer, "pruning FS");
			FlowEntry fe = it.next();
			for (OFAction act : fe.getActionsList()) {
				SliceAction sa = (SliceAction) act;
				if (!sa.getSliceName().equals(fvSlicer.getSliceName()))
					it.remove();
			}
		}
		boolean found = false;
		List<Integer> queuelog = new LinkedList<Integer>();
		Iterator<OFPacketQueue> qit = this.queues.iterator();
		while (qit.hasNext()) {
			FVLog.log(LogLevel.INFO, fvSlicer, "matching queue reply");
			OFPacketQueue queue = qit.next();
			queuelog.add(queue.getQueueId());
			for (FlowEntry fe : entries) {
				if (fe.getQueueId().contains(queue.getQueueId())) {
					found = true;
					break;
				} 
			}
			if (!found) {
				FVLog.log(LogLevel.INFO, fvClassifier, "Pruning queue " + queue.getQueueId() 
						+ " because it is not in slice " + fvSlicer.getSliceName());
				qit.remove();
				this.setLengthU(this.getLengthU() - queue.computeLength());
			}
			
		}
		if (!found) {
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping QueueConfigReply because queues " + queuelog + 
					" are not in slice: " + fvSlicer.getSliceName() + " : " + this);
			return;
		}
		if (fvSlicer.portInSlice(this.port))
			fvSlicer.sendMsg(this, fvClassifier);
		else 
			FVLog.log(LogLevel.WARN, fvClassifier,
					"dropping QueueConfigReply because port is not in slice: " + 
					fvSlicer.getSliceName() + " : " + this);
	}

}