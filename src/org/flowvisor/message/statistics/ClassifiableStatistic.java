/**
 *
 */
package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.openflow.protocol.OFMessage;

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
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier);
}
