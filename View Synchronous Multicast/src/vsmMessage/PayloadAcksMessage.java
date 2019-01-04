package vsmMessage;

import java.util.HashSet;

public class PayloadAcksMessage extends PayloadMessage {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4019400696071541129L;
	
	private HashSet<Integer> ackIds = new HashSet<Integer>();

	public PayloadAcksMessage(int viewId, int senderId, int seqN, String payload, HashSet<Integer> ackIds) {
		super(viewId, senderId, seqN, payload);
		this.ackIds = ackIds;
	}

	public HashSet<Integer> getAckIds() {
		return ackIds;
	}

	public void setAckIds(HashSet<Integer> ackIds) {
		this.ackIds = ackIds;
	}
}
