package org.fogbowcloud.capacityplanner;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.capacityplanner.queue.EScienceCentralQueue;
import org.fogbowcloud.capacityplanner.resource.AllocationPolicy;
import org.fogbowcloud.infrastructure.core.InfrastructureManager;

public class CapacityPlanner {

	public final static int DEFAULT_EXECUTION_PERIOD = 1;

	private InfrastructureManager infraManager;
	private AllocationPolicy allocationPolicy;
	private EScienceCentralQueue queue;
	private ScheduledFuture<?> plannerHandle;
	private int executionInterval;

	private static final Logger LOGGER = Logger.getLogger(CapacityPlanner.class);

	public CapacityPlanner() {
		this(DEFAULT_EXECUTION_PERIOD);
	}

	public CapacityPlanner(int executionInterval) {
		if (executionInterval <= 0) {
			throw new IllegalArgumentException("Execution interval must be a positive integer.");
		}
		this.executionInterval = executionInterval;
	}

	public void setAllocationPolicy(AllocationPolicy allocationPolicy) {
		this.allocationPolicy = allocationPolicy;
	}

	public void setQueue(EScienceCentralQueue queue) {
		this.queue = queue;
	}

	public void setInfraManager(InfrastructureManager infraManager) {
		this.infraManager = infraManager;
	}

	public void initialize() throws CapacityPlannerException {
		LOGGER.info("Initializing Capacity Planner execution.");

		if (infraManager == null || queue == null || allocationPolicy == null) {
			throw new CapacityPlannerException(
					"You need set infraManager, queue and allocationPolicy firstly.");
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
		int numberOfEngines = infraManager.getNumberOfStartedEngines();
		LOGGER.info("Current numberOfEngines=" + numberOfEngines);
		int currentQueueSize = queue.getLength();
		int neededResources = allocationPolicy.calculateResourceNeeds(numberOfEngines,
				currentQueueSize);
		LOGGER.info("currentQueueSize=" + currentQueueSize);
		LOGGER.info("neededResources=" + neededResources);
		infraManager.updateCurrentNeeds(neededResources);
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
