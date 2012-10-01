package org.flowvisor.config;

import junit.framework.TestCase;

import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.log.DevNullLogger;
import org.flowvisor.log.FVLog;
import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFMatch;

public class BracketParseTest extends TestCase {

	@Override
	protected void setUp() {
		FVLog.setDefaultLogger(new DevNullLogger());
	}
	public void testBracketParse() {
		FVMatch match = new FVMatch();
		match.setWildcards(OFMatch.OFPFW_ALL & (~OFMatch.OFPFW_IN_PORT));
		match.setInputPort((short) 4);

		FlowEntry rule = new FlowEntry(FlowEntry.ALL_DPIDS, match,
				new SliceAction("bob", SliceAction.WRITE));
		String test = rule.toString();
		FlowEntry testRule = FlowEntry.fromString(test);
		TestCase.assertEquals(rule, testRule);
	}
}
