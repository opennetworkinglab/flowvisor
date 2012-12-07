package org.flowvisor.message;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.message.statistics.SlicableStatistic;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFStatisticsMessageBase;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

public class FVStatisticsRequest extends OFStatisticsRequest implements
		Classifiable, Slicable, SanityCheckable, Cloneable {

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		
		
		FVStatisticsRequest original = (FVStatisticsRequest) this.clone();
		
		if (this.statisticType == OFStatisticsType.DESC
				|| this.statisticType == OFStatisticsType.TABLE
				|| this.statisticType == OFStatisticsType.VENDOR) {
			assert (this.getStatistics().size() == 0);
			FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
			return;
		}
		
		List<OFStatistics> newStatsList = new LinkedList<OFStatistics>();
		Iterator<OFStatistics> it = this.getStatistics().iterator();
		while (it.hasNext()) {
			OFStatistics stat = it.next();
			assert (stat instanceof SlicableStatistic);
			try {
				((SlicableStatistic) stat).sliceFromController(newStatsList, fvClassifier,
						fvSlicer);
			} catch (StatDisallowedException e) {
				it.remove();
				this.setLengthU(this.getLengthU() - stat.getLength());
				FVLog.log(LogLevel.WARN, fvSlicer, e.getMessage());
				fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
						e.getError(), this), fvSlicer);
			}
			
		}
	
		this.setStatistics(newStatsList);
		FVMessageUtil.translateXidMsgAndSend(original, this, fvClassifier, fvSlicer);
		
	
	}
	
	public FVStatisticsRequest clone() {
		FVStatisticsRequest clone = new FVStatisticsRequest();
		clone.setFlags(this.flags);
		clone.setLength(this.getLength());
		clone.setStatistics(this.getStatistics());
		clone.setStatisticType(this.statisticType);
		clone.setType(this.type);
		clone.setVersion(this.getVersion());
		clone.setXid(this.getXid());
		return clone;
	}

	@Override
	public String toString() {
		return super.toString() + ";st=" + this.getStatisticType();
		// ";mfr=" + this.getManufacturerDescription() +
	}

	/**
	 * Check to make sure the packet really has all of the statistics is claims
	 * to have. This is really to make sure the framing is correct.
	 */
	@Override
	public boolean isSane() {
		int msgLen = this.getLengthU();
		int count;
		count = OFStatisticsMessageBase.MINIMUM_LENGTH;
		for (OFStatistics stat : this.getStatistics()) {
			count += stat.getLength();
		}
		if (count == msgLen)
			return true;
		else {
			FVLog.log(LogLevel.WARN, null, "msg failed sanity check: ", this);
			return false;
		}
	}

}
