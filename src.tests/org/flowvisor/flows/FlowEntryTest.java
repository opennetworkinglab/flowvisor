package org.flowvisor.flows;

import junit.framework.TestCase;

import org.flowvisor.config.LoadConfig;
import org.flowvisor.log.DevNullLogger;
import org.flowvisor.log.FVLog;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.U16;

public class FlowEntryTest extends TestCase {

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FVLog.setDefaultLogger(new DevNullLogger());
		FlowEntry.UNIQUE_FLOW_ID = -1;
		LoadConfig.defaultConfig("blah"); // setup a default config
	}


	public void testFlowMatch() {
		FVMatch allmatch = new FVMatch();
		allmatch.setWildcards(FVMatch.OFPFW_ALL);
		FVMatch goodmatch = new FVMatch();
		goodmatch.fromString("nw_proto=8");

		FVMatch badmatch = new FVMatch();
		badmatch.fromString("nw_proto=3");

		// match is an XXXX of rule
		FlowEntry flowEntryAll = new FlowEntry(1, allmatch, (OFAction) null);
		TestCase.assertEquals(MatchType.EQUAL, flowEntryAll
				.matches(1, allmatch).getMatchType());
		TestCase.assertEquals(MatchType.SUBSET, flowEntryAll.matches(1,
				goodmatch).getMatchType());
		TestCase.assertEquals(MatchType.SUBSET, flowEntryAll.matches(1,
				badmatch).getMatchType());

		FlowEntry flowEntrySpecific = new FlowEntry(1, goodmatch,
				(OFAction) null);
		TestCase.assertEquals(MatchType.SUPERSET, flowEntrySpecific.matches(1,
				allmatch).getMatchType());
		TestCase.assertEquals(MatchType.EQUAL, flowEntrySpecific.matches(1,
				goodmatch).getMatchType());
		TestCase.assertEquals(MatchType.NONE, flowEntrySpecific.matches(1,
				badmatch).getMatchType());

	}

	public void testCIDRFlowMatch() {
		FVMatch allmatch = new FVMatch();
		allmatch.setWildcards(FVMatch.OFPFW_ALL);
		FVMatch l32badmatch = new FVMatch();
		l32badmatch.fromString("nw_dst=192.168.255.4");
		FVMatch l32match = new FVMatch();
		l32match.fromString("nw_dst=192.168.6.4");
		FVMatch l24match = new FVMatch();
		l24match.fromString("nw_dst=192.168.6.0/24");
		FVMatch l8match = new FVMatch();
		l8match.fromString("nw_dst=192.0.0.0/8");

		// rule is an {SUBSET, SUPERSET, EQUAL} of match
		FlowEntry flowEntry24 = new FlowEntry(1, l24match, (OFAction) null);
		TestCase.assertEquals(MatchType.SUBSET, flowEntry24
				.matches(1, l32match).getMatchType());
		TestCase.assertEquals(MatchType.EQUAL, flowEntry24.matches(1, l24match)
				.getMatchType());
		TestCase.assertEquals(MatchType.SUPERSET, flowEntry24.matches(1,
				allmatch).getMatchType());
		TestCase.assertEquals(MatchType.SUPERSET, flowEntry24.matches(1,
				l8match).getMatchType());
		TestCase.assertEquals(MatchType.NONE, flowEntry24.matches(1,
				l32badmatch).getMatchType());
	}

	public void testDL_VLAN_PCP() {
		FVMatch vpcp = new FVMatch();
		vpcp.setWildcards(~FVMatch.OFPFW_DL_VLAN_PCP);
		vpcp.setDataLayerVirtualLanPriorityCodePoint((byte) 3);

		FVMatch vpcp2 = new FVMatch();
		vpcp2.fromString("dl_vpcp=3");

		FlowEntry flowEntry = new FlowEntry(1, vpcp, (OFAction) null);
		TestCase.assertEquals(MatchType.EQUAL, flowEntry.matches(1, vpcp)
				.getMatchType());
		TestCase.assertEquals(MatchType.EQUAL, flowEntry.matches(1, vpcp2)
				.getMatchType());

	}

	public void testClone() {
		FVMatch match = new FVMatch();
		match.fromString("nw_src=128.8.0.0/16");
		FlowEntry flowEntry = new FlowEntry(1, match, new SliceAction("alice",
				SliceAction.WRITE));
		FlowEntry neo = flowEntry.clone();

		TestCase.assertEquals(match, neo.getRuleMatch());
	}

	public void testIntersect() {
		FVMatch match = new FVMatch();
		match.fromString("nw_src=118.8.0.0/16");
		FlowEntry flowEntry = new FlowEntry(1, match, new SliceAction("alice",
				SliceAction.WRITE));
		FlowIntersect intersect = flowEntry.matches(1, match);
		int good_src_mask = match.getNetworkSourceMaskLen();
		int test_src_mask = intersect.getMatch().getNetworkSourceMaskLen();
		TestCase.assertEquals(good_src_mask, test_src_mask);
		int good_dst_mask = match.getNetworkDestinationMaskLen();
		int test_dst_mask = match.getNetworkDestinationMaskLen();
		TestCase.assertEquals(good_dst_mask, test_dst_mask);
		int wc1 = match.getWildcards();
		int wc2 = intersect.getMatch().getWildcards();
		TestCase.assertEquals(wc1, wc2);
		TestCase.assertEquals(match, intersect.getMatch());
	}

	public void testIntersectExact() {
		FVMatch pingMatch = new FVMatch();
		FVMatch allMatch = new FVMatch();
		FlowEntry pingFE = new FlowEntry(pingMatch, new SliceAction("alice",
				SliceAction.WRITE));
		FlowEntry allFE = new FlowEntry(allMatch, new SliceAction("alice",
				SliceAction.WRITE));
		pingMatch
				.fromString("dl_src=00:11:22:33:44:55,dl_dst=66:77:88:99:aa:bb,"
						+ "dl_type=8100,"
						+ "nw_src=1.2.3.4,nw_dst=5.6.7.8,nw_proto=1,tp_src=0,tp_dst=0");
		FlowIntersect pingIntoAll = allFE.matches(1, pingMatch);
		TestCase.assertEquals(pingMatch, pingIntoAll.getMatch());
		FlowIntersect allIntoPing = pingFE.matches(1, allMatch);
		TestCase.assertEquals(pingMatch, allIntoPing.getMatch());

	}

	public void testFVMatchFormat() {
		FVMatch match = new FVMatch();
		match.fromString("tp_dst=51365,nw_src=108.22.0.0/15");
		// works if doesn't fail assert?
		TestCase.assertEquals(51365, U16.f(match.getTransportDestination()));
	}
}
