package se.sics.kompics.p2p.bootstrap.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>CacheAddPeerRequest</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class CacheAddPeerRequest extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8229128521077358811L;

	private final Address peerAddress;

	public CacheAddPeerRequest(Address peerAddress) {
		this.peerAddress = peerAddress;
	}

	public Address getPeerAddress() {
		return peerAddress;
	}
}
