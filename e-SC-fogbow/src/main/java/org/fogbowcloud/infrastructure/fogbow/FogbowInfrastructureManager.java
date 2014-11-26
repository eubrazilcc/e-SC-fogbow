package org.fogbowcloud.infrastructure.fogbow;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ConfigurationConstants;
import org.fogbowcloud.infrastructure.core.CommandResult;
import org.fogbowcloud.infrastructure.core.InfrastructureException;
import org.fogbowcloud.infrastructure.core.InfrastructureManager;
import org.fogbowcloud.infrastructure.core.ResourcePropertiesConstants;
import org.fogbowcloud.infrastructure.core.SSHUtils;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.util.Credential;
import org.fogbowcloud.manager.occi.core.Category;
import org.fogbowcloud.manager.occi.core.HeaderUtils;
import org.fogbowcloud.manager.occi.core.Token;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestState;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

public class FogbowInfrastructureManager implements InfrastructureManager {

	private static final String CATEGORY_PREFIX = "Category:";
	private static final String X_OCCI_ATTRIBUTE_PREFIX = "X-OCCI-Attribute: ";
	protected static final String PLUGIN_PACKAGE = "org.fogbowcloud.manager.core.plugins";
	private String authToken = null;
	private String fogbowEndpoint = null;
	private static HttpClient client;

	private static final Logger LOGGER = Logger.getLogger(FogbowInfrastructureManager.class);
	
	public FogbowInfrastructureManager(Properties properties) {
		String endpoint = properties.getProperty(ConfigurationConstants.INFRA_ENDPOINT);
		if (endpoint == null || endpoint.isEmpty()) {
			throw new IllegalArgumentException("The fogbowEndpoint must not be null or empty.");
		}
		this.fogbowEndpoint = endpoint;
	}
	
	@Override
	public String configure(Map<String, String> credentials)
			throws InfrastructureException {
		LOGGER.info("Configuring the insfrastructure with endpoint=" + fogbowEndpoint
				+ " and credentials=" + credentials);
//		if (endpoint == null || endpoint.isEmpty()) {
//			throw new InfrastructureException("The fogbowEndpoint must not be null or empty.");
//		}
		if (!credentials.containsKey(FogbowContants.PLUGIN_TYPE_KEY)) {
			throw new InfrastructureException("Plugin type was not specified.");
		}

		IdentityPlugin identityPlugin = createIdentityPlugin(
				credentials.remove(FogbowContants.PLUGIN_TYPE_KEY), credentials);
		try {
			Token token = identityPlugin.createToken(credentials);
			this.authToken = token.getAccessId();
		} catch (Exception e) {
			LOGGER.error("Exception while creating token.", e);
			throw new InfrastructureException(e.getMessage() + "\n"
					+ getPluginCredentialsInformation(identityPlugin));
		}

		// getting fogbow resources to check if fogbowEndpoint is valid
		try {
			doRequest("get", fogbowEndpoint + "/-/", authToken, new HashSet<Header>());
		} catch (URISyntaxException | HttpException | IOException e) {
			LOGGER.error("Exception while chechking fogbow endpoint.", e);
			throw new InfrastructureException("The fogbowEndpoint " + fogbowEndpoint
					+ " is invalid or the service is down.");
		}
//		this.fogbowEndpoint = endpoint;
		return authToken;

	}

	private IdentityPlugin createIdentityPlugin(String pluginType, Map<String, String> credentials)
			throws InfrastructureException {
		Reflections reflections = new Reflections(ClasspathHelper.forPackage(PLUGIN_PACKAGE),
				new SubTypesScanner());

		Set<Class<? extends IdentityPlugin>> allClasses = reflections
				.getSubTypesOf(IdentityPlugin.class);
		Class<?> pluginClass = null;
		List<String> possibleTypes = new LinkedList<String>();
		for (Class<? extends IdentityPlugin> eachClass : allClasses) {
			String[] packageName = eachClass.getName().split("\\.");
			String type = packageName[packageName.length - 2];
			possibleTypes.add(type);
			if (type.equals(pluginType)) {
				pluginClass = eachClass;
			}
		}
		IdentityPlugin identityPlugin;
		try {
			identityPlugin = (IdentityPlugin) createInstance(pluginClass, new Properties());
			return identityPlugin;
		} catch (Exception e) {
			LOGGER.error("Exception while creating instance of IdentityPlugin.", e);
			throw new InfrastructureException("Token type [" + pluginType + "] is not valid. "
					+ "Possible types: " + possibleTypes + ".");
		}
	}

