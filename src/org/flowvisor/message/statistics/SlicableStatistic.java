/**
 *
 */
package org.flowvisor.message.statistics;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.StatDisallowedException;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFStatistics;

/**
 * @author capveg
 *
 */
public interface SlicableStatistic {

	/**
	 * Given this stat, classifier, and slicer decide how this statistic should
	 * be rewritten coming from the controller
	 *
	 * @param approuvedStats
	 * @param fvClassifier
	 * @param fvSlicer
	 */

	public void sliceFromController(List<OFStatistics> approvedStats, FVClassifier fvClassifier,
			FVSlicer fvSlicer) throws StatDisallowedException;
}
