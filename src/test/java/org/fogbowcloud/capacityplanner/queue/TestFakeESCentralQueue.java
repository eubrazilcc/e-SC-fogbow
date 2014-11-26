package org.fogbowcloud.capacityplanner.queue;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class TestFakeESCentralQueue {

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyPath() {
		new FakeESCentralQueue("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoExistingFile() {
		new FakeESCentralQueue("no/valid/path");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyPathByProperties() {
		Properties properties = new Properties();
		properties.put(FakeESCentralQueue.FAKE_QUEUE_FILE_PATH_KEY, "");
		new FakeESCentralQueue(properties);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoExistingPathByProperties() {
		Properties properties = new Properties();
		properties.put(FakeESCentralQueue.FAKE_QUEUE_FILE_PATH_KEY, "no/valid/path");
		new FakeESCentralQueue(properties);
	}

	@Test
	public void testValidPathAndValidLength() {
		FakeESCentralQueue queue = new FakeESCentralQueue("src/test/resources/queue/valid.length");
		Assert.assertEquals(10, queue.getLength());
	}

	@Test
	public void testValidPathAndValidLengthByProperties() {
		Properties properties = new Properties();
		properties.put(FakeESCentralQueue.FAKE_QUEUE_FILE_PATH_KEY,
				"src/test/resources/queue/valid.length");
		FakeESCentralQueue queue = new FakeESCentralQueue(properties);
		Assert.assertEquals(10, queue.getLength());
	}

	@Test
	public void testValidPathAndInvalidLength() {
		FakeESCentralQueue queue = new FakeESCentralQueue("src/test/resources/queue/invalid.length");
		Assert.assertEquals(-1, queue.getLength());
	}

	@Test
	public void testValidPathAndInvalidLengthByProperties() {
		Properties properties = new Properties();
		properties.put(FakeESCentralQueue.FAKE_QUEUE_FILE_PATH_KEY,
				"src/test/resources/queue/invalid.length");
		FakeESCentralQueue queue = new FakeESCentralQueue(properties);
		Assert.assertEquals(-1, queue.getLength());
	}

}
