package org.flowvisor.classifier;

import org.flowvisor.log.SendRecvDropStats;
import org.openflow.protocol.OFMessage;

public interface FVSendMsg {
	public void sendMsg(OFMessage msg, FVSendMsg from);

	public void dropMsg(OFMessage msg, FVSendMsg from);

	public SendRecvDropStats getStats();

	public String getConnectionName();

	public String getName();
}
