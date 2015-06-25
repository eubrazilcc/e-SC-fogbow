package org.fogbowcloud.infrastructure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fogbowcloud.ConfigurationConstants;
import org.fogbowcloud.infrastructure.core.InfrastructureException;
import org.fogbowcloud.infrastructure.core.InfrastructureProvider;
import org.fogbowcloud.infrastructure.core.ResourcePropertiesConstants;
import org.fogbowcloud.infrastructure.fogbow.FogbowContants;
import org.fogbowcloud.infrastructure.fogbow.FogbowInfrastructureProvider;
import org.fogbowcloud.manager.occi.request.RequestConstants;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public class Main {

	public static final int DEFAULT_INTANCE_COUNT = 1;
	protected static final String DEFAULT_TYPE = RequestConstants.DEFAULT_TYPE;
	protected static final String DEFAULT_FLAVOR = RequestConstants.SMALL_TERM;
	protected static final String DEFAULT_IMAGE = "fogbow-linux-x86";

	public static void main(String[] args) {
		configureLog4j();

		JCommander jc = new JCommander();

		// TODO Allow user to specify the infrastructure that will be used (not only fogbow)
		InfrastructureProvider infrastructure;
		Properties properties = new Properties();

		CreateResourceCommand createResource = new CreateResourceCommand();
		jc.addCommand("create", createResource);
		GetResourceInfoCommand getResourceInfo = new GetResourceInfoCommand();
		jc.addCommand("get-resource-info", getResourceInfo);		
		DeleteResourceCommand deleteResource = new DeleteResourceCommand();
		jc.addCommand("delete", deleteResource);

		jc.setProgramName("infrastructure-cli");
		try {
			jc.parse(args);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			jc.usage();
			return;
		}

		String parsedCommand = jc.getParsedCommand();

		if (parsedCommand == null) {
			jc.usage();
			return;
		}

		if (parsedCommand.equals("create")) {
			try {
				properties.put(ConfigurationConstants.INFRA_ENDPOINT, createResource.endpoint);
				infrastructure = new FogbowInfrastructureProvider(properties);
				infrastructure.configure(createResource.credentials);
				
				Map<String, String> resourcesProperties =createResource.resourceProperties; 
				resourcesProperties.put(ResourcePropertiesConstants.IMAGE_KEY, createResource.image);
				resourcesProperties.put(ResourcePropertiesConstants.FLAVOR_KEY, createResource.flavor);
				resourcesProperties.put(FogbowContants.TYPE_KEY, createResource.type);
				if (createResource.publicKey != null){
					createResource.publicKey = getFileContent(createResource.publicKey);
					resourcesProperties.put(ResourcePropertiesConstants.PUBLICKEY_KEY, createResource.publicKey);	
				}
				
				List<String> ids = infrastructure.createResource(createResource.instanceCount,
						resourcesProperties);
				for (String currentId : ids) {
					System.out.println(currentId);
				}
			} catch (InfrastructureException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		} else if (parsedCommand.equals("get-resource-info")) {
			try {
				properties.put(ConfigurationConstants.INFRA_ENDPOINT, getResourceInfo.endpoint);
				infrastructure = new FogbowInfrastructureProvider(properties);
				infrastructure.configure(getResourceInfo.credentials);
				System.out.println(infrastructure.getResourceInfo(getResourceInfo.resourceId));
			} catch (InfrastructureException e) {			
				System.out.println(e.getMessage());	
			}
		} else if (parsedCommand.equals("delete")){
			try {
				properties.put(ConfigurationConstants.INFRA_ENDPOINT, deleteResource.endpoint);
				infrastructure = new FogbowInfrastructureProvider(properties);
				infrastructure.configure(deleteResource.credentials);
				infrastructure.deleteResource(deleteResource.resourceId);
				System.out.println("OK");
			} catch (InfrastructureException e) {
				System.out.println(e.getMessage());	
			}
		}
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

	private static void configureLog4j() {
		ConsoleAppender console = new ConsoleAppender();
		console.setThreshold(Level.OFF);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
	}

	@Parameters(separators = "=", commandDescription = "Configure infrastructure")
	private static class ConfigureCommand {
		@Parameter(names = "--endpoint", description = "Infrastructure endpoint", required = true)
		String endpoint = null;
		
		@DynamicParameter(names = "-D", description = "Dynamic user credentials parameters")
		Map<String, String> credentials = new HashMap<String, String>();
	}
	
	
	@Parameters(separators = "=", commandDescription = "Create Resource operation")
	private static class CreateResourceCommand extends ConfigureCommand {
		@Parameter(names = "--n", description = "Number of instances required")
		int instanceCount = Main.DEFAULT_INTANCE_COUNT;

		@Parameter(names = "--image", description = "Instance image")
		String image = Main.DEFAULT_IMAGE;

		@Parameter(names = "--flavor", description = "Instance flavor")
		String flavor = Main.DEFAULT_FLAVOR;

		@Parameter(names = "--type", description = "Resource type (Default is one-time)")
		String type = Main.DEFAULT_TYPE;
		
		@Parameter(names = "--public-key", description = "Public key")
		String publicKey = null;	
		
		@DynamicParameter(names = "-P", description = "Dynamic resource properties parameters (if applicable)")
		Map<String, String> resourceProperties = new HashMap<String, String>();
	}
	
	@Parameters(separators = "=", commandDescription = "Get Resource Info operation")
	private static class GetResourceInfoCommand extends ConfigureCommand {		
		@Parameter(names = "--id", description = "Resource id", required = true)
		String resourceId = null;
	}
	
	@Parameters(separators = "=", commandDescription = "Delete Resource operation")
	private static class DeleteResourceCommand extends ConfigureCommand {
		@Parameter(names = "--id", description = "Resource id", required = true)
		String resourceId = null;		
	}
}
