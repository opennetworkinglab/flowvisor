/**
 *
 */
package org.flowvisor.events;

import org.flowvisor.events.FVEvent;
import java.nio.channels.*;

/**
 * Event: underlying socket has pending I/O
 *
 * @author capveg
 *
 */
public class FVIOEvent extends FVEvent {
	SelectionKey sk;

	public FVIOEvent(SelectionKey sk, FVEventHandler src, FVEventHandler dst) {
		super(src, dst);
		this.sk = sk;
	}

	public SelectionKey getSelectionKey() {
		return sk;
	}
}
