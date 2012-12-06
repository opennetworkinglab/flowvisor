/**
 *
 */
package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFStatistics;

/**
 * @author capveg
 *
 */
public interface ClassifiableStatistic {
	/**
	 * Given this msg and classifier, figure out which slice this message is
	 * for, rewrite anything as necessary, and send it onto the slice's
	 * controller
	 *
	 * @param msg
	 * @param fvClassifier
	 */
	public void classifyFromSwitch(List<OFStatistics> approvedStats, FVClassifier fvClassifier,
			FVSlicer fvSlicer) throws StatDisallowedException;
}