	private static Object createInstance(Class<?> pluginClass, Properties properties)
			throws Exception {
		return pluginClass.getConstructor(Properties.class).newInstance(properties);
	}

	private static String getPluginCredentialsInformation(IdentityPlugin identityPlugin) {
		StringBuilder response = new StringBuilder();
		String[] identityPluginFullName = identityPlugin.getClass().getName().split("\\.");
		response.append("Credentials for "
				+ identityPluginFullName[identityPluginFullName.length - 2] + " are:\n");

		if (identityPlugin.getCredentials() != null) {
			for (Credential credential : identityPlugin.getCredentials()) {
				String valueDefault = "";
				if (credential.getValueDefault() != null) {
					valueDefault = " - default :" + credential.getValueDefault();
				}
				String feature = "Optional";
				if (credential.isRequired()) {
					feature = "Required";
				}
				response.append("	" + credential.getName() + " (" + feature + ")" + valueDefault
						+ "\n");
			}
		}
		return response.toString().trim();
	}

	@Override
	public List<String> createResource(int numberOfInstanes, Map<String, String> properties)
			throws InfrastructureException {
		LOGGER.info("Creating " + numberOfInstanes + " resources with properties=" + properties);
		checkValidNumber(numberOfInstanes);
		checkValidMap(properties);
		checkConfigured();

		String requestType = properties.get(FogbowContants.TYPE_KEY);
		if (requestType == null || requestType.isEmpty()) {
			requestType = RequestConstants.DEFAULT_TYPE;
		}
		Set<Header> headers = new HashSet<Header>();
		headers.add(new BasicHeader("Category", RequestConstants.TERM + "; scheme=\""
				+ RequestConstants.SCHEME + "\"; class=\"" + RequestConstants.KIND_CLASS + "\""));
		headers.add(new BasicHeader("X-OCCI-Attribute", RequestAttribute.INSTANCE_COUNT.getValue()
				+ "=" + numberOfInstanes));
		headers.add(new BasicHeader("X-OCCI-Attribute", RequestAttribute.TYPE.getValue() + "="
				+ requestType));
		headers.add(new BasicHeader("Category", properties.get(ResourcePropertiesConstants.FLAVOR_KEY)
				+ "; scheme=\"" + RequestConstants.TEMPLATE_RESOURCE_SCHEME + "\"; class=\""
				+ RequestConstants.MIXIN_CLASS + "\""));
		headers.add(new BasicHeader("Category", properties.get(ResourcePropertiesConstants.IMAGE_KEY)
				+ "; scheme=\"" + RequestConstants.TEMPLATE_OS_SCHEME + "\"; class=\""
				+ RequestConstants.MIXIN_CLASS + "\""));

		if (properties.get(ResourcePropertiesConstants.PUBLICKEY_KEY) != null
				&& !properties.get(ResourcePropertiesConstants.PUBLICKEY_KEY).isEmpty()) {
			headers.add(new BasicHeader("Category", RequestConstants.PUBLIC_KEY_TERM
					+ "; scheme=\"" + RequestConstants.CREDENTIALS_RESOURCE_SCHEME + "\"; class=\""
					+ RequestConstants.MIXIN_CLASS + "\""));
			headers.add(new BasicHeader("X-OCCI-Attribute", RequestAttribute.DATA_PUBLIC_KEY
					.getValue() + "=" + properties.get(ResourcePropertiesConstants.PUBLICKEY_KEY)));
		}

		String responseStr;
		try {
			responseStr = doRequest("post", fogbowEndpoint + "/" + RequestConstants.TERM,
					authToken, headers);
			LOGGER.debug("post response=" + responseStr);
			return getRequestIds(responseStr);
		} catch (URISyntaxException | HttpException | IOException e) {
			LOGGER.error("Exception while doing post on fogbowEndpoint.", e);
			throw new InfrastructureException(e.getMessage());
		}
	}

