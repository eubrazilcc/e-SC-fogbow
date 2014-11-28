package org.fogbowcloud.capacityplanner.resource;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class TestLinearAllocationPolicy {

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeJobsPerMachine() {
		new LinearAllocationPolicy(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testZeroJobsPerMachine() {
		new LinearAllocationPolicy(0);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNegativeJobsPerMachineByProperties() {
		Properties properties = new Properties();
		properties.put(LinearAllocationPolicy.JOBS_PER_RESOURCE_KEY, "-2");
		new LinearAllocationPolicy(properties);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testZeroJobsPerMachineByProperties() {
		Properties properties = new Properties();
		properties.put(LinearAllocationPolicy.JOBS_PER_RESOURCE_KEY, "0");
		new LinearAllocationPolicy(properties);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testStringJobsPerMachineByProperties() {
		Properties properties = new Properties();
		properties.put(LinearAllocationPolicy.JOBS_PER_RESOURCE_KEY, "two");
		new LinearAllocationPolicy(properties);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEmptyJobsPerMachineByProperties() {
		Properties properties = new Properties();
		properties.put(LinearAllocationPolicy.JOBS_PER_RESOURCE_KEY, "");
		new LinearAllocationPolicy(properties);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNoPropertyJobsPerMachineByProperties() {
		Properties properties = new Properties();
		new LinearAllocationPolicy(properties);
	}

	@Test
	public void testCalculateToReturnAllocateNeeds() {
		LinearAllocationPolicy planner = new LinearAllocationPolicy(2);
		// queue length is 1
		Assert.assertEquals(1, planner.calculateResourceNeeds(0, 0, 1));
		Assert.assertEquals(0, planner.calculateResourceNeeds(1, 0, 1));
		Assert.assertEquals(0, planner.calculateResourceNeeds(0, 1, 1));
		// queue length is 2
		Assert.assertEquals(1, planner.calculateResourceNeeds(0, 0, 2));
		Assert.assertEquals(0, planner.calculateResourceNeeds(1, 0, 2));
		Assert.assertEquals(0, planner.calculateResourceNeeds(0, 1, 2));
		// queue length is 10
		Assert.assertEquals(5, planner.calculateResourceNeeds(0, 0, 10));
		Assert.assertEquals(4, planner.calculateResourceNeeds(1, 0, 10));
		Assert.assertEquals(4, planner.calculateResourceNeeds(0, 1, 10));
		Assert.assertEquals(1, planner.calculateResourceNeeds(1, 3, 10));
		Assert.assertEquals(1, planner.calculateResourceNeeds(3, 1, 10));
		Assert.assertEquals(0, planner.calculateResourceNeeds(5, 0, 10));
		Assert.assertEquals(0, planner.calculateResourceNeeds(0, 5, 10));
		Assert.assertEquals(0, planner.calculateResourceNeeds(2, 3, 10));
		Assert.assertEquals(0, planner.calculateResourceNeeds(3, 2, 10));
	}

	@Test
	public void testCalculateToReturnDeallocateNeeds() {
		LinearAllocationPolicy planner = new LinearAllocationPolicy(2);
		// queue length is 0
		Assert.assertEquals(0, planner.calculateResourceNeeds(0, 0, 0));
		Assert.assertEquals(-1, planner.calculateResourceNeeds(1, 0, 0));
		Assert.assertEquals(-1, planner.calculateResourceNeeds(0, 1, 0));
		// queue length is 2
		Assert.assertEquals(-2, planner.calculateResourceNeeds(4, 0, 2));
		Assert.assertEquals(-3, planner.calculateResourceNeeds(0, 4, 2));
		Assert.assertEquals(0, planner.calculateResourceNeeds(0, 1, 2));
		// queue length is 10
		Assert.assertEquals(0, planner.calculateResourceNeeds(10, 0, 10));
		Assert.assertEquals(-5, planner.calculateResourceNeeds(0, 10, 10));
		Assert.assertEquals(-45, planner.calculateResourceNeeds(0, 50, 10));
		Assert.assertEquals(-40, planner.calculateResourceNeeds(50, 0, 10));
		Assert.assertEquals(-16, planner.calculateResourceNeeds(1, 20, 10));
		Assert.assertEquals(-11, planner.calculateResourceNeeds(20, 1, 10));
		Assert.assertEquals(-1, planner.calculateResourceNeeds(11, 0, 10));
		Assert.assertEquals(-6, planner.calculateResourceNeeds(0, 11, 10));
		Assert.assertEquals(-5, planner.calculateResourceNeeds(5, 5, 10));
	}
	
	@Test
	public void testRandomValues(){
		LinearAllocationPolicy planner = new LinearAllocationPolicy(2);
		
		Assert.assertEquals(3, planner.calculateResourceNeeds(0, 0, 5));
		Assert.assertEquals(0, planner.calculateResourceNeeds(3, 0, 4));
		Assert.assertEquals(0, planner.calculateResourceNeeds(4, 0, 5));
	}

}
