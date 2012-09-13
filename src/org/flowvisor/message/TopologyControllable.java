/**
 *
 */
package org.flowvisor.message;

import org.flowvisor.ofswitch.TopologyConnection;

/**
 * Messages that are handled by the topology controller implement the
 * TopologyControllerable interface
 *
 * @author capveg
 *
 */
public interface TopologyControllable {
	void topologyController(TopologyConnection topologyConnection);
}
