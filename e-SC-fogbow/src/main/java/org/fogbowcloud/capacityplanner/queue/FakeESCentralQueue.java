package org.fogbowcloud.capacityplanner.queue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * This class is a fake Queue of Jobs from a e-Science Central installation. It
 * reads information about the queue from a specified file.
 * 
 * @author giovanni
 *
 */
public class FakeESCentralQueue implements EScienceCentralQueue {

	public static final String FAKE_QUEUE_FILE_PATH_KEY = "fake_queue_file_path";
	
	private String filePath;
	
	private static final Logger LOGGER = Logger.getLogger(FakeESCentralQueue.class);

	public FakeESCentralQueue(Properties properties) {
		this(properties.getProperty(FAKE_QUEUE_FILE_PATH_KEY));
	}
	
	public FakeESCentralQueue(String filePath) {
		LOGGER.debug("Creating FakeESCentral with filePath=" + filePath);
		if (filePath == null || filePath.isEmpty() || !new File(filePath).exists()) {
			throw new IllegalArgumentException("The filePath specified is invalid.");
		}
		this.filePath = filePath;
	}

	@Override
	public int getLength() {
		try {
			return Integer.parseInt(getContentFile(filePath));
		} catch (Exception e) {
			LOGGER.error("Exception while getting queue length from filePath=" + filePath, e);
			return -1;
		}
	}

	private String getContentFile(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString().trim();
		} finally {
			br.close();
		}
	}
}
