package org.flowvisor.message.statistics;

import java.net.InetSocketAddress;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;

public class FVDescriptionStatistics extends OFDescriptionStatistics implements
		SlicableStatistic, ClassifiableStatistic {


	/**
	 * NOTE: we no long do any DescriptionStatistics rewriting, now that 1.0
	 * support dp_desc field.
	 *
	 * NOTE: now that no 1.0 vendors use dp_desc the way I wanted them to, we're
	 * going to do rewriting again... *sigh*
	 */
	@Override
	public void classifyFromSwitch(OFMessage original, List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		if (fvClassifier.wantStatsDescHack()) {
			InetSocketAddress remote = (InetSocketAddress) fvClassifier
					.getSocketChannel().socket().getRemoteSocketAddress();

			this.datapathDescription += " (" + remote.getAddress().getHostAddress() + ":"
					+ remote.getPort() + ")";

			if (this.datapathDescription.length() > FVDescriptionStatistics.DESCRIPTION_STRING_LENGTH)
				this.datapathDescription = this.datapathDescription.substring(
						0,
						FVDescriptionStatistics.DESCRIPTION_STRING_LENGTH - 1);
		}
		approvedStats.add(this);
		
	}

	@Override
	public void sliceFromController(List<OFStatistics> approvedStats,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws StatDisallowedException {
		
		FVLog.log(LogLevel.INFO, fvSlicer, "FVDescriptions requests have no body; message is illegal. Dropping: ", this);
		return;
		
	}
}
