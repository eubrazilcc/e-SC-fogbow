package org.fogbowcloud.infrastructure.fogbow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.infrastructure.fogbow.FogbowContants;
import org.fogbowcloud.manager.occi.model.Category;
import org.fogbowcloud.manager.occi.model.ErrorType;
import org.fogbowcloud.manager.occi.model.HeaderUtils;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.OCCIHeaders;
import org.fogbowcloud.manager.occi.model.Resource;
import org.fogbowcloud.manager.occi.model.ResourceRepository;
import org.fogbowcloud.manager.occi.model.ResponseConstants;
import org.fogbowcloud.manager.occi.model.Token;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.request.Request;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestServerResource;
import org.fogbowcloud.manager.occi.request.RequestState;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;

/**
 * The Fogbbow Application to be used by testing. Simulates the Fogbow
 * behaviour.
 * 
 * @author giovanni
 *
 */
public class FogbowApplication extends Application {

	public static final String IMAGE1_NAME = "image1";
	public static final String IMAGE2_NAME = "image2";
	public static final String INSTANCE_PREFIX = "instance_";

	private ResourceRepository repository = ResourceRepository.getInstance();

	private Map<String, List<String>> userToRequestIds;
	private Map<String, Request> requestIdToRequest;
	private Map<String, Instance> requestIdToInstance;
	private Map<String, String> authTokenToUser;
	private String[] expectedIds;
	private int numberOfRequests;
	private String fogbowEndpoint;

	private static final Logger LOGGER = Logger.getLogger(FogbowApplication.class);
	public static final String DEFAULT_SSH_PORT = "40000";

