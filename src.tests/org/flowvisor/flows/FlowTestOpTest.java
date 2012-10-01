package org.flowvisor.flows;

import junit.framework.TestCase;

import org.flowvisor.log.DevNullLogger;
import org.flowvisor.log.FVLog;
import org.flowvisor.openflow.protocol.FVMatch;


public class FlowTestOpTest extends TestCase {

	@Override
	protected void setUp() {
		// don't do logging in unittests
		FVLog.setDefaultLogger(new DevNullLogger());
	}

	public void testCIDRMatch() {
		FlowEntry flowEntry = new FlowEntry(new FVMatch()
				.setWildcards(FVMatch.OFPFW_ALL), new SliceAction("alice",
				SliceAction.WRITE));
		FlowIntersect intersect = new FlowIntersect(flowEntry);
		int rip = 0xaabbccdd;
		int ip = FlowTestOp.testFieldMask(intersect,
				FVMatch.OFPFW_NW_DST_SHIFT, 32, 0, rip, 0x00000000);
		TestCase.assertEquals(rip, ip);
		int dmask = intersect.getMatch().getNetworkDestinationMaskLen();
		TestCase.assertEquals(32, dmask);
		int goodBits = (~FVMatch.OFPFW_NW_DST_MASK) & FVMatch.OFPFW_ALL;
		int wildcards = intersect.getMatch().getWildcards();
		TestCase.assertEquals(goodBits, wildcards);

		rip = 0;
		ip = FlowTestOp.testFieldMask(intersect, FVMatch.OFPFW_NW_SRC_SHIFT, 0,
				0, rip, 0x00000000);
		TestCase.assertEquals(rip, ip);
		dmask = intersect.getMatch().getNetworkSourceMaskLen();
		TestCase.assertEquals(0, dmask);
		wildcards = intersect.getMatch().getWildcards();
		TestCase.assertEquals(goodBits, wildcards);
	}
}
