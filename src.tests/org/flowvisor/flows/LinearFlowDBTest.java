package org.flowvisor.flows;

import junit.framework.TestCase;

import org.flowvisor.log.DevNullLogger;
import org.flowvisor.log.FVLog;
import org.flowvisor.message.FVFlowMod;
import org.flowvisor.message.FVMessageFactory;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.OFMessageFactory;

public class LinearFlowDBTest extends TestCase {

	public void testInsert() {
		OFMessageFactory factory = new FVMessageFactory();
		LinearFlowDB flowDB = new LinearFlowDB(null);
		FVFlowMod flowMod1 = (FVFlowMod) factory.getMessage(OFType.FLOW_MOD);
		FVFlowMod flowMod2 = (FVFlowMod) factory.getMessage(OFType.FLOW_MOD);
		FVFlowMod flowMod3 = (FVFlowMod) factory.getMessage(OFType.FLOW_MOD);

		FVLog.setDefaultLogger(new DevNullLogger());
		flowDB.processFlowMod(flowMod1, 1, "alice");
		flowDB.processFlowMod(flowMod2, 1, "alice");
		flowDB.processFlowMod(flowMod3, 1, "alice");

		TestCase.assertEquals(3, flowDB.size());
	}

}
