package org.fogbowcloud.infrastructure.fogbow;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Random;

import org.restlet.Component;
import org.restlet.data.Protocol;

public class FogbowTestHelper {

	public static final String FOGBOW_ADDRESS = "http://localhost";
	private final int PORT_ENDPOINT = getAvailablePort();
	private final String DEFAULT_USERNAME = "username";
	private final String DEFAULT_AUTH_TOKEN = "tokenid";
	private final String FOGBOW_ENDPOINT = FOGBOW_ADDRESS + ":" + PORT_ENDPOINT;
	
	private Component component;
	FogbowApplication fogbowApp;

	/**
	 * Getting a available port on range 60000:61000
	 * 
	 * @return
	 */
	public static int getAvailablePort() {
		int initialP = 60000;
		int finalP = 61000;
		for (int i = initialP; i < finalP; i++) {
			int port = new Random().nextInt(finalP - initialP) + initialP;
			ServerSocket ss = null;
			DatagramSocket ds = null;
			try {
				ss = new ServerSocket(port);
				ss.setReuseAddress(true);
				ds = new DatagramSocket(port);
				ds.setReuseAddress(true);
				return port;
			} catch (IOException e) {
			} finally {
				if (ds != null) {
					ds.close();
				}
				if (ss != null) {
					try {
						ss.close();
					} catch (IOException e) {
						/* should not be thrown */
					}
				}
			}
		}
		return -1;
	}

	public void initializeFogbowComponent(String[] expectedIds) throws Exception {
		this.component = new Component();
		this.component.getServers().add(Protocol.HTTP, PORT_ENDPOINT);

		fogbowApp = new FogbowApplication(FOGBOW_ENDPOINT, expectedIds);
		fogbowApp.addTokenToUser(DEFAULT_AUTH_TOKEN, DEFAULT_USERNAME);

		this.component.getDefaultHost().attach(fogbowApp);
		this.component.start();
	}

	public void disconnectFogbowComponent() throws Exception {
		this.component.stop();		
	}

	public String getFogbowEndepoint() {
		return FOGBOW_ENDPOINT;
	}

	public String getDefaultToken() {
		return DEFAULT_AUTH_TOKEN;
	}
	
	public FogbowApplication getFogbowApplication(){
		return fogbowApp;
	}
}
