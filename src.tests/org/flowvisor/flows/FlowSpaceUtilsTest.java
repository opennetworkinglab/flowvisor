package org.flowvisor.flows;

import org.flowvisor.config.ConfDBHandler;
import org.flowvisor.config.FVConfigurationController;

import java.util.Set;

import junit.framework.TestCase;

import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.config.LoadConfig;
import org.flowvisor.log.DevNullLogger;
import org.flowvisor.log.FVLog;
import org.flowvisor.openflow.protocol.FVMatch;

import org.openflow.protocol.OFPort;
import org.openflow.util.U16;

public class FlowSpaceUtilsTest extends TestCase {

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FVLog.setDefaultLogger(new DevNullLogger());
		FlowEntry.UNIQUE_FLOW_ID = 0;
        FVConfigurationController.init(new ConfDBHandler());
		LoadConfig.defaultConfig("0fw0rk");
	}

	public void testByDPID() {
		long dpid = 1;
		Set<String> slices = FlowSpaceUtil.getSlicesByDPID(FVConfig
				.getFlowSpaceFlowMap(), dpid);
		TestCase.assertEquals(3, slices.size());
		TestCase.assertTrue(slices.contains("alice"));
		TestCase.assertTrue(slices.contains("bob"));
        TestCase.assertTrue(slices.contains("fvadmin"));
	}

	public void testByPort() {
		long dpid = 1;
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(dpid, "alice",
				FVConfig.getFlowSpaceFlowMap());
		TestCase.assertEquals(3, ports.size());
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 0)));
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 2)));
		TestCase.assertTrue(ports.contains(Short.valueOf((short) 3)));
	}

	public void testByPortAll() {
		// matches all ports and all dpids
		FVMatch aliceMatch = new FVMatch();
		aliceMatch.fromString("dl_src=00:00:00:00:00:01");
		FlowEntry flowEntry1 = new FlowEntry(aliceMatch, new SliceAction(
				"alice", SliceAction.WRITE));
		flowEntry1.setPriority(1);
		FVMatch bobMatch = new FVMatch();
		bobMatch.fromString("in_port=3,dl_src=00:00:00:00:00:02");
		FlowEntry flowEntry2 = new FlowEntry(bobMatch, new SliceAction("bob",
				SliceAction.WRITE));
		flowEntry2.setPriority(2); // has higher priority over fe1
		FlowMap flowMap = new LinearFlowMap();
		flowMap.addRule(flowEntry1);
		flowMap.addRule(flowEntry2);
		Set<Short> ports = FlowSpaceUtil.getPortsBySlice(1, "alice", flowMap);
		TestCase.assertEquals(1, ports.size());
		TestCase.assertTrue(ports.contains(OFPort.OFPP_ALL.getValue()));
		// get this subFlowMap with dpid=1
		FlowMap subFlowMap = FlowSpaceUtil.getSubFlowMap(flowMap, 1,
				new FVMatch());
		ports = FlowSpaceUtil.getPortsBySlice(1, "alice", subFlowMap);
		TestCase.assertEquals(1, ports.size());
		TestCase.assertTrue(ports.contains(OFPort.OFPP_ALL.getValue()));
	}

	public void testGetSubFlowSpace() {
		long dpid = 57;
		FlowMap flowMap = null;
		try {
			flowMap = FlowSpaceUtil.getSubFlowMap(dpid);
		} catch (ConfigError e) {
			e.printStackTrace();
		}
		TestCase.assertEquals(10, flowMap.countRules());
		for (FlowEntry flowEntry : flowMap.getRules())
			TestCase.assertEquals(dpid, flowEntry.getDpid());
	}

	public void testDPIDPrinting() {
		// problem with leading zeros: #158
		String str = "03:2b:00:26:f1:3f:3b:00";
		long dpid = FlowSpaceUtil.parseDPID(str);
		String result = FlowSpaceUtil.dpidToString(dpid);
		TestCase.assertEquals(str, result);
	}

	/**
	 * this is a junit test of tests-vlan.py: this must pass before that can
	 * pass
	 *
	 * Make sure that a higher priority FlowEntry with a specific vlan does not
	 * eclipse a match all entry
	 *
	 */
	public void testVlanEclipe() {
		FlowMap flowMap = new LinearFlowMap();
		FVMatch matchVlan = new FVMatch();
		matchVlan.fromString("dl_vlan=512");
		FlowEntry feVlan = new FlowEntry(matchVlan, new SliceAction("alice",
				SliceAction.WRITE));
		feVlan.setPriority(1000);
		flowMap.addRule(feVlan);

		TestCase.assertEquals(512, U16.f(feVlan.getRuleMatch()
				.getDataLayerVirtualLan()));
		FlowEntry feAll = new FlowEntry(new FVMatch(), new SliceAction("bob",
				SliceAction.WRITE));
		feAll.setPriority(500);
		flowMap.addRule(feAll);

		Set<String> slices = FlowSpaceUtil.getSlicesByDPID(flowMap, 1);
		TestCase.assertTrue(slices.contains("alice"));
		TestCase.assertTrue(slices.contains("bob"));
       	TestCase.assertTrue(slices.contains("fvadmin"));
		TestCase.assertEquals(3, slices.size());

		FlowMap subMap = FlowSpaceUtil.getSubFlowMap(flowMap, 1, new FVMatch());

		slices = FlowSpaceUtil.getSlicesByDPID(subMap, 1);
		TestCase.assertTrue(slices.contains("alice"));
		TestCase.assertTrue(slices.contains("bob"));
        TestCase.assertTrue(slices.contains("fvadmin"));
		TestCase.assertEquals(3, slices.size());

	}
	/*
	 * TODO: Need to fix!
	 *
	 * public void testDPID2Str() { long dpid = 0xffffffffffffffffl; String
	 * good_str = "ff:ff:ff:ff:ff:ff:ff:ff"; String test_str =
	 * FlowSpaceUtil.dpidToString(dpid); TestCase.assertEquals(good_str,
	 * test_str); long test_dpid = FlowSpaceUtil.parseDPID(test_str);
	 * TestCase.assertEquals(dpid, test_dpid); }
	 *
	 *
	 *
	 *
	 *
	 * public void testDPID2Str2() { long dpid = 0x9fffffffffffaf00l; String
	 * good_str = "9f:ff:ff:ff:ff:ff:af:00"; String test_str =
	 * FlowSpaceUtil.dpidToString(dpid); TestCase.assertEquals(good_str,
	 * test_str); long test_dpid = FlowSpaceUtil.parseDPID(test_str);
	 * TestCase.assertEquals(dpid, test_dpid); }
	 */
}
