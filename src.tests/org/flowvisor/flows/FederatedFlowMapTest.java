package org.flowvisor.flows;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.flowvisor.log.DevNullLogger;
import org.flowvisor.log.FVLog;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.U16;

public class FederatedFlowMapTest extends TestCase {
	FederatedFlowMap flowmap;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		FVLog.setDefaultLogger(new DevNullLogger());
		this.flowmap = new FederatedFlowMap();
		FlowEntry.UNIQUE_FLOW_ID = -1;
		FVMatch match = new FVMatch();
		match.fromString("nw_src=128.8.0.0/16");
		FlowEntry flowEntry = new FlowEntry(match, new SliceAction("alice",
				SliceAction.WRITE));
		flowmap.addRule(flowEntry);
		match = new FVMatch();
		match.fromString("nw_src=129.8.0.0/16");
		flowEntry = new FlowEntry(match, new SliceAction("bob",
				SliceAction.WRITE));
		flowmap.addRule(flowEntry);
	}

	public void testIntersectCIDR() {
		FlowMap submap = FlowSpaceUtil.getSubFlowMap(this.flowmap, 1,
				new FVMatch());

		FVMatch packet = new FVMatch();
		packet
				.fromString("OFMatch[in_port=1,dl_dst=00:1c:f0:ed:98:5a,dl_src=00:22:41:fa:73:01,dl_type=2048,dl_vlan=-1,dl_vpcp=0," +
						"nw_dst=3.4.4.5,nw_src=128.8.9.9,nw_proto=1,nw_tos=0,tp_dst=0,tp_src=8]");
		List<FlowIntersect> intersects = submap.intersects(1, packet);
		
		TestCase.assertEquals(1, intersects.size());
		SliceAction action = (SliceAction) intersects.get(0).getFlowEntry().getActionsList().get(0);
		TestCase.assertEquals("alice", action.getSliceName());
	}

	/**
	 * this is a junit test of tests-vlan.py: this must pass before that can
	 * pass
	 *
	 * Make sure that a higher priority FlowEntry with a specific vlan does not
	 * eclipse a match all entry
	 *
	 */
	public void testPacketLookupOnVlanEclipe() {
		FlowMap flowMap = new FederatedFlowMap();
		FlowEntry.UNIQUE_FLOW_ID = -1;
		FVMatch matchVlan = new FVMatch();
		matchVlan.fromString("dl_vlan=512");
		FlowEntry feVlan = new FlowEntry(matchVlan, new SliceAction("alice",
				SliceAction.WRITE));
		feVlan.setPriority(1000);
		flowMap.addRule(feVlan);
		TestCase.assertEquals(512, U16.f(feVlan.getRuleMatch()
				.getDataLayerVirtualLan()));
		FVMatch match = new FVMatch();
		match.fromString("");
		FlowEntry feAll = new FlowEntry(match, new SliceAction("bob",
				SliceAction.WRITE));
		feAll.setPriority(500);
		flowMap.addRule(feAll);

		FVMatch packet = new FVMatch();
		packet.fromString("OFMatch[in_port=3,dl_dst=00:00:00:00:00:01,"
				+ "dl_src=00:00:00:00:00:02,dl_type=2048,dl_vlan=-1,"
				+ "dl_vpcp=0,nw_dst=-64.168.1.40,nw_src=-64.168.0.40,"
				+ "nw_proto=-1,nw_tos=0,tp_dst=0,tp_src=0]");
		List<FlowEntry> list = flowMap.matches(1, packet);
		TestCase.assertNotNull(list);
		TestCase.assertEquals(1, list.size());

		OFAction ofAction = list.get(0).getActionsList().get(0);
		TestCase.assertTrue(ofAction instanceof SliceAction);
		SliceAction sliceAction = (SliceAction) ofAction;
		TestCase.assertEquals("bob", sliceAction.getSliceName());

		FlowMap subMap = FlowSpaceUtil.getSubFlowMap(flowMap, 1, new FVMatch());
		list = subMap.matches(1, packet);
		TestCase.assertNotNull(list);
		TestCase.assertEquals(1, list.size());

		ofAction = list.get(0).getActionsList().get(0);
		TestCase.assertTrue(ofAction instanceof SliceAction);
		sliceAction = (SliceAction) ofAction;
		TestCase.assertEquals("bob", sliceAction.getSliceName());

	}

	public void testKKArp() {
		FlowMap fmap = new FederatedFlowMap();
		short port = 1;
		long dpid = 1;

		FVMatch matchsome = new FVMatch();
		matchsome.fromString("nw_proto=17,tp_src=8080");
		FlowEntry flowEntry = new FlowEntry(matchsome, new SliceAction("ncast",
				SliceAction.WRITE));
		flowEntry.setPriority(1000);
		fmap.addRule(flowEntry);

		FVMatch matchall = new FVMatch();
		matchall.fromString("");
		flowEntry = new FlowEntry(matchall, new SliceAction("prod",
				SliceAction.WRITE));
		flowEntry.setPriority(500);
		fmap.addRule(flowEntry);

		int[] arppacket_ints = { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00,
				0x23, 0xae, 0x35, 0xfd, 0xf3, 0x08, 0x06, 0x00, 0x01, 0x08,
				0x00, 0x06, 0x04, 0x00, 0x01, 0x00, 0x23, 0xae, 0x35, 0xfd,
				0xf3, 0x0a, 0x4f, 0x01, 0x69, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x0a, 0x4f, 0x01, 0x9f, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00 };
		byte[] arppacket = new byte[arppacket_ints.length];
		for (int i = 0; i < arppacket_ints.length; i++)
			arppacket[i] = (byte) arppacket_ints[i];
		FVMatch arp = new FVMatch();
		arp.loadFromPacket(arppacket, port);

		List<FlowEntry> flowEntries = fmap.matches(dpid, arp);
		TestCase.assertEquals(1, flowEntries.size());
		String slice = ((SliceAction) flowEntries.get(0).getActionsList()
				.get(0)).getSliceName();
		TestCase.assertEquals("prod", slice);

		FlowMap subflowmap = FlowSpaceUtil.getSubFlowMap(fmap, dpid, matchall);
		flowEntries = subflowmap.matches(dpid, arp);
		TestCase.assertEquals(1, flowEntries.size());
		slice = ((SliceAction) flowEntries.get(0).getActionsList().get(0))
				.getSliceName();
		TestCase.assertEquals("prod", slice);
	}

	public void testFlowSpaceOverlap() {
		FlowMap map = new FederatedFlowMap();
		FVMatch match = new FVMatch();
		match.fromString("nw_src=128.8.0.0/16");
		FVMatch match2 = new FVMatch();
		match2.fromString("nw_dst=129.9.0.0/16");

		FlowEntry flowEntry1 = new FlowEntry(match, new SliceAction("alice",
				SliceAction.WRITE));
		flowEntry1.setPriority(200);
		FlowEntry flowEntry2 = new FlowEntry(match2, new SliceAction("bob",
				SliceAction.WRITE));
		flowEntry2.setPriority(100);
		map.addRule(flowEntry1);
		map.addRule(flowEntry2);

		List<FlowIntersect> intersects = map.intersects(1, match2);
		TestCase.assertEquals(2, intersects.size());

		List<FVMatch> matches = new LinkedList<FVMatch>();
		for (FlowIntersect intersect : intersects) {
			if (intersect.getFlowEntry().hasPermissions("bob",
					SliceAction.WRITE)) {
				matches.add(intersect.getMatch());
			}
		}
		TestCase.assertEquals(1, matches.size());
		TestCase.assertEquals(match2, matches.get(0));

	}
}
