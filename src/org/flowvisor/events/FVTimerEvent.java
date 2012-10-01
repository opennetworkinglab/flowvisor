/**
 *
 */
package org.flowvisor.events;

/**
 * Event: a timer has expired
 *
 * @author capveg
 *
 */
public class FVTimerEvent extends FVEvent implements Comparable<FVTimerEvent> {
	long expireTime;
	static int ID = 0;
	int id;
	Object arg;

	/**
	 * Send an event from src to dst at absolute time expireTime
	 *
	 * @param expireTime
	 *            Absolute wallclock time since the epoch in milliseconds
	 * @param src
	 *            event sender
	 * @param dst
	 *            event destination
	 * @param arg
	 *            caller specifiable argument to the timer
	 */
	public FVTimerEvent(long expireTime, FVEventHandler src,
			FVEventHandler dst, Object arg) {
		super(src, dst);
		this.expireTime = expireTime;
		this.arg = arg;
		synchronized (FVTimerEvent.class) {
			this.id = ID++;
		}
	}

	public long getExpireTime() {
		return expireTime;
	}

	public int getID() {
		return id;
	}

	public Object getArg() {
		return arg;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param expireTime
	 *            the expireTime to set
	 */
	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	/**
	 * @param arg
	 *            the arg to set
	 */
	public void setArg(Object arg) {
		this.arg = arg;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(FVTimerEvent timerEvent) {
		if (timerEvent.expireTime != this.expireTime)
			return (int) (this.expireTime - timerEvent.expireTime);
		else
			return this.id - timerEvent.id;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FVTimerEvent [arg=" + arg + ", expireTime=+"
				+ (expireTime - System.currentTimeMillis()) + "ms, id=" + id
				+ ",src=" + this.getSrc().getName() + "]";
	}
}
