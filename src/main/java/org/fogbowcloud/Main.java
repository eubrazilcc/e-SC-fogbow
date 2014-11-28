package org.fogbowcloud;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.capacityplanner.CapacityPlanner;
import org.fogbowcloud.capacityplanner.CapacityPlannerException;
import org.fogbowcloud.capacityplanner.queue.EScienceCentralQueue;
import org.fogbowcloud.capacityplanner.queue.FakeESCentralQueue;
import org.fogbowcloud.capacityplanner.resource.AllocationPolicy;
import org.fogbowcloud.capacityplanner.resource.LinearAllocationPolicy;
import org.fogbowcloud.infrastructure.core.InfrastructureException;
import org.fogbowcloud.infrastructure.core.InfrastructureManager;
import org.fogbowcloud.infrastructure.core.ResourcePropertiesConstants;
import org.fogbowcloud.infrastructure.fogbow.FogbowInfrastructureManager;

public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);
	private static final int DEFAULT_INFRA_CONF_UPDATE_INTERVAL = 60;

	public static void main(String[] args) {

		String configurationFilePath = args[0];

		Properties properties = new Properties();
		FileInputStream input;
		try {
			input = new FileInputStream(configurationFilePath);
			properties.load(input);
	
			// creating infrastructure
			InfrastructureManager infrastructure;
			String infraClass = properties.getProperty(ConfigurationConstants.INFRASTRUCTURE_CLASS);
			if (infraClass != null) {
				infrastructure = (InfrastructureManager) createInstance(
						ConfigurationConstants.INFRASTRUCTURE_CLASS, properties);
			} else {
				infrastructure = new FogbowInfrastructureManager(properties);
			}
			
			String infraEndpoint = properties.getProperty(ConfigurationConstants.INFRA_ENDPOINT);
			Map<String, String> infraCredentials = getPropertiesByPrefix(properties,
					ConfigurationConstants.INFRA_CREDENTIALS_PREFIX);
			
			infrastructure.configure(new HashMap<String, String>(infraCredentials));
			
			//TODO updating configure!		
			String infraConfUpdateIntervalStr = properties.getProperty(ConfigurationConstants.INFRA_CONF_UPDATE_INTERVAL);	
			int infraConfUpdateInterval;
			try {
				infraConfUpdateInterval = Integer.parseInt(infraConfUpdateIntervalStr);
			} catch (Exception e) {
				infraConfUpdateInterval = DEFAULT_INFRA_CONF_UPDATE_INTERVAL;
			}
			triggerInfraConfigurationUpdater(infrastructure, infraEndpoint, infraCredentials,
					infraConfUpdateInterval);
			
			// creating queue
			EScienceCentralQueue queue;
			String queueClass = properties.getProperty(ConfigurationConstants.QUEUE_CLASS);
			if (queueClass != null) {
				queue= (EScienceCentralQueue) createInstance(
						ConfigurationConstants.QUEUE_CLASS, properties);
			} else {
				queue = new FakeESCentralQueue(properties);
			}
			
			// creating resource planner 
			AllocationPolicy resourcePlanner;
			String resourceClass = properties.getProperty(ConfigurationConstants.RESOURCE_PLANNER_CLASS);
			if (resourceClass != null) {
				resourcePlanner = (AllocationPolicy) createInstance(
						ConfigurationConstants.RESOURCE_PLANNER_CLASS, properties);
			} else {
				resourcePlanner = new LinearAllocationPolicy(properties);
			}
			
			Map<String, String> resourceProperties = getPropertiesByPrefix(properties,
					ConfigurationConstants.RESOURCE_PROPERTIES_PREFIX);
			
			if (resourceProperties.keySet().contains(ResourcePropertiesConstants.PUBLICKEY_KEY)) {
				String publicKey = getFileContent(resourceProperties.get(ResourcePropertiesConstants.PUBLICKEY_KEY));
				resourceProperties.put(ResourcePropertiesConstants.PUBLICKEY_KEY, publicKey);			
			}
			
			/*
			 * Create infrastructureManager and Monitor Create
			 * defaultInstanceProperties (permanent requests)
			 * 
			 * 
			 * Choose the ResourcePlanner Create the queue
			 * 
			 * Create capacityPlanner
			 */
			
			CapacityPlanner capacityPlanner = new CapacityPlanner(resourceProperties,
					Integer.parseInt(properties.getProperty(ConfigurationConstants.CAPACITY_EXECUTION_INTERVAL)));
			
			capacityPlanner.setInfrastructure(infrastructure);
			capacityPlanner.setQueue(queue);
			capacityPlanner.setResourcePlanner(resourcePlanner);
			
			capacityPlanner.initialize();		
			
			while (true) {
				LOGGER.info("Capacity Planner is running...");
				LOGGER.info("Current resourcesInUse=" + capacityPlanner.getResourcesInUse());
				LOGGER.info("Current resourcesNotAvailable=" + capacityPlanner.getResourcesNotAvailable());
				waitExecutionInterval(properties);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(0);
		} catch (CapacityPlannerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void triggerInfraConfigurationUpdater(
			final InfrastructureManager infrastructure, final String infraEndpoint,
			final Map<String, String> infraCredentials, int configurationUpdaterInterval) {

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					infrastructure.configure(infraCredentials);
				} catch (InfrastructureException e) {
					LOGGER.error("Exception while configuring the infrastructure. endpoint="
							+ infraEndpoint + ", credentials=" + infraCredentials, e);
				}
			}
		}, configurationUpdaterInterval, configurationUpdaterInterval, TimeUnit.MINUTES);
	}

	private static void waitExecutionInterval(Properties properties) {
		try {
			Thread.sleep(Long.parseLong(properties.getProperty(ConfigurationConstants.CAPACITY_EXECUTION_INTERVAL)) * 1000 * 60);
		} catch (Exception e) {
			LOGGER.error("Exception while sleeping capacity execution interval.", e);
		}
	}

	private static Map<String, String> getPropertiesByPrefix(Properties properties, String prefix) {
		LOGGER.debug("Prefix: " + prefix);
		Map<String, String> selectedProperties = new HashMap<String, String>();

		for (Object propName : properties.keySet()) {
			String propNameStr = (String) propName;
			if (propNameStr.startsWith(prefix)) {
				selectedProperties.put(propNameStr.substring(prefix.length()),
						properties.getProperty(propNameStr).trim());
			}
		}
		LOGGER.debug("Selected Properties: " + selectedProperties);
		return selectedProperties;
	}
	
	private static String getFileContent(String path) throws IOException {		
		FileReader reader = new FileReader(path);
		BufferedReader leitor = new BufferedReader(reader);
		String fileContent = "";
		String linha = "";
		while (true) {
			linha = leitor.readLine();
			if (linha == null)
				break;
			fileContent += linha + "\n";
		}
		return fileContent.trim();
	}

	private static Object createInstance(String propName, Properties properties) throws Exception {
		return Class.forName(properties.getProperty(propName)).getConstructor(Properties.class)
				.newInstance(properties);
	}
}
