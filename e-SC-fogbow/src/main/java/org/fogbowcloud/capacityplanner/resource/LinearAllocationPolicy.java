package org.fogbowcloud.capacityplanner.resource;

import java.util.Properties;

/**
 * This class implements the calculation for needed resources linearly. Each
 * machine has capacity to execute 'x' jobs. But, if it already exists resources
 * in use, the ResourcePlanner will not deallocate them while queue length is
 * not smaller than the number of resources in use. For example, if the resource
 * capacity is 2 (jobs per resource), the queue length is 10, it is possible
 * check some scenario bellow:
 * 
 * 0 in use and 0 not available yet - 5 resources will be allocated 1 in use and
 * 0 not available yet - 4 resources will be allocated 0 in use and 1 not
 * available yet - 4 resources will be allocated 1 in use and 3 not available
 * yet - 1 resources will be allocated 3 in use and 1 not available yet - 1
 * resources will be allocated 10 in use and 0 not available yet - 0 resource
 * will be deallocated 15 in use and 0 not available yet - 5 resources will be
 * deallocated 12 in use and 3 not available yet - 5 resources will be
 * deallocated 0 in use and 10 not available yet - 5 resources will be
 * deallocated 5 in use and 5 not available yet - 5 resources will be
 * deallocated
 * 
 * @author giovanni
 *
 */
public class LinearAllocationPolicy implements AllocationPolicy {

	public static final String JOBS_PER_RESOURCE_KEY = "linear_allocation_jobs_per_resource";
	private int jobsPerResource;

	public LinearAllocationPolicy(Properties properties) {
		try {
			setJobsPerResource(Integer.parseInt(properties.getProperty(JOBS_PER_RESOURCE_KEY)));
		} catch (NumberFormatException | NullPointerException e) {
			throw new IllegalArgumentException(
					"Number of jobs per resource must be a positive integer.");
		}
	}

	private void setJobsPerResource(int jobsPerResource) {
		if (jobsPerResource <= 0) {
			throw new IllegalArgumentException(
					"Number of jobs per resource must be a positive integer.");
		}
		this.jobsPerResource = jobsPerResource;
	}

	public LinearAllocationPolicy(int jobsPerResource) {
		setJobsPerResource(jobsPerResource);
	}

	@Override
	public int calculateResourceNeeds(int inUse, int notAvailableYet, int currentQueueLength) {

		int totalRequestedResources = inUse + notAvailableYet;
		if (currentQueueLength == 1) {
			currentQueueLength = jobsPerResource;
		}
		
		int neededResources = (int) Math.ceil(currentQueueLength / jobsPerResource);
		if (totalRequestedResources > neededResources) {
			int toDeallocate = totalRequestedResources - neededResources;

			if (toDeallocate > notAvailableYet) {
				return -1 * (inUse - currentQueueLength + notAvailableYet);
			}
			return -1 * toDeallocate;
		}
		return neededResources - totalRequestedResources;
	}

}
