package org.fogbowcloud.infrastructure.core;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author giovanni
 *
 */
public interface InfrastructureManager {

	public String configure(Map<String, String> credentials)
			throws InfrastructureException;

	public List<String> createResource(int numberOfInstanes, Map<String, String> properties)
			throws InfrastructureException;

	public Map<String, String> getResourceInfo(String resourceId) throws InfrastructureException;

	public void deleteResource(String resourceId) throws InfrastructureException;
	
	public CommandResult executeCommand(Map<String, String> resourceInfo, Map<String, String> resourceCredentials, String command) throws InfrastructureException;
	
	public boolean isResourceAvailable(String resourceId);
}
