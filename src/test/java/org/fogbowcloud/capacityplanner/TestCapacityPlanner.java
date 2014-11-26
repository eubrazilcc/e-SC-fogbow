package org.fogbowcloud.capacityplanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.capacityplanner.queue.EScienceCentralQueue;
import org.fogbowcloud.capacityplanner.resource.AllocationPolicy;
import org.fogbowcloud.infrastructure.core.InfrastructureException;
import org.fogbowcloud.infrastructure.core.InfrastructureManager;
import org.fogbowcloud.infrastructure.core.ResourcePropertiesConstants;
import org.fogbowcloud.infrastructure.fogbow.FogbowApplication;
import org.fogbowcloud.infrastructure.fogbow.FogbowContants;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestCapacityPlanner {

	private static final String THIRD_ID = "third";
	private static final String SECOND_ID = "second";
	private static final String FIRST_ID = "first";
	private Map<String, String> defaultProperties;

	@Before
	public void settup() {
		defaultProperties = new HashMap<String, String>();
		defaultProperties.put(FogbowContants.TYPE_KEY, RequestType.ONE_TIME.getValue());
		defaultProperties.put(ResourcePropertiesConstants.FLAVOR_KEY, RequestConstants.SMALL_TERM);
		defaultProperties.put(ResourcePropertiesConstants.IMAGE_KEY, FogbowApplication.IMAGE1_NAME);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeExecPeriod() {
		new CapacityPlanner(defaultProperties, -2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testZeroExecPeriod() {
		new CapacityPlanner(defaultProperties, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullInstanceProperties() {
		new CapacityPlanner(null, 20);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyInstanceProperties() {
		new CapacityPlanner(new HashMap<String, String>(), 20);
	}

	@Test(expected = CapacityPlannerException.class)
	public void testInitialize() throws CapacityPlannerException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);
		capacity.initialize();
	}

	@Test
	public void testValidInitialize() throws CapacityPlannerException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		
		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);

		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);
		capacity.initialize();

		Assert.assertTrue(capacity.isInitialized());
	}

	@Test
	public void testExeAllocateAResource() throws CapacityPlannerException, InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> result = new ArrayList<String>();
		result.add(FIRST_ID);
		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.createResource(1, defaultProperties)).thenReturn(result);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(1);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(0, 0, 1)).thenReturn(1);

		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there is not resources
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there is one resource
		Assert.assertEquals(1, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());
	}

	@Test
	public void testExeAllocateTwoResources() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> result = new ArrayList<String>();
		result.add(FIRST_ID);
		result.add(SECOND_ID);
		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.createResource(2, defaultProperties)).thenReturn(result);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(2);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(0, 0, 2)).thenReturn(2);

		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there is not resources
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there are 2 resources
		Assert.assertEquals(2, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());
	}

	@Test
	public void testExeAllocateTenResources() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

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
		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.createResource(10, defaultProperties)).thenReturn(result);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(10);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(0, 0, 10)).thenReturn(10);

		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there is not resources
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there are 10 resources
		Assert.assertEquals(10, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());
	}

	@Test
	public void testAllocateResourceFails() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.doThrow(new InfrastructureException("Any exception on creating resource"))
				.when(infrastructure).createResource(2, defaultProperties);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(2);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(0, 0, 2)).thenReturn(2);

		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are not resources
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there are not resources because infrastructure throws
		// exception
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());
	}

	@Test
	public void testDeallocateResourceFails() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourceInUse = new ArrayList<String>();
		resourceInUse.add(FIRST_ID);
		resourceInUse.add(SECOND_ID);
		
		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.doThrow(new InfrastructureException("Any exception on deallocating resource"))
				.when(infrastructure).deleteResource(FIRST_ID);
		Mockito.doThrow(new InfrastructureException("Any exception on deallocating resource"))
				.when(infrastructure).deleteResource(SECOND_ID);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(true);
		
		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(0);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(2, 0, 0)).thenReturn(-2);

		capacity.setResourcesInUse(resourceInUse);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are resources
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(2, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there are resources because infrastructure throws
		// exception
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(2, capacity.getResourcesInUse().size());
	}

	@Test
	public void testExeQueueDellocateOneResource() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.doNothing().when(infrastructure).deleteResource(FIRST_ID);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(0);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(0, 1, 0)).thenReturn(-1);

		capacity.setResourcesNotAvailable(resourcesNotAvailable);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there is one resource
		Assert.assertEquals(1, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there is not resource
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());
	}

	@Test
	public void testExeQueueDellocateTwoResources() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);
		resourcesNotAvailable.add(SECOND_ID);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.doNothing().when(infrastructure).deleteResource(FIRST_ID);
		Mockito.doNothing().when(infrastructure).deleteResource(SECOND_ID);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(0);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(0, 2, 0)).thenReturn(-2);

		capacity.setResourcesNotAvailable(resourcesNotAvailable);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are two resource
		Assert.assertEquals(2, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there is not resource
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());
	}

	@Test
	public void testExeQueueDellocateFiveResources() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

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

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.doNothing().when(infrastructure).deleteResource(Mockito.anyString());
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable("fourth")).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable("fifth")).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable("sixth")).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable("seventh")).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable("eighth")).thenReturn(true);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		Mockito.when(queue.getLength()).thenReturn(3);

		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);
		Mockito.when(resourcePlanner.calculateResourceNeeds(5, 3, 3)).thenReturn(-5);

		capacity.setResourcesNotAvailable(resourcesNotAvailable);
		capacity.setResourcesInUse(resourcesInUse);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are two resource
		Assert.assertEquals(3, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(5, capacity.getResourcesInUse().size());

		capacity.runCapacityPanner();

		// checking there is not resource
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(3, capacity.getResourcesInUse().size());
	}

	@Test
	public void testANotAvailableResourcePassToAvailable() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);
		resourcesNotAvailable.add(SECOND_ID);
		resourcesNotAvailable.add(THIRD_ID);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(false);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);

		capacity.setResourcesNotAvailable(resourcesNotAvailable);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are 3 resources not available yet
		Assert.assertEquals(3, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(2, capacity.getResourcesNotAvailable().size());
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(SECOND_ID));
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(THIRD_ID));
		Assert.assertEquals(1, capacity.getResourcesInUse().size());
		Assert.assertTrue(capacity.getResourcesInUse().contains(FIRST_ID));
	}

	@Test
	public void testAllNotAvailableResourcesPassToAvailable() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(FIRST_ID);
		resourcesNotAvailable.add(SECOND_ID);
		resourcesNotAvailable.add(THIRD_ID);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(true);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);

		capacity.setResourcesNotAvailable(resourcesNotAvailable);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are 3 resources not available yet
		Assert.assertEquals(3, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(0, capacity.getResourcesInUse().size());

		capacity.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(3, capacity.getResourcesInUse().size());
		Assert.assertTrue(capacity.getResourcesInUse().contains(FIRST_ID));
		Assert.assertTrue(capacity.getResourcesInUse().contains(SECOND_ID));
		Assert.assertTrue(capacity.getResourcesInUse().contains(THIRD_ID));
	}

	@Test
	public void testAAvailableResourcePassToNotAvailable() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourcesInUse = new ArrayList<String>();
		resourcesInUse.add(FIRST_ID);
		resourcesInUse.add(SECOND_ID);
		resourcesInUse.add(THIRD_ID);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(true);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(false);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);

		capacity.setResourcesInUse(resourcesInUse);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are 3 available resources
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(3, capacity.getResourcesInUse().size());

		capacity.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(1, capacity.getResourcesNotAvailable().size());
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(THIRD_ID));
		Assert.assertEquals(2, capacity.getResourcesInUse().size());
		Assert.assertTrue(capacity.getResourcesInUse().contains(FIRST_ID));
		Assert.assertTrue(capacity.getResourcesInUse().contains(SECOND_ID));
	}

	@Test
	public void testAllAvailableResourcesPassToNotAvailable() throws CapacityPlannerException,
			InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourcesInUse = new ArrayList<String>();
		resourcesInUse.add(FIRST_ID);
		resourcesInUse.add(SECOND_ID);
		resourcesInUse.add(THIRD_ID);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(false);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);

		capacity.setResourcesInUse(resourcesInUse);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are 3 available resources
		Assert.assertEquals(0, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(3, capacity.getResourcesInUse().size());

		capacity.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(3, capacity.getResourcesNotAvailable().size());
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(THIRD_ID));
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(FIRST_ID));
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(SECOND_ID));
		Assert.assertEquals(0, capacity.getResourcesInUse().size());
	}

	@Test
	public void testAvailableResourcesPassToNotAvailableAndViceVersa()
			throws CapacityPlannerException, InfrastructureException {
		CapacityPlanner capacity = new CapacityPlanner(defaultProperties);

		// mocking
		List<String> resourcesInUse = new ArrayList<String>();
		resourcesInUse.add(FIRST_ID);
		resourcesInUse.add(SECOND_ID);

		List<String> resourcesNotAvailable = new ArrayList<String>();
		resourcesNotAvailable.add(THIRD_ID);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);
		Mockito.when(infrastructure.isResourceAvailable(FIRST_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(SECOND_ID)).thenReturn(false);
		Mockito.when(infrastructure.isResourceAvailable(THIRD_ID)).thenReturn(true);

		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);

		capacity.setResourcesInUse(resourcesInUse);
		capacity.setResourcesNotAvailable(resourcesNotAvailable);
		capacity.setInfrastructure(infrastructure);
		capacity.setQueue(queue);
		capacity.setResourcePlanner(resourcePlanner);

		// checking there are 2 available resources and 1 not available
		Assert.assertEquals(1, capacity.getResourcesNotAvailable().size());
		Assert.assertEquals(2, capacity.getResourcesInUse().size());

		capacity.monitoringResources();

		// checking there resource lists were updated according resource status
		Assert.assertEquals(2, capacity.getResourcesNotAvailable().size());
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(FIRST_ID));
		Assert.assertTrue(capacity.getResourcesNotAvailable().contains(SECOND_ID));
		Assert.assertEquals(1, capacity.getResourcesInUse().size());
		Assert.assertTrue(capacity.getResourcesInUse().contains(THIRD_ID));
	}
}
