package org.flowvisor.flows;

import java.util.Arrays;

import org.openflow.protocol.OFMatch;

public class FlowTestOp {
	private enum TestOp {
		LEFT, // no test, just return the LEFT param
		RIGHT, // no test, just return the RIGHT param
		EQUAL, // test if they are equal
		WILD
		// no test, both are wildcards
	}

	/**
	 * What type of test should we do on these params?
	 *
	 * This is the common code for all of the tests
	 *
	 * @param flowIntersect
	 * @param wildIndex
	 * @param wildX
	 * @param wildY
	 * @return
	 */
	static TestOp whichTest(FlowIntersect flowIntersect, int wildIndex,
			int wildX, int wildY) {
		TestOp ret;
		if (((wildX & wildIndex) == 0) && ((wildY & wildIndex) == 0)) { // is
			// neither
			// field
			// wildcarded?
			ret = TestOp.EQUAL;
		} else if ((wildX & wildIndex) == 0) { // is just X not wildcarded?
			ret = TestOp.LEFT;
			flowIntersect.maybeSuperset = false;
		} else if ((wildY & wildIndex) == 0) { // is just Y not wildcarded?
			ret = TestOp.RIGHT;
			flowIntersect.maybeSubset = false;
		} else
			ret = TestOp.WILD; // both are wildcarded!

		OFMatch interMatch = flowIntersect.getMatch();
		int wildCards = interMatch.getWildcards();
		if (ret != TestOp.WILD)
			interMatch.setWildcards(wildCards & (~wildIndex)); // disable
		// wildCards for
		// this field
		else
			interMatch.setWildcards(wildCards | wildIndex); // enable wildCards
		// for this field
		return ret;
	}

	static short testFieldShort(FlowIntersect flowIntersect, int wildIndex,
			int wildX, int wildY, short x, short y) {

		switch (whichTest(flowIntersect, wildIndex, wildX, wildY)) {
		case EQUAL:
			if (x != y)
				flowIntersect.setMatchType(MatchType.NONE);
			return x; // or y; doesn't matter if they are equal
		case LEFT:
			return x;
		case WILD: // or y; doesn't matter if they are wild
		case RIGHT:
			return y;
		}
		assert (false); // should never get here
		return -1;
	}

	static long testFieldLong(FlowIntersect flowIntersect, int wildIndex,
			int wildX, int wildY, long x, long y) {

		switch (whichTest(flowIntersect, wildIndex, wildX, wildY)) {
		case EQUAL:
			if (x != y)
				flowIntersect.setMatchType(MatchType.NONE);
			return x; // or y; doesn't matter if they are equal
		case LEFT:
			return x;
		case WILD: // or y; doesn't matter if they are wild
		case RIGHT:
			return y;
		}
		assert (false); // should never get here
		return -1;
	}

	static byte testFieldByte(FlowIntersect flowIntersect, int wildIndex,
			int wildX, int wildY, byte x, byte y) {

		switch (whichTest(flowIntersect, wildIndex, wildX, wildY)) {
		case EQUAL:
			if (x != y)
				flowIntersect.setMatchType(MatchType.NONE);
			return x; // or y; doesn't matter if they are equal
		case LEFT:
			return x;
		case WILD: // or y; doesn't matter if they are wild
		case RIGHT:
			return y;
		}
		assert (false); // should never get here
		return -1;
	}

	static byte[] testFieldByteArray(FlowIntersect flowIntersect,
			int wildIndex, int wildX, int wildY, byte x[], byte y[]) {

		switch (whichTest(flowIntersect, wildIndex, wildX, wildY)) {
		case EQUAL:
			if (!Arrays.equals(x, y))
				flowIntersect.setMatchType(MatchType.NONE);
			return x; // or y; doesn't matter if they are equal
		case LEFT:
			return x;
		case WILD: // or y; doesn't matter if they are wild
		case RIGHT:
			return y;
		}
		assert (false); // should never get here
		return null;
	}

	// see if ip prefix x/masklenX intersects with y/masklenY (CIDR-style)
	static int testFieldMask(FlowIntersect flowIntersect, int maskShift,
			int masklenX, int masklenY, int x, int y) {
		int min = Math.min(masklenX, masklenY); // get the less specific address
		int max = Math.max(masklenX, masklenY); // get the more specific address
		int min_encoded = 32 - min; // because OpenFlow does it backwards... grr
		int max_encoded = 32 - max; // because OpenFlow does it backwards... grr
		if (max_encoded >= 32) // set all the bits if this is in fact fully
			max_encoded = 63; // wildcarded; if only for wireshark's sake

		int mask;
		if (min == 0)
			mask = 0; // nasty work around for stupid signed ints
		else
			mask = ~((1 << min_encoded) - 1); // min < 32, so no signed issues
		// int mask = (1 << min) - 1;

		if ((x & mask) != (y & mask)) // if these are not in the same CIDR block
			flowIntersect.setMatchType(MatchType.NONE);
		// else there is some overlap
		OFMatch interMatch = flowIntersect.getMatch();
		int wildCards = interMatch.getWildcards();
		// turn off all bits for this match and then turn on the used ones
		// use MAX not MIN, because we want the most specific intersection
		// split into two ops, so we can see intermediate step in debugger
		// assumes SRC mask == DST mask
		// turn off all bits for this match (making it an exact match)
		wildCards = wildCards
				& ~(((1 << OFMatch.OFPFW_NW_SRC_BITS) - 1) << maskShift);
		// turn on the bits for the intersection
		wildCards = wildCards | max_encoded << maskShift;
		interMatch.setWildcards(wildCards);
		if (masklenX < masklenY) {
			flowIntersect.maybeSubset = false;
			return y;
		} else if (masklenX > masklenY) {
			flowIntersect.maybeSuperset = false;
			return x;
		}

		// note that b/c of how CIDR addressing works, there is no overlap that
		// is not a SUB or SUPERSET
		return x; // x == y; doesn't matter
	}
}
