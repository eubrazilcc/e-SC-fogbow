package org.fogbowcloud.infrastructure.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class InfrastructureManager {

	public static final int DEFAULT_MONITORING_INTERVAL = 1;
	public static final String START_ENGINE_COMMAND = "ls -lh"; //TODO update the command here
	private InfrastructureProvider infraProvider;
	private List<String> resourcesNotAvailable = new ArrayList<String>();
	private List<String> resourcesInUse = new ArrayList<String>();
	private ScheduledFuture<?> monitoringHandle;
	private Map<String, String> instanceProperties;
	private Map<String, String> instanceCredentials;
	private int monitoringInterval;

	private static final Logger LOGGER = Logger.getLogger(InfrastructureManager.class);

	public InfrastructureManager(Map<String, String> instanceProperties, Map<String, String> instanceCredentials) {
		this(instanceProperties, instanceCredentials, DEFAULT_MONITORING_INTERVAL);
	}

	public InfrastructureManager(Map<String, String> instanceProperties,
			Map<String, String> instanceCredentials, int monitoringInterval) {
		setInstanceProperties(instanceProperties);
		setInstanceCredentials(instanceCredentials);
		setMonitoringInterval(monitoringInterval);
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

	public void setInstanceCredentials(Map<String, String> instanceCredentials) {
		if (instanceCredentials == null || instanceCredentials.isEmpty()) {
			throw new IllegalArgumentException("Instance credentials must not be a null or empty.");
		}
		this.instanceCredentials = instanceCredentials;
	}

	public void setInstanceProperties(Map<String, String> instanceProperties) {
		if (instanceProperties == null || instanceProperties.isEmpty()) {
			throw new IllegalArgumentException("Instance properties must not be a null or empty.");
		}
		this.instanceProperties = instanceProperties;
	}

	public void setMonitoringInterval(int monitoringInterval) {
		if (monitoringInterval <= 0) {
			throw new IllegalArgumentException("Execution interval must be a positive integer.");
		}
		this.monitoringInterval = monitoringInterval;
	}

	public void setInfraProvider(InfrastructureProvider infraProvider) {
		if (infraProvider == null) {
			throw new IllegalArgumentException("InfrastructureProvider must not be null.");
		}
		this.infraProvider = infraProvider;
	}

	public int getNumberOfStartedEngines() {
		return resourcesInUse.size();
	}

	public void updateCurrentNeeds(int neededResources) {
		LOGGER.info("Updating current needs...");
		
		int currentResources = resourcesInUse.size() + resourcesNotAvailable.size();
		LOGGER.info("neededResources=" + neededResources + " and currentResources="
				+ currentResources + " (inUse=" + resourcesInUse.size() + " notAvailable="
				+ resourcesNotAvailable.size() + ")");
		
		if (currentResources > neededResources) {
			deallocateResources(currentResources - neededResources);
		} else if (currentResources < neededResources) {
			allocateResources(neededResources - currentResources);
		}
		
		if (currentResources != 0 || neededResources != 0) {	
			if (monitoringHandle == null || monitoringHandle.isCancelled()) {
				ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
				monitoringHandle = executor.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						monitoringResources();
					}
					
				}, 0, monitoringInterval, TimeUnit.MINUTES);
			}
		}
	}

	protected void monitoringResources() {
		LOGGER.info("Monitoring resources ...");
		// monitoring in use resources
		List<String> inUseClone = new ArrayList<String>();
		inUseClone.addAll(resourcesInUse);
		for (String resourceId : inUseClone) {
			if (!infraProvider.isResourceAvailable(resourceId)) {
				LOGGER.debug("Changing resourceId=" + resourceId
						+ " from resourcesInUse to resourceNotAvailable.");
				resourcesNotAvailable.add(resourceId);
				resourcesInUse.remove(resourceId);
			}
		}

		// monitoring not available resources
		List<String> notAvailableClone = new ArrayList<String>();
		notAvailableClone.addAll(resourcesNotAvailable);
		for (String resourceId : notAvailableClone) {
			if (infraProvider.isResourceAvailable(resourceId)) {
				try {
					CommandResult result = infraProvider.executeCommand(
							infraProvider.getResourceInfo(resourceId), instanceCredentials,
							START_ENGINE_COMMAND);
					LOGGER.debug("commandResult=" + result);
					if (result.getExitStatus() == 0){ //success
						LOGGER.debug("Changing resourceId=" + resourceId
								+ " from resourcesNotAvailable to resourceInUse.");
						resourcesInUse.add(resourceId);
						resourcesNotAvailable.remove(resourceId);
					}
				} catch (InfrastructureException e) {
					LOGGER.error("There was an exception while trying to execute command= "
							+ START_ENGINE_COMMAND + " in resourceId=" + resourceId + ".", e);
				}
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
			infraProvider.deleteResource(resourceId);
			if (inUse) {
				resourcesInUse.remove(resourceId);
			} else {
				resourcesNotAvailable.remove(resourceId);
			}
		} catch (Exception e) {
			LOGGER.error("Error while deallocating resource.", e);
		}
	}

	private void allocateResources(int numberOfResources) {
		LOGGER.debug("Allocating more " + numberOfResources + " resources.");
		LOGGER.debug("InfraProvider=" + infraProvider);
		try {
			List<String> resourceIds = infraProvider.createResource(numberOfResources,
					instanceProperties);
			LOGGER.debug("New resource IDs=" + resourceIds);
			resourcesNotAvailable.addAll(resourceIds);
		} catch (Exception e) {
			LOGGER.error("Error while creating more resources.", e);
		}
	}

}
