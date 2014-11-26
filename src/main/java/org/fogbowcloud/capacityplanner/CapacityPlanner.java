package org.fogbowcloud.capacityplanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.capacityplanner.queue.EScienceCentralQueue;
import org.fogbowcloud.capacityplanner.resource.AllocationPolicy;
import org.fogbowcloud.infrastructure.core.InfrastructureException;
import org.fogbowcloud.infrastructure.core.InfrastructureManager;

public class CapacityPlanner {

	private final static int DEFAULT_EXECUTION_PERIOD = 60;

	private InfrastructureManager infrastructure;
	private AllocationPolicy resourcePlanner;
	private EScienceCentralQueue queue;
	private ScheduledFuture<?> plannerHandle;
	private Map<String, String> instanceProperties;
	private int executionInterval;
	private List<String> resourcesNotAvailable = new ArrayList<String>();
	private List<String> resourcesInUse = new ArrayList<String>();

	private static final Logger LOGGER = Logger.getLogger(CapacityPlanner.class);

	public CapacityPlanner(Map<String, String> instanceProperties) {
		this(instanceProperties, DEFAULT_EXECUTION_PERIOD);
	}

	public CapacityPlanner(Map<String, String> instanceProperties, int executionInterval) {
		if (executionInterval <= 0) {
			throw new IllegalArgumentException("Execution interval must be a positive integer.");
		}

		if (instanceProperties == null || instanceProperties.isEmpty()) {
			throw new IllegalArgumentException("Instance properties must not be a null or empty.");
		}
		this.executionInterval = executionInterval;
		this.instanceProperties = instanceProperties;
	}

	public void setInfrastructure(InfrastructureManager infrastructure) {
		this.infrastructure = infrastructure;
	}

	public void setResourcePlanner(AllocationPolicy resourcePlanner) {
		this.resourcePlanner = resourcePlanner;
	}

	public void setQueue(EScienceCentralQueue queue) {
		this.queue = queue;
	}

	protected void setResourcesNotAvailable(List<String> resourcesNotAvailable) {
		this.resourcesNotAvailable = resourcesNotAvailable;
	}

	protected void setResourcesInUse(List<String> resourcesInUse) {
		this.resourcesInUse = resourcesInUse;
	}

	public List<String> getResourcesNotAvailable() {
		return resourcesNotAvailable;
	}

	public List<String> getResourcesInUse() {
		return resourcesInUse;
	}

	public void initialize() throws CapacityPlannerException {
		LOGGER.info("Initializing Capacity Planner execution.");

		if (infrastructure == null || queue == null || resourcePlanner == null) {
			throw new CapacityPlannerException(
					"You need set infrastructure, queue and resourcePlanner firstly.");
		}

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		plannerHandle = executor.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {			
				runCapacityPanner();			
			}

		}, 0, executionInterval, TimeUnit.MINUTES);
	}

	protected void runCapacityPanner() {
		LOGGER.info("Running CapacityPlanner ...");
		try {
			monitoringResources();
			
			LOGGER.debug("resourcesInUse=" + resourcesInUse.size() + ", resourcesNotAvailable="
					+ resourcesNotAvailable.size() + ", queueLength=" + queue.getLength());
			int neededResources = resourcePlanner.calculateResourceNeeds(resourcesInUse.size(),
					resourcesNotAvailable.size(), queue.getLength());
			
			LOGGER.debug("neededResources=" + neededResources);
			if (neededResources != 0) {
				if (neededResources > 0) {
					allocateResources(neededResources);
				} else {
					// if neededResouces is negative. it means that it is possible
					// deallocate resources
					deallocateResources(Math.abs(neededResources));
				}
			}			
		} catch (Throwable e) {
			LOGGER.error("Some throwable happened while running capacity planner.", e);
		}
	}

	protected void monitoringResources() {
		LOGGER.info("Monitoring resources ...");
		// monitoring in use resources
		List<String> inUseClone = new ArrayList<String>();
		inUseClone.addAll(resourcesInUse);
		for (String resourceId : inUseClone) {
			if (!infrastructure.isResourceAvailable(resourceId)) {
				resourcesNotAvailable.add(resourceId);
				resourcesInUse.remove(resourceId);
			}
		}

		// monitoring not available resources
		List<String> notAvailableClone = new ArrayList<String>();
		notAvailableClone.addAll(resourcesNotAvailable);
		for (String resourceId : notAvailableClone) {
			if (infrastructure.isResourceAvailable(resourceId)) {
				resourcesInUse.add(resourceId);
				resourcesNotAvailable.remove(resourceId);
			}
		}
	}

	private void deallocateResources(int numberOfResources) {
		LOGGER.debug("Deallocating " + numberOfResources + " resources.");

		for (int i = 0; i < numberOfResources; i++) {
			// Firstly it deallocates resources not available yet
			if (resourcesNotAvailable.size() > 0) {
				deallocateResource(resourcesNotAvailable.get(0), false);
			} else if (resourcesInUse.size() > 0) {
				deallocateResource(resourcesInUse.get(0), true);
			} else {
				LOGGER.warn("Trying to deallocate resource, but there is not any resource allocated.");
			}
		}
	}

	private void deallocateResource(String resourceId, boolean inUse) {
		LOGGER.debug("Deallocating resource " + resourceId);
		try {
			infrastructure.deleteResource(resourceId);
			if (inUse) {
				resourcesInUse.remove(resourceId);
			} else {
				resourcesNotAvailable.remove(resourceId);
			}
		} catch (InfrastructureException e) {
			LOGGER.error("Error while deallocating resource.", e);
		}
	}

	private void allocateResources(int numberOfResources) {
		LOGGER.debug("Allocating more " + numberOfResources + " resources.");
		try {
			List<String> resourceIds = infrastructure.createResource(numberOfResources,
					instanceProperties);
			LOGGER.debug("New resource IDs=" + resourceIds);
			resourcesNotAvailable.addAll(resourceIds);
		} catch (InfrastructureException e) {
			LOGGER.error("Error while creating more resources.", e);
		}
	}

	public boolean isInitialized() {
		return plannerHandle != null && !plannerHandle.isCancelled();
	}

	public void stop() {
		LOGGER.info("Stoping Capacity Planner execution.");
		if (plannerHandle != null) {
			plannerHandle.cancel(false);
		}
		plannerHandle = null;
	}

}
