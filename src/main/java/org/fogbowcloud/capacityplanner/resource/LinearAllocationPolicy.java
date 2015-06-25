package org.fogbowcloud.capacityplanner.resource;

import java.util.Properties;

import org.apache.log4j.Logger;

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
	private static final Logger LOGGER = Logger.getLogger(LinearAllocationPolicy.class);

	public LinearAllocationPolicy(Properties properties) {
		LOGGER.info("Creating Allocation Policy with properties="+properties);
		try {
			setJobsPerResource(Integer.parseInt(properties.getProperty(JOBS_PER_RESOURCE_KEY)));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"Number of jobs per resource must be a positive integer.");
		} catch (NullPointerException e) {
			throw new IllegalArgumentException(
					"Number of jobs per resource must be a positive integer.");
		}
	}

	private void setJobsPerResource(int jobsPerResource) {
		LOGGER.debug("jobsPerResource=" + jobsPerResource);
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
	public int calculateResourceNeeds(int numberOfEngines, int currentQueueLength) {
		if (currentQueueLength == 1) {
			currentQueueLength = jobsPerResource;
		}

		int neededResources = currentQueueLength / jobsPerResource;
		if (currentQueueLength % jobsPerResource != 0){
			neededResources++;
		}
		
		if (currentQueueLength <= numberOfEngines) {
			return currentQueueLength;
		} else if (neededResources <= currentQueueLength){
			return Math.max(neededResources, numberOfEngines);
		} else {
			return neededResources;
		}
	}
}
