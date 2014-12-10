package org.fogbowcloud.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.ConfigurationConstants;
import org.fogbowcloud.infrastructure.core.InfrastructureException;
import org.fogbowcloud.infrastructure.core.InfrastructureProvider;
import org.fogbowcloud.infrastructure.fogbow.FogbowContants;
import org.fogbowcloud.infrastructure.fogbow.FogbowInfrastructureProvider;

public class MainTest {

	public static void main(String[] args) {
		
		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put(FogbowContants.PLUGIN_TYPE_KEY, "openstack");
//		credentials.put("authUrl", "http://150.165.15.107:5000");
//		credentials.put("username", "fogbow");
//		credentials.put("password", "nc3SRPS2");
//		credentials.put("tenantName", "fogbow");

//		credentials.put("authUrl", "http://150.165.15.12:5000");
//		credentials.put("username", "fogbow");
//		credentials.put("password", "nc3SRPS2");
//		credentials.put("tenantName", "fogbow-project");
		
		credentials.put("authUrl", "http://150.165.15.14:5000");
		credentials.put("username", "fogbow");
		credentials.put("password", "nc3SRPS2");
		credentials.put("tenantName", "fogbow-project");
		
//		credentials.put("authUrl", "http://150.165.15.81:5000");
//		credentials.put("username", "admin");
//		credentials.put("password", "labstack");
//		credentials.put("tenantName", "demo");
		
		Properties p = new Properties();
//		p.put(ConfigurationConstants.INFRA_ENDPOINT, "http://150.165.15.107:8182");
		p.put(ConfigurationConstants.INFRA_ENDPOINT, "http://150.165.15.14:8182");
//		p.put(ConfigurationConstants.INFRA_ENDPOINT, "http://150.165.15.81:8182");
		
		InfrastructureProvider infrastructure = new FogbowInfrastructureProvider(p);
//
		try {
//			System.out.println(infrastructure.configure("http://150.165.15.107:8182", credentials));
			System.out.println(infrastructure.configure(credentials));
//			System.out.println(infrastructure.configure("http://150.165.15.81:8182", credentials));
		} catch (InfrastructureException e) {
			System.out.println(e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		Map<String, String> properties = new HashMap<String, String>();
//		
//		properties.put(FogbowContants.TYPE_KEY, "persistent");
//		properties.put(FogbowContants.FLAVOR_KEY, "fogbow_small");
//		properties.put(FogbowContants.IMAGE_KEY, "fogbow-linux-x86");
////		
//		try {
//			System.out.println(infrastructure.createResource(1, properties));
//		} catch (InfrastructureException e) {
//			System.out.println(e.getMessage());
//			e.printStackTrace();
//		}
//		
//		String id = "ef3ca120-745e-4b81-8628-049aa2fec1e6";
//		try {
//			System.out.println(infrastructure.getResourceInfo(id));
//		} catch (InfrastructureException e) {
//			// TODO Auto-generated catch block
//			System.out.println(e.getMessage());
//			e.printStackTrace();
//		}
	
//		System.out.println(SSHUtils.doSshWithPrivateKey("150.165.15.107", 10011, "ls -lh", "cirros", "C:/Users/cmdadmin/Documents/giovanni/keys/id_rsa.fogmember.txt" ));
//		System.out.println(SSHUtils.doSshWithPassword("150.165.15.107", 10011, "ls -lh", "cirros", "cubswin:)" ));
		
//		List<String> names = new ArrayList<String>();
//		names.add("gil");
//		names.add("dani");
//		names.add("marinha");
//		
//		System.out.println("names1: " + names);
//		Test test = new Test(names);
//		System.out.println("names2: " + test.getNames());
//		names.add("lucia");
//		
//		System.out.println("names1: " + names);
//		System.out.println("names2: " + test.getNames());
//		
//		
//		System.out.println(Math.ceil(10/3));
//		System.out.println(Math.ceil(10/4));
//		System.out.println((int)Math.ceil(10/3));
	}
	
	static class Test {
		
		List<String> names;
		
		public Test(List<String> names){
			this.names = names;
		}
		
		public List<String> getNames(){
			return names;
		}
	}

}
