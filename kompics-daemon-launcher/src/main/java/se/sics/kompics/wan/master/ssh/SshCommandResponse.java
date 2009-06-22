package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Response;
import ch.ethz.ssh2.Session;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshCommandResponse extends Response {

	private final String commandResponse;

	private final int sessionId;

	private final boolean status;

	public SshCommandResponse(SshCommandRequest request, int sessionId,
			String commandResponse, boolean status) {
		super(request);
		this.commandResponse = commandResponse;
		this.sessionId = sessionId;
		this.status = status;
	}

	/**
	 * @return the command
	 */
	public String getCommandResponse() {
		return commandResponse;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}

	/**
	 * @return the stopOnError
	 */
	public boolean isStatus() {
		return status;
	}
}
