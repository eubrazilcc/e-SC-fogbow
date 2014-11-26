package org.fogbowcloud.capacityplanner.queue;

/**
 * This interface represents the Queue of Jobs from a e-Science Central
 * installation.
 * 
 * @author giovanni
 *
 */
public interface EScienceCentralQueue {

	/**
	 * Gets the length of e-science central queue.
	 * 
	 * @return The length of e-science central queue or -1 if the length is
	 *         undefined.
	 */
	public int getLength();

}
