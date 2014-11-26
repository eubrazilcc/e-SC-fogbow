package org.fogbowcloud.capacityplanner.resource;

public interface AllocationPolicy {

	/**
	 * Implements the number of resources to be allocated or deallocated
	 * according to some Allocation Policy. If it is necessary to deallocate
	 * some resource the value returned will be a negative integer representing
	 * the number of resources to be deallocated.
	 * 
	 * @param inUse
	 * @param notAvailableYet
	 * @param currentQueueLength
	 * @return Number of resources to be allocated. If this number is negative
	 *         one, it means that resources can be deallocated.
	 */
	public int calculateResourceNeeds(int inUse, int notAvailableYet, int currentQueueLength);

}
