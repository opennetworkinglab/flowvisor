/**
 *
 */
package org.flowvisor.config;

import java.util.Map;

/**
 *
 * This entire thing is a horrible hack around the fact that my XML encoder
 * sucks
 *
 * I should probably find a better way of dealing with that...
 *
 * @author capveg
 *
 */
public interface Bracketable<E> {
	public Map<String, String> toBracketMap();

	public E fromBacketMap(Map<String, String> bracketMap);
}
