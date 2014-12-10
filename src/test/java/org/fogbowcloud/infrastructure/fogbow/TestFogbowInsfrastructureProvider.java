package org.fogbowcloud.infrastructure.fogbow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.ConfigurationConstants;
import org.fogbowcloud.infrastructure.core.InfrastructureException;
import org.fogbowcloud.infrastructure.core.ResourcePropertiesConstants;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestState;
import org.fogbowcloud.manager.occi.request.RequestType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFogbowInsfrastructureProvider {

	private String FIRST_ID = "first";
	private String SECOND_ID = "second";
	private String THIRD_ID = "third";
	private String FOURTH_ID = "fourth";
	private String FIFTH_ID = "fifth";

	FogbowTestHelper helper;
	FogbowInfrastructureProvider infra;
	Map<String, String> defaultProperties;

	@Before
	public void setUp() throws Exception {
		helper = new FogbowTestHelper();

		String[] expectedIds = new String[5];
		expectedIds[0] = FIRST_ID;
		expectedIds[1] = SECOND_ID;
		expectedIds[2] = THIRD_ID;
		expectedIds[3] = FOURTH_ID;
		expectedIds[4] = FIFTH_ID;

		helper.initializeFogbowComponent(expectedIds);

		defaultProperties = new HashMap<String, String>();
		defaultProperties.put(FogbowContants.TYPE_KEY, RequestType.ONE_TIME.getValue());
		defaultProperties.put(ResourcePropertiesConstants.FLAVOR_KEY, RequestConstants.SMALL_TERM);
		defaultProperties.put(ResourcePropertiesConstants.IMAGE_KEY, FogbowApplication.IMAGE1_NAME);

		// setting configuration for testing
		Properties properties = new Properties();
		properties.put(ConfigurationConstants.INFRA_ENDPOINT, helper.getFogbowEndepoint());
		infra = new FogbowInfrastructureProvider(properties);
		infra.setAuthToken(helper.getDefaultToken());
	}

	@After
	public void tearDown() throws Exception {
		helper.disconnectFogbowComponent();
	}

	@Test
	public void testCreateOneResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);

		// checking
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));
	}

	@Test
	public void testCreateTwoResources() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(02, defaultProperties);

		// checking
		Assert.assertEquals(2, requestIds.size());
		Assert.assertTrue(requestIds.contains(FIRST_ID));
		Assert.assertTrue(requestIds.contains(SECOND_ID));
	}

	@Test
	public void testCreateSomeResources() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(5, defaultProperties);

		// checking
		Assert.assertEquals(5, requestIds.size());
		Assert.assertTrue(requestIds.contains(FIRST_ID));
		Assert.assertTrue(requestIds.contains(SECOND_ID));
		Assert.assertTrue(requestIds.contains(THIRD_ID));
		Assert.assertTrue(requestIds.contains(FOURTH_ID));
		Assert.assertTrue(requestIds.contains(FIFTH_ID));
	}

	@Test(expected = InfrastructureException.class)
	public void testCreateResourceInvalidNumber() throws InfrastructureException {
		// creating resources
		infra.createResource(0, defaultProperties);
	}

	@Test(expected = InfrastructureException.class)
	public void testCreateResourceInvalidNumber2() throws InfrastructureException {
		// creating resources
		infra.createResource(-5, defaultProperties);
	}

	@Test(expected = InfrastructureException.class)
	public void testCreateResourceInvalidType() throws InfrastructureException {
		// creating resources
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(FogbowContants.TYPE_KEY, "invalid-type");
		properties.put(ResourcePropertiesConstants.FLAVOR_KEY, RequestConstants.SMALL_TERM);
		properties.put(ResourcePropertiesConstants.IMAGE_KEY, FogbowApplication.IMAGE1_NAME);

		infra.createResource(1, properties);
	}

	@Test(expected = InfrastructureException.class)
	public void testCreateResourceInvalidFlavor() throws InfrastructureException {
		// creating resources
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(FogbowContants.TYPE_KEY, RequestType.ONE_TIME.getValue());
		properties.put(ResourcePropertiesConstants.FLAVOR_KEY, "fogbow_invalid_flavor");
		properties.put(ResourcePropertiesConstants.IMAGE_KEY, FogbowApplication.IMAGE1_NAME);

		infra.createResource(1, properties);
	}

	@Test(expected = InfrastructureException.class)
	public void testCreateResourceInvalidImage() throws InfrastructureException {
		// creating resources
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(FogbowContants.TYPE_KEY, RequestType.ONE_TIME.getValue());
		properties.put(ResourcePropertiesConstants.FLAVOR_KEY, RequestConstants.SMALL_TERM);
		properties.put(ResourcePropertiesConstants.IMAGE_KEY, "fogbow_invalid-image");

		infra.createResource(1, properties);
	}

	@Test
	public void testGetResourceInfo() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// checking getting resourceInfo
		Map<String, String> resourceInfo = infra.getResourceInfo(FIRST_ID);
		Assert.assertEquals(FIRST_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		Assert.assertEquals(RequestState.OPEN.getValue(),
				resourceInfo.get(RequestAttribute.STATE.getValue()));
		Assert.assertEquals(1,
				Integer.parseInt(resourceInfo.get(RequestAttribute.INSTANCE_COUNT.getValue())));
		Assert.assertEquals("null", resourceInfo.get(RequestAttribute.INSTANCE_ID.getValue()));
		Assert.assertEquals(RequestType.ONE_TIME.getValue(),
				resourceInfo.get(RequestAttribute.TYPE.getValue()));
		Assert.assertEquals(RequestConstants.SMALL_TERM,
				resourceInfo.get(ResourcePropertiesConstants.FLAVOR_KEY));
		Assert.assertEquals(FogbowApplication.IMAGE1_NAME,
				resourceInfo.get(ResourcePropertiesConstants.IMAGE_KEY));
	}

	@Test
	public void testGetTwoResourceInfo() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(2, defaultProperties);
		Assert.assertEquals(2, requestIds.size());
		Assert.assertTrue(requestIds.contains(FIRST_ID));
		Assert.assertTrue(requestIds.contains(SECOND_ID));

		// checking getting resourceInfo
		Map<String, String> resourceInfo = infra.getResourceInfo(FIRST_ID);
		Assert.assertEquals(FIRST_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		Assert.assertEquals(RequestState.OPEN.getValue(),
				resourceInfo.get(RequestAttribute.STATE.getValue()));
		Assert.assertEquals(2,
				Integer.parseInt(resourceInfo.get(RequestAttribute.INSTANCE_COUNT.getValue())));
		Assert.assertEquals("null", resourceInfo.get(RequestAttribute.INSTANCE_ID.getValue()));
		Assert.assertEquals(RequestType.ONE_TIME.getValue(),
				resourceInfo.get(RequestAttribute.TYPE.getValue()));
		Assert.assertEquals(RequestConstants.SMALL_TERM,
				resourceInfo.get(ResourcePropertiesConstants.FLAVOR_KEY));
		Assert.assertEquals(FogbowApplication.IMAGE1_NAME,
				resourceInfo.get(ResourcePropertiesConstants.IMAGE_KEY));

		resourceInfo = infra.getResourceInfo(SECOND_ID);
		Assert.assertEquals(SECOND_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		Assert.assertEquals(RequestState.OPEN.getValue(),
				resourceInfo.get(RequestAttribute.STATE.getValue()));
		Assert.assertEquals(2,
				Integer.parseInt(resourceInfo.get(RequestAttribute.INSTANCE_COUNT.getValue())));
		Assert.assertEquals("null", resourceInfo.get(RequestAttribute.INSTANCE_ID.getValue()));
		Assert.assertEquals(RequestType.ONE_TIME.getValue(),
				resourceInfo.get(RequestAttribute.TYPE.getValue()));
		Assert.assertEquals(RequestConstants.SMALL_TERM,
				resourceInfo.get(ResourcePropertiesConstants.FLAVOR_KEY));
		Assert.assertEquals(FogbowApplication.IMAGE1_NAME,
				resourceInfo.get(ResourcePropertiesConstants.IMAGE_KEY));
	}

	@Test
	public void testGetFullfieldResourceInfo() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// changing request state
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.FULFILLED);

		// checking getting resourceInfo
		Map<String, String> resourceInfo = infra.getResourceInfo(FIRST_ID);
		Assert.assertEquals(FIRST_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		Assert.assertEquals(RequestState.FULFILLED.getValue(),
				resourceInfo.get(RequestAttribute.STATE.getValue()));
		Assert.assertEquals(1,
				Integer.parseInt(resourceInfo.get(RequestAttribute.INSTANCE_COUNT.getValue())));
		Assert.assertEquals(FogbowApplication.INSTANCE_PREFIX + FIRST_ID,
				resourceInfo.get(RequestAttribute.INSTANCE_ID.getValue()));
		Assert.assertEquals(RequestType.ONE_TIME.getValue(),
				resourceInfo.get(RequestAttribute.TYPE.getValue()));
		Assert.assertEquals(RequestConstants.SMALL_TERM,
				resourceInfo.get(ResourcePropertiesConstants.FLAVOR_KEY));
		Assert.assertEquals(FogbowApplication.IMAGE1_NAME,
				resourceInfo.get(ResourcePropertiesConstants.IMAGE_KEY));
		
		// checking instance attributes
		Assert.assertEquals("x86",
				resourceInfo.get("occi.compute.architecture"));
		Assert.assertEquals("active",
				resourceInfo.get("occi.compute.state"));
		Assert.assertEquals("1024",
				resourceInfo.get("occi.compute.memory"));
		Assert.assertEquals("1.0",
				resourceInfo.get("occi.compute.speed"));
		Assert.assertEquals("1.0",
				resourceInfo.get("occi.compute.cores"));
		Assert.assertEquals("server-instance_first",
				resourceInfo.get("occi.compute.hostname"));
		Assert.assertEquals(FogbowTestHelper.FOGBOW_ADDRESS + ":" + FogbowApplication.DEFAULT_SSH_PORT,
				resourceInfo.get(Instance.SSH_PUBLIC_ADDRESS_ATT));
	}

	@Test
	public void testIsNotAvailableNoExistingResource() throws InfrastructureException {
		Assert.assertFalse(infra.isResourceAvailable("no_existing"));	
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testIsNotAvailableEmptyResourceId() throws InfrastructureException {
		Assert.assertFalse(infra.isResourceAvailable(""));	
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testIsNotAvailableNullResourceId() throws InfrastructureException {
		Assert.assertFalse(infra.isResourceAvailable(null));	
	}
	
	@Test
	public void testIsNotAvailableOpenResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// checking getting resourceInfo
		Assert.assertFalse(infra.isResourceAvailable(FIRST_ID));	
	}
	
	@Test
	public void testIsNotAvailableClosedResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// changing request state
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.CLOSED);
				
		// checking getting resourceInfo
		Assert.assertFalse(infra.isResourceAvailable(FIRST_ID));	
	}
	
	@Test
	public void testIsNotAvailableDeletedResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// changing request state
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.DELETED);
				
		// checking getting resourceInfo
		Assert.assertFalse(infra.isResourceAvailable(FIRST_ID));	
	}
	
	@Test
	public void testIsNotAvailableFailedResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// changing request state
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.FAILED);
				
		// checking getting resourceInfo
		Assert.assertFalse(infra.isResourceAvailable(FIRST_ID));	
	}
	
	@Test
	public void testIsAvailableFulfilledResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// changing request state
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.FULFILLED);

		// checking getting resourceInfo
		Assert.assertTrue(infra.isResourceAvailable(FIRST_ID));	
	}
	
	@Test
	public void testGetDeletedResourceInfo() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// changing request state
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.FULFILLED);
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.DELETED);

		// checking getting resourceInfo
		Map<String, String> resourceInfo = infra.getResourceInfo(FIRST_ID);
		Assert.assertEquals(FIRST_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		Assert.assertEquals(RequestState.DELETED.getValue(),
				resourceInfo.get(RequestAttribute.STATE.getValue()));
		Assert.assertEquals(1,
				Integer.parseInt(resourceInfo.get(RequestAttribute.INSTANCE_COUNT.getValue())));
		Assert.assertEquals(FogbowApplication.INSTANCE_PREFIX + FIRST_ID,
				resourceInfo.get(RequestAttribute.INSTANCE_ID.getValue()));
		Assert.assertEquals(RequestType.ONE_TIME.getValue(),
				resourceInfo.get(RequestAttribute.TYPE.getValue()));
		Assert.assertEquals(RequestConstants.SMALL_TERM,
				resourceInfo.get(ResourcePropertiesConstants.FLAVOR_KEY));
		Assert.assertEquals(FogbowApplication.IMAGE1_NAME,
				resourceInfo.get(ResourcePropertiesConstants.IMAGE_KEY));
	}

	@Test(expected = InfrastructureException.class)
	public void testGetResourceInfoInvalidId() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// checking getting resourceInfo
		infra.getResourceInfo("invalid_id");
	}

	@Test(expected = InfrastructureException.class)
	public void testGetResourceInfoNullId() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// checking getting resourceInfo
		infra.getResourceInfo(null);
	}

	@Test(expected = InfrastructureException.class)
	public void testGetResourceInfoEmptyId() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// checking getting resourceInfo
		infra.getResourceInfo("");
	}

	@Test
	public void testDeleteOneResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);

		// checking
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// checking resource was created
		Map<String, String> resourceInfo = infra.getResourceInfo(FIRST_ID);
		Assert.assertEquals(FIRST_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));

		// deleting resource
		infra.deleteResource(FIRST_ID);

		// checking resource was deleted
		try {
			resourceInfo = infra.getResourceInfo(FIRST_ID);
			Assert.fail("There was exception in getResourcesInfo after deletion");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("Not Found"));
		}
	}

	@Test
	public void testDeleteOneResourceAndNotDeleteOthers() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(5, defaultProperties);

		// checking
		Assert.assertEquals(5, requestIds.size());
		Assert.assertTrue(requestIds.contains(FIRST_ID));
		Assert.assertTrue(requestIds.contains(SECOND_ID));
		Assert.assertTrue(requestIds.contains(THIRD_ID));
		Assert.assertTrue(requestIds.contains(FOURTH_ID));
		Assert.assertTrue(requestIds.contains(FIFTH_ID));

		// checking resource was created
		Map<String, String> resourceInfo = infra.getResourceInfo(FIRST_ID);
		Assert.assertEquals(FIRST_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		resourceInfo = infra.getResourceInfo(SECOND_ID);
		Assert.assertEquals(SECOND_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		resourceInfo = infra.getResourceInfo(THIRD_ID);
		Assert.assertEquals(THIRD_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		resourceInfo = infra.getResourceInfo(FOURTH_ID);
		Assert.assertEquals(FOURTH_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		resourceInfo = infra.getResourceInfo(FIFTH_ID);
		Assert.assertEquals(FIFTH_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));

		// deleting resource
		infra.deleteResource(FIRST_ID);

		// checking resource was deleted
		try {
			resourceInfo = infra.getResourceInfo(FIRST_ID);
			Assert.fail("There was exception in getResourcesInfo after deletion");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("Not Found"));
		}

		// checking if other resources were not deleted
		resourceInfo = infra.getResourceInfo(SECOND_ID);
		Assert.assertEquals(SECOND_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		resourceInfo = infra.getResourceInfo(THIRD_ID);
		Assert.assertEquals(THIRD_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		resourceInfo = infra.getResourceInfo(FOURTH_ID);
		Assert.assertEquals(FOURTH_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		resourceInfo = infra.getResourceInfo(FIFTH_ID);
		Assert.assertEquals(FIFTH_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
	}

	@Test
	public void testDeleteOneFulfielledResource() throws InfrastructureException {
		// creating resources
		List<String> requestIds = infra.createResource(1, defaultProperties);
		Assert.assertEquals(1, requestIds.size());
		Assert.assertEquals(FIRST_ID, requestIds.get(0));

		// changing request state
		helper.getFogbowApplication().changeRequestState(FIRST_ID, RequestState.FULFILLED);

		// checking resource was created
		Map<String, String> resourceInfo = infra.getResourceInfo(FIRST_ID);
		Assert.assertEquals(FIRST_ID, resourceInfo.get(FogbowContants.OCCI_CORE_ID));
		Assert.assertEquals(RequestState.FULFILLED.getValue(),
				resourceInfo.get(RequestAttribute.STATE.getValue()));

		// deleting resource
		infra.deleteResource(FIRST_ID);

		// checking resource was deleted
		try {
			resourceInfo = infra.getResourceInfo(FIRST_ID);
			Assert.fail("There was exception in getResourcesInfo after deletion");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("Not Found"));
		}
	}

}
