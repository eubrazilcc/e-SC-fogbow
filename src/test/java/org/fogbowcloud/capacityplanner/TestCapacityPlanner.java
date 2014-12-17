package org.fogbowcloud.capacityplanner;

import org.fogbowcloud.capacityplanner.queue.EScienceCentralQueue;
import org.fogbowcloud.capacityplanner.resource.AllocationPolicy;
import org.fogbowcloud.infrastructure.core.InfrastructureManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestCapacityPlanner {

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeExecPeriod() {
		new CapacityPlanner(-2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testZeroExecPeriod() {
		new CapacityPlanner(0);
	}
	
	@Test(expected = CapacityPlannerException.class)
	public void testInitialize() throws CapacityPlannerException {
		CapacityPlanner capacity = new CapacityPlanner(CapacityPlanner.DEFAULT_EXECUTION_PERIOD);
		capacity.initialize();
	}

	@Test
	public void testValidInitialize() throws CapacityPlannerException {
		CapacityPlanner capacity = new CapacityPlanner(CapacityPlanner.DEFAULT_EXECUTION_PERIOD);

		InfrastructureManager infrastructure = Mockito.mock(InfrastructureManager.class);	
		EScienceCentralQueue queue = Mockito.mock(EScienceCentralQueue.class);
		AllocationPolicy resourcePlanner = Mockito.mock(AllocationPolicy.class);

		capacity.setInfraManager(infrastructure);
		capacity.setQueue(queue);
		capacity.setAllocationPolicy(resourcePlanner);
		capacity.initialize();

		Assert.assertTrue(capacity.isInitialized());
	}
}
