/**
 *
 */
package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;

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
	
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier, FVSlicer fvSlicer);

	/*public void sliceFromController(List<OFStatistics> approvedStats, FVClassifier fvClassifier,
			FVSlicer fvSlicer) throws StatDisallowedException;*/
}
