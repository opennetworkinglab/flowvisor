package org.flowvisor.message.statistics;

import java.util.List;
import java.util.Set;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class FVPortStatisticsRequest extends OFPortStatisticsRequest implements
		ClassifiableStatistic, SlicableStatistic, Cloneable {



	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
	
		
		if (!fvSlicer.isAllowAllPorts() && this.portNumber == OFPort.OFPP_NONE.ordinal()) {
			Set<Short> ports = fvSlicer.getPorts();
			for (Short port : ports) {
				FVPortStatisticsRequest portReq = this.clone();
				portReq.portNumber = port;
				approvedStats.add(portReq);
			}
			return;
		}
		
		if (fvSlicer.portInSlice(this.portNumber)) {
			approvedStats.add(this);
			return;
		}
		throw new StatDisallowedException("Port " + this.portNumber + 
				" is not in slice " + fvSlicer.getSliceName(), OFBadRequestCode.OFPBRC_EPERM);
		
	}
	
	public FVPortStatisticsRequest clone() {
		FVPortStatisticsRequest req = new FVPortStatisticsRequest();
		req.portNumber = this.portNumber;
		return req;
	}

	@Override
	public void classifyFromSwitch(OFMessage original,
			List<OFStatistics> approvedStats, FVClassifier fvClassifier,
			FVSlicer fvSlicer) throws StatDisallowedException {
		FVLog.log(LogLevel.WARN, fvClassifier, "dropping unexpected msg: "
				+ this);
		
	}

}
