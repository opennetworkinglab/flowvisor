package org.flowvisor.ofswitch;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.events.FVEvent;
import org.flowvisor.events.FVEventHandler;
import org.flowvisor.events.FVEventLoop;
import org.flowvisor.events.FVIOEvent;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.flowvisor.resources.SlicerLimits;

public class OFSwitchAcceptor implements FVEventHandler {
	FVEventLoop pollLoop;

	int backlog;
	int listenPort;
	ServerSocketChannel ssc;
	List<FVClassifier> switches;

	private SlicerLimits slicerLimits;

	public OFSwitchAcceptor(FVEventLoop pollLoop, int port, int backlog)
			throws IOException {
		this.pollLoop = pollLoop;

		ssc = ServerSocketChannel.open();
		ssc.socket().setReuseAddress(true);
		try {
			// FIXME: check -Djava.net.preferIPv4Stack=true instead of brute force?
			// try IPv6 first; this works on dual stacked machines too
			ssc.socket().bind(
					new InetSocketAddress(InetAddress.getByName("::"), port),
					backlog);
		} catch (java.net.SocketException e) {
			// try default/ipv4 if that fails
			try {
				FVLog.log(LogLevel.NOTE, this, "failed to bind IPv6 address; trying IPv4");
				ssc.socket().bind(
						new InetSocketAddress(port),
						backlog);
			} catch (BindException be) {
				FVLog.log(LogLevel.FATAL, this, "Unable to listen on port " + port + 
						" on localhost; verify that nothing else is running on that port.");
				System.out.println("OF Control address already in use.");
				System.exit(1);
			} catch (java.net.SocketException se) {
				FVLog.log(LogLevel.NOTE, this, "failed to bind IPv4 address; Quitting");
				FVLog.log(LogLevel.NOTE, this, "OF Control address already in use.");
				e.printStackTrace();
				System.exit(1);
			} 
		} 
		ssc.configureBlocking(false);
		this.listenPort = ssc.socket().getLocalPort();

		FVLog.log(LogLevel.INFO, this, "Listenning on port " + this.listenPort);

		// register this module with the polling loop
		pollLoop.register(ssc, SelectionKey.OP_ACCEPT, this);
	}

	@Override
	public boolean needsConnect() {
		return false;
	}

	@Override
	public boolean needsRead() {
		return false;
	}

	@Override
	public boolean needsWrite() {
		return false;
	}

	@Override
	public boolean needsAccept() {
		return true;
	}

	/**
	 * @return the listenPort
	 */
	public int getListenPort() {
		return listenPort;
	}

	@Override
	public long getThreadContext() {
		return pollLoop.getThreadContext();
	}

	@Override
	public void tearDown() {

		try {
			ssc.close();
		} catch (IOException e) {
			// ignore if shutting down throws an error... we're already shutting
			// down
		}
	}

	@Override
	public void handleEvent(FVEvent e) throws UnhandledEvent {
		if (Thread.currentThread().getId() != this.getThreadContext()) {
			// this event was sent from a different thread context
			pollLoop.queueEvent(e); // queue event
			return; // and process later
		}
		if (e instanceof FVIOEvent)
			handleIOEvent((FVIOEvent) e);
		else
			throw new UnhandledEvent(e);
	}

	void handleIOEvent(FVIOEvent event) {
		SocketChannel sock = null;

		try {
			sock = ssc.accept();
			if (sock == null) {
				FVLog.log(LogLevel.CRIT, null,
						"ssc.accept() returned null !?! FIXME!");
				return;
			}
			FVLog.log(LogLevel.INFO, this, "got new connection: " + sock);
			FVClassifier fvc = new FVClassifier(pollLoop, sock);
			fvc.setSlicerLimits(this.slicerLimits);
			fvc.init();
		} catch (IOException e) // ignore IOExceptions -- is this the right
		// thing to do?
		{
			FVLog.log(LogLevel.CRIT, this, "Got IOException for "
					+ (sock != null ? sock : "unknown socket :: ") + e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return "OFSwitchAcceptor";
	}

	public void setSlicerLimits(SlicerLimits slicerLimits) {
		this.slicerLimits = slicerLimits;	
	}
}