	private List<String> getRequestIds(String responseStr) {
		List<String> requestIds = new ArrayList<String>();
		String[] requestLocations = responseStr.split("\n");
		for (String reqLocation : requestLocations) {
			if (!reqLocation.isEmpty()) {
				requestIds.add(normalizeRequestId(reqLocation));
			}
		}
		return requestIds;
	}

	private String normalizeRequestId(String reqLocation) {
		String[] tokens = reqLocation.split("/");
		return tokens[tokens.length - 1];
	}

	private void checkConfigured() throws InfrastructureException {
		if (authToken == null || fogbowEndpoint == null) {
			throw new InfrastructureException(
					"You must previously configure the infrastructure with a valid user and endpoint service.");
		}
	}

	private void checkValidMap(Map<String, String> map) throws InfrastructureException {
		if (map == null || map.isEmpty()) {
			throw new InfrastructureException("The map must not be null or empty.");
		}
	}

	private void checkValidNumber(int integer) throws InfrastructureException {
		if (integer < 1) {
			throw new InfrastructureException("The number " + integer + " is invalid.");
		}
	}

	@Override
	public Map<String, String> getResourceInfo(String id) throws InfrastructureException {
		LOGGER.info("Getting resource info from id=" + id);

		checkConfigured();
		checkValidId(id);

		try {
			String requestResponseStr = doRequest("get", fogbowEndpoint + "/"
					+ RequestConstants.TERM + "/" + id, authToken, new HashSet<Header>());
			Map<String, String> resourcesInfo = new HashMap<String, String>();
			LOGGER.debug("get request response=" + requestResponseStr);

			Map<String, String> requestAtt = getAttributes(requestResponseStr);
			resourcesInfo.putAll(requestAtt);

			List<Category> requestCategories = getCategories(requestResponseStr);
			// getting flavor info
			resourcesInfo.put(ResourcePropertiesConstants.FLAVOR_KEY,
					getCategoryTerm(requestCategories, RequestConstants.TEMPLATE_RESOURCE_SCHEME));
			// getting image info
			resourcesInfo.put(ResourcePropertiesConstants.IMAGE_KEY,
					getCategoryTerm(requestCategories, RequestConstants.TEMPLATE_OS_SCHEME));

			// if request is fulfilled we must add instance attributes in
			// response
			if (requestAtt.get(RequestAttribute.STATE.getValue()).equals(
					RequestState.FULFILLED.getValue())) {
				String instanceId = requestAtt.get(RequestAttribute.INSTANCE_ID.getValue());

				// getting the instance
				if (instanceId != null && !"null".equals(instanceId) && !instanceId.isEmpty()) {
					try {
						String instanceResponseStr = doRequest("get", fogbowEndpoint + "/compute/"
								+ instanceId, authToken, new HashSet<Header>());
						LOGGER.debug("get instance response=" + instanceResponseStr);
						Map<String, String> instanceAtt = getAttributes(instanceResponseStr);

						// removing "occi.core.id" attributes from instance
						// because ti is using request id as unique request
						instanceAtt.remove(FogbowContants.OCCI_CORE_ID);
						resourcesInfo.putAll(instanceAtt);

						return resourcesInfo;
					} catch (URISyntaxException | HttpException | IOException e) {
						LOGGER.warn("Exception while getting the instance " + instanceId + ".", e);
					}
				} else {
					LOGGER.warn("The request is FULFILLED but there is not instance. "
							+ "This is not expected behaviour, report fogbow team.");
				}

			}
			return resourcesInfo;
		} catch (URISyntaxException | HttpException | IOException e) {
			LOGGER.error("Exception while getting requestId " + id + ".", e);
			throw new InfrastructureException(e.getMessage());
		}
	}

	private String getCategoryTerm(List<Category> categories, String scheme) {
		for (Category category : categories) {
			if (category.getScheme().equals(scheme)) {
				return category.getTerm();
			}
		}
		return null;
	}

