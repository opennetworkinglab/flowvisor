/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;

/**
 * The interface for classifying this message and sending it on to the correct
 * FVSlicer instance
 *
 * Does switch-specific, slice agnostic rewriting
 *
 * @author capveg
 *
 */
public interface Classifiable {

	/**
	 * Given a message from a switch, send it to the appropriate FVSlicer
	 * instance(s)
	 *
	 * Possibly do some rewriting, record state, or even drop
	 *
	 * @param fvClassifier
	 *            Switch state
	 */
	public void classifyFromSwitch(FVClassifier fvClassifier);
}
