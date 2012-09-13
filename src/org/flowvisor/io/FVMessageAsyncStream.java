package org.flowvisor.io;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.flowvisor.classifier.FVSendMsg;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.exceptions.BufferFull;
import org.flowvisor.exceptions.MalformedOFMessage;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.log.SendRecvDropStats;
import org.flowvisor.log.SendRecvDropStats.FVStatsType;
import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.factory.OFMessageFactory;

public class FVMessageAsyncStream extends OFMessageAsyncStream {
	FVEventHandler source;
	FVSendMsg sender;
	SendRecvDropStats stats;
	int consecutiveDropped;
	static int DroppedMessageThreshold = 1000;

	public FVMessageAsyncStream(SocketChannel sock,
			OFMessageFactory messageFactory, FVEventHandler source,
			SendRecvDropStats stats) throws IOException {
		super(sock, messageFactory);
		// OF messages are small, so this is
		// a big performance boost
		sock.socket().setTcpNoDelay(true);
		sock.socket().setSendBufferSize(1024 * 1024);
		this.source = source;
		this.sender = (FVSendMsg) source; // FIXME: currently assumes all
											// FVEventHandlersimplement
											// FVSendMsg
		this.stats = stats;
		this.consecutiveDropped = 0;
	}

	public void testAndWrite(OFMessage m) throws BufferFull,
			MalformedOFMessage, IOException {
		int len = m.getLengthU();
		if (this.outBuf.remaining() < len) {
			this.flush(); // try a quick write to flush buffer
			if (this.outBuf.remaining() < len) {
				// drop message; throw error if we've dropped too many
				if (this.stats != null)
					this.stats.increment(FVStatsType.DROP, this.sender, m);
				this.consecutiveDropped++;
				FVLog.log(LogLevel.WARN, source,
						"wanted to write " + m.getLengthU() + " bytes to "
								+ outBuf.capacity()
								+ " byte buffer, but only have space for "
								+ outBuf.remaining() + " :: dropping msg " + m);
				if (consecutiveDropped > DroppedMessageThreshold) {
					throw new BufferFull("dropped more than "
							+ DroppedMessageThreshold
							+ " in a row; resetting connection");
				}
				return;
			} else
				FVLog.log(LogLevel.WARN, source,
						"Emergency buffer flush: was full, now ",
						outBuf.remaining(), " of ", outBuf.capacity(),
						" bytes free");
		}
		int start = this.outBuf.position();
		super.write(m);
		if (this.stats != null)
			this.stats.increment(FVStatsType.SEND, (FVSendMsg) source, m);
		this.consecutiveDropped = 0;
		int wrote = this.outBuf.position() - start;
		if (len != wrote) { // was the packet correctly written
			// no! back it out and throw an error
			this.outBuf.position(start);
			FVLog.log(LogLevel.CRIT, null, "dropping bad OF Message: " + m);
			throw new MalformedOFMessage("len=" + len + ",wrote=" + wrote
					+ " msg=" + m);
		}
	}

	@Override
	public List<OFMessage> read(int limit) throws IOException {
		List<OFMessage> list = super.read(limit);
		if (list != null)
			for (OFMessage m : list)
				this.stats.increment(FVStatsType.RECV, this.sender, m);
		return list;

	}
}