	private void checkValidId(String id) throws InfrastructureException {
		if (id == null || id.isEmpty()) {
			throw new InfrastructureException("The resource id " + id
					+ " is invalid. It must not be null or empty.");
		}
	}

	private List<Category> getCategories(String resourceStr) {
		List<Category> categories = new ArrayList<Category>();

		String[] lines = resourceStr.split("\n");
		for (String line : lines) {
			if (line.contains(CATEGORY_PREFIX)) {
				String[] blockLine = line.split(";");
				String[] blockValues;
				String term = "";
				String scheme = "";
				String catClass = "";
				List<String> attributesResource = new ArrayList<String>();
				List<String> actionsResource = new ArrayList<String>();

				for (String block : blockLine) {
					if (block.contains(CATEGORY_PREFIX)) {
						blockValues = block.split(":");
						term = blockValues[1].trim();
					} else {
						blockValues = block.split("=");
						if (blockValues[0].contains("scheme")) {
							scheme = blockValues[1].replace("\"", "").trim();
						} else if (blockValues[0].contains("class")) {
							catClass = blockValues[1].replace("\"", "").trim();
						} else if (blockValues[0].contains("attributes")) {
							String[] attributesValues = blockValues[1].replace("\"", "").split(" ");
							for (String attribute : attributesValues) {
								attributesResource.add(attribute);
							}
						} else if (blockValues[0].contains("actions")) {
							String[] actionsValues = blockValues[1].replace("\"", "").split(" ");
							for (String action : actionsValues) {
								actionsResource.add(action);
							}
						}
					}
				}
				categories.add(new Category(term, scheme, catClass));
			}
		}
		return categories;
	}

	private Map<String, String> getAttributes(String resourceStr) {
		Map<String, String> attributes = new HashMap<String, String>();
		String[] lines = resourceStr.split("\n");
		for (String line : lines) {
			if (line.contains(X_OCCI_ATTRIBUTE_PREFIX)) {
				String[] lineTokens = line.replace(X_OCCI_ATTRIBUTE_PREFIX, "").trim().split("=");
				attributes.put(lineTokens[0], lineTokens[1].replace("\"", "").trim());
			}
		}
		return attributes;
	}

	@Override
	public boolean isResourceAvailable(String resourceId) {		
		if (resourceId == null || resourceId.isEmpty()) {
			throw new IllegalArgumentException("The resourceId must not be null or empty.");
		}
		
		Map<String, String> resourceInfo;
		try {
			resourceInfo = getResourceInfo(resourceId);
			if (resourceInfo.get(RequestAttribute.STATE.getValue()) != null
					&& resourceInfo.get(RequestAttribute.STATE.getValue()).equals(
							RequestState.FULFILLED.getValue())) {
				return true;
			}
		} catch (InfrastructureException e) {
			LOGGER.error("Exception while getting resource info from resourceId=" +resourceId, e);
		}
		return false;
	}

	@Override
	public void deleteResource(String id) throws InfrastructureException {
		LOGGER.info("Deleting resource id=" + id);
		checkConfigured();
		checkValidId(id);

		Map<String, String> requestInfo = getResourceInfo(id);

		if (requestInfo.get(RequestAttribute.STATE.getValue()).equals(
				RequestState.FULFILLED.getValue())) {
			String instanceId = requestInfo.get(RequestAttribute.INSTANCE_ID.getValue());

			// delete the instance
			if (instanceId != null && !"null".equals(instanceId) && !instanceId.isEmpty()) {
				try {
					doRequest("delete", fogbowEndpoint + "/compute/" + instanceId, authToken,
							new HashSet<Header>());
				} catch (URISyntaxException | HttpException | IOException e) {
					LOGGER.warn("Exception while deleting the instance " + instanceId + ".", e);
				}
			}
		}

		// delete the request permanently
		try {
			doRequest("delete", fogbowEndpoint + "/" + RequestConstants.TERM + "/" + id, authToken,
					new HashSet<Header>());
		} catch (URISyntaxException | HttpException | IOException e) {
			LOGGER.error("Exception while deleting the requestId " + id + ".", e);
			throw new InfrastructureException(e.getMessage());
		}
	}

