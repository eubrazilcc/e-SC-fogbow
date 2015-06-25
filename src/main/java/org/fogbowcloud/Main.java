package org.fogbowcloud;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
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
import org.fogbowcloud.infrastructure.core.InfrastructureProvider;
import org.fogbowcloud.infrastructure.core.ResourcePropertiesConstants;
import org.fogbowcloud.infrastructure.fogbow.FogbowInfrastructureProvider;

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
	
			// creating infrastructure provider
			InfrastructureProvider infrastructureProvider;
			String infraClass = properties.getProperty(ConfigurationConstants.INFRASTRUCTURE_CLASS);
			if (infraClass != null) {
				infrastructureProvider = (InfrastructureProvider) createInstance(
						ConfigurationConstants.INFRASTRUCTURE_CLASS, properties);
			} else {
				infrastructureProvider = new FogbowInfrastructureProvider(properties);
			}
			
			String infraEndpoint = properties.getProperty(ConfigurationConstants.INFRA_ENDPOINT);
			Map<String, String> infraCredentials = getPropertiesByPrefix(properties,
					ConfigurationConstants.INFRA_CREDENTIALS_PREFIX);
			
			infrastructureProvider.configure(new HashMap<String, String>(infraCredentials));
			
			//TODO updating configure!
			String infraConfUpdateIntervalStr = properties.getProperty(ConfigurationConstants.INFRA_CONF_UPDATE_INTERVAL);	
			int infraConfUpdateInterval;
			try {
				infraConfUpdateInterval = Integer.parseInt(infraConfUpdateIntervalStr);
			} catch (Exception e) {
				infraConfUpdateInterval = DEFAULT_INFRA_CONF_UPDATE_INTERVAL;
			}
			triggerInfraConfigurationUpdater(infrastructureProvider, infraEndpoint, infraCredentials,
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
			
			// creating resource allocation policy 
			AllocationPolicy allocationPolicy;
			String allocationPolicyClass = properties.getProperty(ConfigurationConstants.ALLOCATION_POLICY_CLASS);
			if (allocationPolicyClass != null) {
				allocationPolicy = (AllocationPolicy) createInstance(
						ConfigurationConstants.ALLOCATION_POLICY_CLASS, properties);
			} else {
				allocationPolicy = new LinearAllocationPolicy(properties);
			}
			
			Map<String, String> resourceProperties = getPropertiesByPrefix(properties,
					ConfigurationConstants.RESOURCE_PROPERTIES_PREFIX);
			
			if (resourceProperties.keySet().contains(ResourcePropertiesConstants.PUBLICKEY_KEY)) {
				String publicKey = getFileContent(resourceProperties.get(ResourcePropertiesConstants.PUBLICKEY_KEY));
				resourceProperties.put(ResourcePropertiesConstants.PUBLICKEY_KEY, publicKey);			
			}
			
			Map<String, String> resourceCredentials = getPropertiesByPrefix(properties,
					ConfigurationConstants.RESOURCE_CREDENTIALS_PREFIX);

			// creating infrastructure manager
			InfrastructureManager infraManager;
			if (properties.getProperty(ConfigurationConstants.MONITORING_INTERVAL) != null){
				int monitoring_interval = Integer.parseInt(properties.getProperty(ConfigurationConstants.MONITORING_INTERVAL));
				infraManager = new InfrastructureManager(resourceProperties, resourceCredentials, monitoring_interval);
			} else {
				infraManager = new InfrastructureManager(resourceProperties, resourceCredentials);
			}
			
			infraManager.setInfraProvider(infrastructureProvider);



			//UNCOMMENT THIS PART TO RUN CAPACITY PLANNER
			// creating capacity planner
			/*CapacityPlanner capacityPlanner;
			if (properties.getProperty(ConfigurationConstants.CAPACITY_EXECUTION_INTERVAL) != null){
				int capacityInterval = Integer.parseInt(properties.getProperty(ConfigurationConstants.CAPACITY_EXECUTION_INTERVAL));
				capacityPlanner = new CapacityPlanner(capacityInterval);				
			} else {
				capacityPlanner = new CapacityPlanner();
			}		
			
			capacityPlanner.setInfraManager(infraManager);
			capacityPlanner.setQueue(queue);
			capacityPlanner.setAllocationPolicy(allocationPolicy);
			
			capacityPlanner.initialize();		
			
			while (true) {
				LOGGER.info("Capacity Planner is running...");
				LOGGER.info("Current resourcesInUse=" + infraManager.getResourcesInUse());
				LOGGER.info("Current resourcesNotAvailable=" + infraManager.getResourcesNotAvailable());
				waitExecutionInterval(properties);
			}*/

			//COMMENT THIS PART WHEN RUNNING THE ENTIRE CODE AND NOT JUST TESTING AS IF TO CREATE THE VM
			System.out.println("-----------------CREATING A NEW VM------------------------");
			List<String> results =  infrastructureProvider.createResource(1, resourceProperties);
			System.out.println("GETTING RESOURCE ID");
			System.out.println(results.get(0));
			System.out.println("GETTING RESOURCE INFO");
			System.out.println(infrastructureProvider.getResourceInfo(results.get(0)));


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
			final InfrastructureProvider infrastructure, final String infraEndpoint,
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
