package org.fogbowcloud.infrastructure.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.capacityplanner.CapacityPlannerException;
import org.fogbowcloud.infrastructure.fogbow.FogbowApplication;
import org.fogbowcloud.infrastructure.fogbow.FogbowContants;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestInfrastructureManager {

	private static final CommandResult DEFAULT_COMMAND_RESULT = new CommandResult(0, "OK", "");
	private static final String THIRD_ID = "third";
	private static final String SECOND_ID = "second";
	private static final String FIRST_ID = "first";
	
	private Map<String, String> defaultResourceProperties;	
	private Map<String, String> defaultResourceCredentials;
	
	@Before
	public void setUp(){
		defaultResourceProperties = new HashMap<String, String>();
		defaultResourceProperties.put(FogbowContants.TYPE_KEY, RequestType.ONE_TIME.getValue());
		defaultResourceProperties.put(ResourcePropertiesConstants.FLAVOR_KEY, RequestConstants.SMALL_TERM);
		defaultResourceProperties.put(ResourcePropertiesConstants.IMAGE_KEY, FogbowApplication.IMAGE1_NAME);
		
		defaultResourceCredentials = new HashMap<String, String>();
		defaultResourceCredentials.put(ResourcePropertiesConstants.USERNAME_KEY, "user");
		defaultResourceCredentials.put(ResourcePropertiesConstants.USER_PASSWORD_KEY, "password");
	}

	@Test
	public void testExeAllocateAResource() throws CapacityPlannerException, InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> result = new ArrayList<String>();
		result.add(FIRST_ID);
		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.createResource(1, defaultResourceProperties)).thenReturn(result);

		infrastructureManager.setInfraProvider(infrastructure);

		// checking there is not resources
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(1);

		// checking there is one resource
		Assert.assertEquals(1, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testExeAllocateTwoResources() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> result = new ArrayList<String>();
		result.add(FIRST_ID);
		result.add(SECOND_ID);
		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.createResource(2, defaultResourceProperties)).thenReturn(result);

		infrastructureManager.setInfraProvider(infrastructure);

		// checking there is not resources
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(2);

		// checking there are 2 resource
		Assert.assertEquals(2, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testExeAllocateTenResources() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> result = new ArrayList<String>();
		result.add(FIRST_ID);
		result.add(SECOND_ID);
		result.add(THIRD_ID);
		result.add("fourth");
		result.add("fifth");
		result.add("sixth");
		result.add("seventh");
		result.add("eighth");
		result.add("ninth");
		result.add("tenth");
		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.createResource(10, defaultResourceProperties)).thenReturn(result);

		infrastructureManager.setInfraProvider(infrastructure);

		// checking there is not resources
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(10);

		// checking there are 10 resources
		Assert.assertEquals(10, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testAllocateResourceFails() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.doThrow(new InfrastructureException("Any exception on creating resource"))
				.when(infrastructure).createResource(2, defaultResourceProperties);

		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are not resources
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(2);

		// checking there are not resources because infrastructure throws
		// exception
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());		
	}
	
	@Test
	public void testDeallocateResourceFails() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourceInUse = new ArrayList<String>();
		resourceInUse.add(FIRST_ID);
		resourceInUse.add(SECOND_ID);
		
		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.doThrow(new InfrastructureException("Any exception on deallocating resource"))
				.when(infrastructure).deleteResource(FIRST_ID);
		Mockito.doThrow(new InfrastructureException("Any exception on deallocating resource"))
				.when(infrastructure).deleteResource(SECOND_ID);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(true);
		
		infrastructureManager.setResourcesInUse(resourceInUse);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are resources
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(2, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(0);

		// checking there are resources because infrastructure throws
		// exception
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(2, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testExeQueueDellocateOneResource() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.doNothing().when(infrastructure).deleteResource(FIRST_ID);

		infrastructureManager.setResourcesNotAvailable(resourcesNotAvailable);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there is one resource
		Assert.assertEquals(1, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(0);

		// checking there is not resource
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testExeQueueDellocateTwoResources() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);
		resourcesNotAvailable.add(SECOND_ID);

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.doNothing().when(infrastructure).deleteResource(FIRST_ID);
		Mockito.doNothing().when(infrastructure).deleteResource(SECOND_ID);

		infrastructureManager.setResourcesNotAvailable(resourcesNotAvailable);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are two resource
		Assert.assertEquals(2, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(0);

		// checking there is not resource
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testExeQueueDellocateFiveResources() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);
		resourcesNotAvailable.add(SECOND_ID);
		resourcesNotAvailable.add(THIRD_ID);

		List<String> resourcesInUse = new ArrayList<String>();
		resourcesInUse.add("fourth");
		resourcesInUse.add("fifth");
		resourcesInUse.add("sixth");
		resourcesInUse.add("seventh");
		resourcesInUse.add("eighth");

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.doNothing().when(infrastructure).deleteResource(Mockito.anyString());

		infrastructureManager.setResourcesInUse(resourcesInUse);
		infrastructureManager.setResourcesNotAvailable(resourcesNotAvailable);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are resources
		Assert.assertEquals(3, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(5, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.updateCurrentNeeds(3);

		// checking there is not resource
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(3, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testANotAvailableResourcePassToAvailable() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);
		resourcesNotAvailable.add(SECOND_ID);
		resourcesNotAvailable.add(THIRD_ID);

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(false);
		Mockito.when(
				infrastructure.executeCommand(Mockito.anyMap(), Mockito.anyMap(),
						Mockito.anyString())).thenReturn(DEFAULT_COMMAND_RESULT);

		infrastructureManager.setResourcesNotAvailable(resourcesNotAvailable);
		infrastructureManager.setInfraProvider(infrastructure);
		
		// checking there are 3 resources not available yet
		Assert.assertEquals(3, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(2, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(SECOND_ID));
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(THIRD_ID));
		Assert.assertEquals(1, infrastructureManager.getResourcesInUse().size());
		Assert.assertTrue(infrastructureManager.getResourcesInUse().contains(FIRST_ID));
	}
	
	@Test
	public void testAllNotAvailableResourcesPassToAvailable() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);
		resourcesNotAvailable.add(SECOND_ID);
		resourcesNotAvailable.add(THIRD_ID);

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(true);
		Mockito.when(
				infrastructure.executeCommand(Mockito.anyMap(), Mockito.anyMap(),
						Mockito.anyString())).thenReturn(DEFAULT_COMMAND_RESULT);

		infrastructureManager.setResourcesNotAvailable(resourcesNotAvailable);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are 3 resources not available yet
		Assert.assertEquals(3, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(3, infrastructureManager.getResourcesInUse().size());
		Assert.assertTrue(infrastructureManager.getResourcesInUse().contains(FIRST_ID));
		Assert.assertTrue(infrastructureManager.getResourcesInUse().contains(SECOND_ID));
		Assert.assertTrue(infrastructureManager.getResourcesInUse().contains(THIRD_ID));
	}

	@Test
	public void testAAvailableResourcePassToNotAvailable() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesInUse = new ArrayList<String>();
		resourcesInUse.add(FIRST_ID);
		resourcesInUse.add(SECOND_ID);
		resourcesInUse.add(THIRD_ID);

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(false);
		Mockito.when(
				infrastructure.executeCommand(Mockito.anyMap(), Mockito.anyMap(),
						Mockito.anyString())).thenReturn(DEFAULT_COMMAND_RESULT);

		infrastructureManager.setResourcesInUse(resourcesInUse);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are 3 available resources
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(3, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(1, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(THIRD_ID));
		Assert.assertEquals(2, infrastructureManager.getResourcesInUse().size());
		Assert.assertTrue(infrastructureManager.getResourcesInUse().contains(FIRST_ID));
		Assert.assertTrue(infrastructureManager.getResourcesInUse().contains(SECOND_ID));
	}
	
	@Test
	public void testAllAvailableResourcesPassToNotAvailable() throws CapacityPlannerException,
			InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesInUse = new ArrayList<String>();
		resourcesInUse.add(FIRST_ID);
		resourcesInUse.add(SECOND_ID);
		resourcesInUse.add(THIRD_ID);

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(false);
		Mockito.when(
				infrastructure.executeCommand(Mockito.anyMap(), Mockito.anyMap(),
						Mockito.anyString())).thenReturn(DEFAULT_COMMAND_RESULT);

		infrastructureManager.setResourcesInUse(resourcesInUse);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are 3 available resources
		Assert.assertEquals(0, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(3, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(3, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(THIRD_ID));
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(FIRST_ID));
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(SECOND_ID));
		Assert.assertEquals(0, infrastructureManager.getResourcesInUse().size());
	}
	
	@Test
	public void testAvailableResourcesPassToNotAvailableAndViceVersa()
			throws CapacityPlannerException, InfrastructureException {
		InfrastructureManager infrastructureManager = new InfrastructureManager(defaultResourceProperties, defaultResourceCredentials);

		// mocking
		List<String> resourcesInUse = new ArrayList<String>();
		resourcesInUse.add(FIRST_ID);
		resourcesInUse.add(SECOND_ID);

		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(THIRD_ID);

		InfrastructureProvider infrastructure = Mockito.mock(InfrastructureProvider.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(true);
		Mockito.when(
				infrastructure.executeCommand(Mockito.anyMap(), Mockito.anyMap(),
						Mockito.anyString())).thenReturn(DEFAULT_COMMAND_RESULT);

		infrastructureManager.setResourcesInUse(resourcesInUse);
		infrastructureManager.setResourcesNotAvailable(resourcesNotAvailable);
		infrastructureManager.setInfraProvider(infrastructure);

		// checking there are 2 available resources and 1 not available
		Assert.assertEquals(1, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertEquals(2, infrastructureManager.getResourcesInUse().size());

		infrastructureManager.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(2, infrastructureManager.getResourcesNotAvailable().size());
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(FIRST_ID));
		Assert.assertTrue(infrastructureManager.getResourcesNotAvailable().contains(SECOND_ID));
		Assert.assertEquals(1, infrastructureManager.getResourcesInUse().size());
		Assert.assertTrue(infrastructureManager.getResourcesInUse().contains(THIRD_ID));
	}
}
