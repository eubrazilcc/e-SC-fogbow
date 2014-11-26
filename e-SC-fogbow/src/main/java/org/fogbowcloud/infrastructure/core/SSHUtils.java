package org.fogbowcloud.infrastructure.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import org.apache.log4j.Logger;

public class SSHUtils {

	private static final Logger LOGGER = Logger.getLogger(SSHUtils.class);

	public static CommandResult doSshWithPassword(String address, int port, String command,
			String username, String password) {
		LOGGER.info("Doing ssh to address=" + address + ", port=" + port + " to run command="
				+ command + " authenticating by username=" + username + ", password=" + password);
		return doSSH(address, port, command, username, password, true);
	}

	private static CommandResult doSSH(String address, int port, String command, String username,
			String authentication, boolean usingPassword) {
		SSHClient ssh = new SSHClient();
		Session session = null;
		addBlankHostKeyVerifier(ssh);
		try {
			ssh.connect(address, port);
			if (usingPassword) {
				ssh.authPassword(username, authentication);
			} else {
				// if it doesn't using password, the authentication is key file
				// path
				ssh.authPublickey(username, authentication);
			}
			session = ssh.startSession();
			Command cmd = session.exec(command);
			cmd.join();
			int cmdExitStatus = cmd.getExitStatus();
			String cmdOutput = readInputStream(cmd.getInputStream());
			String cmdErrorMessage = readInputStream(cmd.getErrorStream());
			return new CommandResult(cmdExitStatus, cmdOutput, cmdErrorMessage);
		} catch (Exception e) {
			LOGGER.error("Exception while doing ssh.", e);
			return null;
		} finally {
			try {
				if (session != null) {
					session.close();
				}
				ssh.disconnect();
				ssh.close();
			} catch (Throwable e) {
			}
		}
	}

	public static CommandResult doSshWithPrivateKey(String address, int port, String command,
			String username, String privateKeyFilePath) {
		LOGGER.info("Doing ssh to address=" + address + ", port=" + port + " to run command="
				+ command + " authenticating by username=" + username + ", privateKeyFilePath="
				+ privateKeyFilePath);
		return doSSH(address, port, command, username, privateKeyFilePath, false);
	}

	private static String readInputStream(InputStream inputStream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		String answer = sb.toString();
		return answer;
	}

	private static void addBlankHostKeyVerifier(SSHClient ssh) {
		ssh.addHostKeyVerifier(new HostKeyVerifier() {
			@Override
			public boolean verify(String arg0, int arg1, PublicKey arg2) {
				return true;
			}
		});
	}
}