	private String doRequest(String method, String endpoint, String authToken,
			Set<Header> additionalHeaders) throws URISyntaxException, HttpException, IOException,
			InfrastructureException {
		HttpUriRequest request = null;
		if (method.equals("get")) {
			request = new HttpGet(endpoint);
		} else if (method.equals("delete")) {
			request = new HttpDelete(endpoint);
		} else if (method.equals("post")) {
			request = new HttpPost(endpoint);
		}
		request.addHeader(FogbowContants.CONTENT_TYPE, FogbowContants.OCCI_CONTENT_TYPE);
		if (authToken != null) {
			request.addHeader(FogbowContants.X_AUTH_TOKEN, authToken);
		}
		for (Header header : additionalHeaders) {
			request.addHeader(header);
		}

		if (client == null) {
			client = new DefaultHttpClient();
			HttpParams params = new BasicHttpParams();
			params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			client = new DefaultHttpClient(new ThreadSafeClientConnManager(params, client
					.getConnectionManager().getSchemeRegistry()), params);
		}

		HttpResponse response = client.execute(request);

		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
			|| response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
			
			Header locationHeader = getLocationHeader(response.getAllHeaders());
			if (locationHeader != null && locationHeader.getValue().contains(RequestConstants.TERM)) {
				return generateLocationHeaderResponse(locationHeader);
			} else {
				return EntityUtils.toString(response.getEntity());
			}		
		} else {
			throw new InfrastructureException(response.getStatusLine().toString());
		}
	}
	
	protected static Header getLocationHeader(Header[] headers) {
		for (Header header : headers) {	
			if (header.getName().equals("Location")) {
				return header;
			}
		}
		return null;
	}
	
	protected static String generateLocationHeaderResponse(Header header) {
		String[] locations = header.getValue().split(",");
		String response = "";
		for (String location : locations) {
			response += HeaderUtils.X_OCCI_LOCATION_PREFIX + location + "\n";
		}
		return response.trim();
	}

	/**
	 * This method was created to be used just for testing
	 * 
	 * @param fogbowEndpoint
	 */
	protected void setFogbowEndpoint(String fogbowEndpoint) {
		this.fogbowEndpoint = fogbowEndpoint;

	}

	/**
	 * This method was created to be used just for testing
	 * 
	 * @param authToken
	 */
	protected void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	@Override
	public CommandResult executeCommand(Map<String, String> resourceInfo,
			Map<String, String> resourceCredentials, String command) throws InfrastructureException {
		if (resourceInfo == null || resourceInfo.get(Instance.SSH_PUBLIC_ADDRESS_ATT) == null) {
			throw new InfrastructureException(
					"The resource does not have available public address to be accessed.");
		}
		String publicAddress = resourceInfo.get(Instance.SSH_PUBLIC_ADDRESS_ATT);
		String[] tokens = publicAddress.split(":");
		String address;
		int port;
		try {
			address = tokens[tokens.length - 2];
			port = Integer.parseInt(tokens[tokens.length - 1]);
		} catch (Exception e) {
			LOGGER.error("Exceprion while getting the address and port of resource.", e);
			throw new InfrastructureException("The public address " + publicAddress
					+ " provided by resource is invalid.");
		}

		if (resourceCredentials == null) {
			throw new InfrastructureException("Invalid resouce credentials.");
		}
		String username = resourceCredentials.get("username");
		String privateKeyFilePath = resourceCredentials.get("private_key_file_path");
		String userPassword = resourceCredentials.get("user_password");
		if (username == null || (privateKeyFilePath == null || userPassword == null)) {
			throw new InfrastructureException(
					"Invalid resouce credentials. It is nedded at least username and (private key file path or user password)");
		}
		if (privateKeyFilePath != null) {
			return SSHUtils.doSshWithPrivateKey(address, port, command, username, privateKeyFilePath);
		}
		return SSHUtils.doSshWithPassword(address, port, command, username, userPassword);
	}

}
