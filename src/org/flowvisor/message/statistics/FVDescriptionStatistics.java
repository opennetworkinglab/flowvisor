package org.flowvisor.message.statistics;

import java.net.InetSocketAddress;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFDescriptionStatistics;

public class FVDescriptionStatistics extends OFDescriptionStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	/**
	 * NOTE: we no long do any DescriptionStatistics rewriting, now that 1.0
	 * support dp_desc field.
	 *
	 * NOTE: now that no 1.0 vendors use dp_desc the way I wanted them to, we're
	 * going to do rewriting again... *sigh*
	 */
	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		// TODO change this if FVDescription Request gets its own type

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
		FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
	}
}
