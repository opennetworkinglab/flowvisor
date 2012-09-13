/**
 *
 */
package org.flowvisor.flows;

/**
 * @author capveg Describe the overlap between matches A and B
 */
public enum MatchType {
	UNKNOWN, // not yet calculated
	NONE, // no overlap between A and B
	SUBSET, // match A is a subset of B
	SUPERSET, // match A is a superset of match B
	INTERSECT, // match A and B overlap, but neither subsumes the other
	EQUAL; // match A and B are exactly the same
}
