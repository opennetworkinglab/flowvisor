/**
 *
 */
package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;

/**
 * @author capveg
 *
 */
public interface SlicableStatistic {

	/**
	 * Given this msg, classifier, and slicer decide how this statistic should
	 * be rewritten coming from the controller
	 *
	 * @param msg
	 * @param fvClassifier
	 * @param fvSlicer
	 */

	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer);
}