	public FogbowApplication(String fogbowEndpoint, String[] expectedIds) {
		repository.addImageResource(IMAGE1_NAME);
		repository.addImageResource(IMAGE2_NAME);

		this.fogbowEndpoint = fogbowEndpoint;
		this.numberOfRequests = 0;
		this.expectedIds = expectedIds;
		this.userToRequestIds = new HashMap<String, List<String>>();
		this.requestIdToRequest = new HashMap<String, Request>();
		this.requestIdToInstance = new HashMap<String, Instance>();
		this.authTokenToUser = new HashMap<String, String>();
	}

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/-/", FakeQueryServerResource.class);
		router.attach("/compute/", FakeComputeServerResource.class);
		router.attach("/compute/{instance_id}", FakeComputeServerResource.class);
		router.attach("/fogbow_request", FakeRequestServerResource.class);
		router.attach("/fogbow_request/", FakeRequestServerResource.class);
		router.attach("/fogbow_request/{requestid}", FakeRequestServerResource.class);
		return router;
	}

	public void checkUserByAccessId(String authToken) {
		if (authTokenToUser.get(authToken) == null) {
			throw new OCCIException(ErrorType.UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
		}
	}

	private void checkRequestId(String authToken, String requestId) {
		String user = authTokenToUser.get(authToken);

		if (userToRequestIds.get(user) == null || !userToRequestIds.get(user).contains(requestId)) {
			throw new OCCIException(ErrorType.NOT_FOUND, ResponseConstants.NOT_FOUND);
		}
	}

	public void addTokenToUser(String authToken, String username) {
		authTokenToUser.put(authToken, username);
	}

	public void changeRequestState(String requestId, RequestState newState) {
		Request request = requestIdToRequest.get(requestId);
		if (request != null) {
			if (newState.equals(RequestState.FULFILLED) || newState.equals(RequestState.CLOSED)) {
				request.setState(newState);
				request.setInstanceId(INSTANCE_PREFIX + requestId);

				Map<String, String> attributes = new HashMap<String, String>();
				attributes.put("occi.compute.architecture", "x86");
				attributes.put("occi.compute.state", "active");
				attributes.put("occi.compute.speed", "1.0");
				attributes.put("occi.compute.memory", "1024");
				attributes.put("occi.compute.cores", "1.0");
				attributes.put("occi.compute.hostname", "server-" + INSTANCE_PREFIX + requestId);
				attributes.put("occi.core.id", INSTANCE_PREFIX + requestId);
				attributes.put(Instance.SSH_PUBLIC_ADDRESS_ATT, FogbowTestHelper.FOGBOW_ADDRESS
						+ ":" + DEFAULT_SSH_PORT);

				Category category = getCategory(request.getCategories(),
						RequestConstants.TEMPLATE_RESOURCE_SCHEME);
				List<Resource> resources = new ArrayList<Resource>();
				resources.add(repository.get("compute"));
				resources.add(repository.get("os_tpl"));
				resources.add(repository.get(category.getTerm()));

				String image = getCategory(request.getCategories(),
						RequestConstants.TEMPLATE_OS_SCHEME).getTerm();
				if (ResourceRepository.getInstance().get(image) != null) {
					resources.add(ResourceRepository.getInstance().get(image));
				}

				Instance instance = null;
				/*instance = new Instance(INSTANCE_PREFIX + requestId, resources,attributes, new ArrayList<Instance.Link>());*/
				requestIdToInstance.put(INSTANCE_PREFIX + requestId, instance);
			} else if (newState.equals(RequestState.OPEN) || newState.equals(RequestState.FAILED)) {
				request.setState(newState);
				request.setInstanceId(null);
			} else {
				request.setState(newState);
			}
		}
	}

	private Category getCategory(List<Category> categories, String scheme) {
		for (Category category : categories) {
			if (category.getScheme().equals(scheme)) {
				return category;
			}
		}
		return null;
	}

	public List<Request> createRequests(String authToken, List<Category> categories,
			Map<String, String> xOCCIAtt) {
		checkUserByAccessId(authToken);

		String user = authTokenToUser.get(authToken);
		Integer instanceCount = Integer.valueOf(xOCCIAtt.get(RequestAttribute.INSTANCE_COUNT
				.getValue()));
		LOGGER.debug("Request " + instanceCount + " instances");

		List<Request> newRequests = new ArrayList<Request>();
		for (int i = 0; i < instanceCount; i++) {
			String requestId = expectedIds[numberOfRequests];
			numberOfRequests++;

			Request request = null;
			/*request = new Request(requestId, new Token(authToken, user, null,
					new HashMap<String, String>()), new LinkedList<Category>(categories),
					new HashMap<String, String>(xOCCIAtt));*/
			LOGGER.debug("Created request: " + request);
			newRequests.add(request);

			if (userToRequestIds.get(user) == null) {
				userToRequestIds.put(user, new ArrayList<String>());
			}
			userToRequestIds.get(user).add(requestId);
			requestIdToRequest.put(requestId, request);
		}
		return newRequests;
	}

	public void removeRequest(String authToken, String requestId) {
		checkUserByAccessId(authToken);
		checkRequestId(authToken, requestId);
		requestIdToRequest.remove(requestId);
		requestIdToInstance.remove(INSTANCE_PREFIX + requestId);
		userToRequestIds.get(authTokenToUser.get(authToken)).remove(requestId);
	}

	public void removeAllRequests(String authToken) {
		checkUserByAccessId(authToken);
		for (String requestId : userToRequestIds.get(authTokenToUser.get(authToken))) {
			requestIdToRequest.remove(requestId);
		}
		userToRequestIds.remove(authTokenToUser.get(authToken));
	}

	public Request getRequest(String authToken, String requestId) {
		checkUserByAccessId(authToken);
		checkRequestId(authToken, requestId);
		return requestIdToRequest.get(requestId);
	}

	public List<Request> getAllRequests(String authToken) {
		checkUserByAccessId(authToken);
		String user = authTokenToUser.get(authToken);
		List<Request> requests = new ArrayList<Request>();
		for (String requestId : userToRequestIds.get(user)) {
			requests.add(requestIdToRequest.get(requestId));
		}
		return requests;
	}

	public List<Resource> getResources() {
		return repository.getAll();
	}

	public String getEndpoint() {
		return fogbowEndpoint;
	}

	public static class FakeQueryServerResource extends ServerResource {

		private static final Logger LOGGER = Logger.getLogger(FakeQueryServerResource.class);

		@Get
		public String fetch() {
			FogbowApplication fogbowApp = (FogbowApplication) getApplication();
			HttpRequest req = (HttpRequest) getRequest();
			String authToken = req.getHeaders().getValues(OCCIHeaders.X_AUTH_TOKEN);
			LOGGER.debug("Quering resources using auth token: " + authToken);
			fogbowApp.checkUserByAccessId(authToken);
			return generateQueryResonse(fogbowApp.getResources());
		}

		private String generateQueryResonse(List<Resource> resources) {
			String response = "";
			for (Resource resource : resources) {
				response += "Category: " + resource.toHeader() + "\n";
			}
			return "\n" + response.trim();
		}
	}

	public static class FakeComputeServerResource extends ServerResource {

		private static final Logger LOGGER = Logger.getLogger(FakeComputeServerResource.class);

		@Get
		public StringRepresentation fetch() {
			FogbowApplication fogbowApp = (FogbowApplication) getApplication();
			HttpRequest req = (HttpRequest) getRequest();
			String authToken = req.getHeaders().getValues(OCCIHeaders.X_AUTH_TOKEN);
			String instanceId = (String) getRequestAttributes().get("instance_id");

			LOGGER.info("Getting instance " + instanceId);
			if (instanceId == null) {
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
			}

			Instance instance = fogbowApp.getInstance(authToken, instanceId);
			LOGGER.debug("Instance OCCI format " + instance.toOCCIMessageFormatDetails());

			return new StringRepresentation(instance.toOCCIMessageFormatDetails(),
					MediaType.TEXT_PLAIN);
		}

		@Delete
		public String remove() {
			String instanceId = (String) getRequestAttributes().get("instance_id");
			LOGGER.debug("Removing instance " + instanceId);
			if (instanceId == null) {
				// the InfrastructureFacade always need to specify the compute
				// instance
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST);
			}
			return "OK";
		}
	}

	public static class FakeRequestServerResource extends ServerResource {

		private static final Logger LOGGER = Logger.getLogger(FakeRequestServerResource.class);
		public static final String OCCI_CORE_TITLE = "occi.core.title";

		@Get
		public String fetch() {
			FogbowApplication fogbowApp = (FogbowApplication) getApplication();
			HttpRequest req = (HttpRequest) getRequest();
			String authToken = req.getHeaders().getValues(OCCIHeaders.X_AUTH_TOKEN);

			String requestId = (String) getRequestAttributes().get("requestid");
			if (requestId == null) {
				LOGGER.debug("Getting all requests using auth token: " + authToken);
				return generateTextPlainResponse(fogbowApp.getAllRequests(authToken), req);
			}
			LOGGER.debug("Getting request " + requestId + " using auth token: " + authToken);

			return generateOneRequestResponse(fogbowApp.getRequest(authToken, requestId));
		}

		public String generateOneRequestResponse(Request request) {
			LOGGER.debug("Generating response to request: " + request);
			String requestOCCIFormat = "\n";
			for (Category category : request.getCategories()) {
				LOGGER.debug("Resource exists? "
						+ (ResourceRepository.getInstance().get(category.getTerm()) != null));
				try {
					requestOCCIFormat += "Category: "
							+ ResourceRepository.getInstance().get(category.getTerm()).toHeader()
							+ "\n";
				} catch (Exception e) {
					LOGGER.error(e);
				}
			}

			Map<String, String> attToOutput = new HashMap<String, String>();
			attToOutput.put(FogbowContants.OCCI_CORE_ID, request.getId());
			if (request.getAttValue(OCCI_CORE_TITLE) != null) {
				attToOutput.put(OCCI_CORE_TITLE, request.getAttValue(OCCI_CORE_TITLE));
			}
			for (String attributeName : RequestAttribute.getValues()) {
				if (request.getAttValue(attributeName) == null) {
					attToOutput.put(attributeName, "Not defined");
				} else {
					attToOutput.put(attributeName, request.getAttValue(attributeName));
				}
			}

			attToOutput.put(RequestAttribute.STATE.getValue(), request.getState().getValue());
			attToOutput.put(RequestAttribute.INSTANCE_ID.getValue(), request.getInstanceId());

			for (String attName : attToOutput.keySet()) {
				requestOCCIFormat += OCCIHeaders.X_OCCI_ATTRIBUTE + ": " + attName + "=\""
						+ attToOutput.get(attName) + "\" \n";
			}

			return "\n" + requestOCCIFormat.trim();
		}

		@Post
		public String post() {
			FogbowApplication fogbowApp = (FogbowApplication) getApplication();
			HttpRequest req = (HttpRequest) getRequest();

			List<Category> categories = HeaderUtils.getCategories(req.getHeaders());
			LOGGER.debug("categories: " + categories);
			HeaderUtils.checkCategories(categories, RequestConstants.TERM);
			HeaderUtils.checkOCCIContentType(req.getHeaders());
			Map<String, String> xOCCIAtt = HeaderUtils.getXOCCIAtributes(req.getHeaders());
			xOCCIAtt = RequestServerResource.normalizeXOCCIAtt(xOCCIAtt);
			LOGGER.debug("normalized xOCCIAtt: " + xOCCIAtt);
			String authToken = req.getHeaders().getValues(OCCIHeaders.X_AUTH_TOKEN);

			LOGGER.debug("Posting new request using auth token: " + authToken + " categories: "
					+ categories + " xOCCIAtt: " + xOCCIAtt);

			return generateTextPlainResponse(
					fogbowApp.createRequests(authToken, categories, xOCCIAtt), req);
		}

		private String generateTextPlainResponse(List<Request> requests, HttpRequest req) {
			if (requests == null || requests.isEmpty()) {
				return "There is not requests";
			}
			FogbowApplication fogbowApp = (FogbowApplication) getApplication();
			String requestEndpoint = fogbowApp.getEndpoint() + req.getHttpCall().getRequestUri();
			String response = "";
			Iterator<Request> requestIt = requests.iterator();
			while (requestIt.hasNext()) {
				Request request = requestIt.next();
				String prefixOCCILocation = "";
				if (requestEndpoint.endsWith("/")) {
					prefixOCCILocation += HeaderUtils.X_OCCI_LOCATION_PREFIX + requestEndpoint;
				} else {
					prefixOCCILocation += HeaderUtils.X_OCCI_LOCATION_PREFIX + requestEndpoint
							+ "/";
				}
				response += prefixOCCILocation + request.getId() + "\n";
			}
			return response.length() > 0 ? response.trim() : "\n";
		}

		@Delete
		public String remove() {
			FogbowApplication fogbowApp = (FogbowApplication) getApplication();
			HttpRequest req = (HttpRequest) getRequest();
			String authToken = req.getHeaders().getValues(OCCIHeaders.X_AUTH_TOKEN);
			String requestId = (String) getRequestAttributes().get("requestid");

			if (requestId == null) {
				LOGGER.debug("Removing all requests using auth token: " + authToken);
				fogbowApp.removeAllRequests(authToken);
			} else {
				LOGGER.debug("Removing request " + requestId + " using auth token: " + authToken);
				fogbowApp.removeRequest(authToken, requestId);
			}
			return "OK";
		}
	}

	public Instance getInstance(String authToken, String instanceId) {
		return requestIdToInstance.get(instanceId);
	}
}
