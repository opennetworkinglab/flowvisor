/**
 *
 */
package org.flowvisor.events;

import org.flowvisor.classifier.FVSendMsg;
import org.flowvisor.message.FVMessageFactory;
import org.openflow.protocol.OFEchoRequest;
import org.openflow.protocol.OFType;

/**
 * @author capveg
 *
 */
public class OFKeepAlive extends FVTimerEvent {
	long lastPongTime;
	long timeout;
	long pingPeriod;
	int xid;
	private final FVMessageFactory offactory;
	FVEventLoop loop;
	private final FVSendMsg sendMsg;

	public OFKeepAlive(FVEventHandler handler, FVSendMsg sendMsg,
			FVEventLoop loop) {
		super(0, handler, handler, null);
		this.loop = loop;
		this.sendMsg = sendMsg;
		this.pingPeriod = 5000; // 5 seconds
		this.timeout = 10100; // ~10 seconds == 2 ping periods + a little
		this.lastPongTime = System.currentTimeMillis();
		this.xid = 100;
		this.offactory = new FVMessageFactory();
	}

	/**
	 * @return the timeout
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout
	 *            the timeout to set
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return the pingPeriod
	 */
	public long getPingPeriod() {
		return pingPeriod;
	}

	/**
	 * @param pingPeriod
	 *            the pingPeriod to set
	 */
	public void setPingPeriod(long pingPeriod) {
		this.pingPeriod = pingPeriod;
	}

	public void sendPing() {
		OFEchoRequest ping = (OFEchoRequest) offactory
				.getMessage(OFType.ECHO_REQUEST);
		ping.setXid(xid++);
		ping.computeLength();
		this.sendMsg.sendMsg(ping, this.sendMsg);
	}

	public void registerPong() {
		lastPongTime = System.currentTimeMillis();
	}

	/**
	 * Have we gotten a pong in the timeout period?
	 *
	 * @return
	 */
	public boolean isAlive() {
		return ((this.lastPongTime + this.timeout) > System.currentTimeMillis());
	}

	public void scheduleNextCheck() {
		this.setExpireTime(System.currentTimeMillis() + this.pingPeriod);
		loop.addTimer(this);
	}
}
