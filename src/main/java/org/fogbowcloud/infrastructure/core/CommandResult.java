package org.fogbowcloud.infrastructure.core;

/**
 * This class represents an command result executed inside some resource.
 * 
 * @author giovanni
 *
 */
public class CommandResult {

	private int exitStatus;
	private String output;
	private String errorMessage;

	public CommandResult(int exitStatus, String output, String errorMessage) {
		this.exitStatus = exitStatus;
		this.output = output;
		this.errorMessage = errorMessage;
	}

	public int getExitStatus() {
		return exitStatus;
	}

	public String getOutput() {
		return output;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String toString() {
		return "exitStatus=" + exitStatus + ";\noutput=" + output + ";\nerrorMessage="
				+ errorMessage;
	}
}
