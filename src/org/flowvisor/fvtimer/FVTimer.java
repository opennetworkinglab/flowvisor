package org.flowvisor.fvtimer;

import java.sql.Time;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.flowvisor.events.FVEventUtils;
import org.flowvisor.events.FVTimerEvent;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/***
 * A priority queue of timer events does NOT actually call events directly.
 * External callers (e.g., eventLoop) do addTimer() and repeated call
 * processTimer()
 *
 * NOT threadsafe!
 *
 * @author capveg
 *
 */
public class FVTimer {
	public static final long MAX_TIMEOUT = 5000;
	public static final long MIN_TIMEOUT = 1; // timeout == 0 implies infinite!

	PriorityQueue<FVTimerEvent> pq;

	public FVTimer() {
		pq = new PriorityQueue<FVTimerEvent>();
		if (pq == null)
			throw new NullPointerException();
	}

	public void addTimer(FVTimerEvent e) {
		FVLog.log(LogLevel.MOBUG, e.getSrc(), "Scheduleing event ", e.getId()
				+ " at t=" + new Time(System.currentTimeMillis())
				+ " to happen at " + new Time(e.getExpireTime()));
		pq.add(e);
		FVLog.log(LogLevel.MOBUG, null, "Events in timer queue: ", pq.size());
	}

	public void logEventQueue(String prefix, LogLevel level) {
		for (FVTimerEvent e : this.pq) {
			FVLog.log(level, null, prefix + " " + e);
		}
	}

	/***
	 * Compare the current wall clock time to the next event in the queue. If
	 * there is nothing in the queue, return MAX_TIMEOUT If the time for this
	 * event has passed, process it (only one event per call) and return
	 * MIN_TIMEOUT Else, return the time in milliseconds until the next event
	 */
	public long processEvent() throws UnhandledEvent {
		long now = System.currentTimeMillis();
		FVTimerEvent e = this.pq.peek();

		while ((e != null) && (e.getExpireTime() <= now)) {
			pq.remove();
			FVLog.log(LogLevel.MOBUG, e.getDst(), "processing event ", e
					.getId(), " scheduling err = ", (now - e.getExpireTime()));
			long startCounter = System.currentTimeMillis();
			e.getDst().handleEvent(e);
			FVEventUtils.starvationTest(startCounter, e.getDst(), e);
			e = this.pq.peek();
		}

		if (e == null)
			return MAX_TIMEOUT;
		else
			return Math.min(e.getExpireTime() - now, MAX_TIMEOUT);
	}

	/****
	 * Cancels a timer that has previously been added via addTimer()
	 *
	 * @param id
	 *            the id of the timer as returned by FVTimerEvent.getID()
	 * @return true if found and removed, else false
	 */
	boolean removeTimer(int id) {
		FVTimerEvent e;
		Iterator<FVTimerEvent> it = pq.iterator();
		for (e = it.next(); it.hasNext(); e = it.next()) {
			if (e.getID() == id) {
				pq.remove(e);
				return true;
			}
		}
		return false;
	}
}
