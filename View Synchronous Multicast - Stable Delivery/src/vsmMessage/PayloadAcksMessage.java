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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((ackIds == null) ? 0 : ackIds.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PayloadAcksMessage other = (PayloadAcksMessage) obj;
		if (ackIds == null) {
			if (other.ackIds != null)
				return false;
		} else if (!ackIds.equals(other.ackIds))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PayloadAcksMessage [viewID=" + super.getViewId() + " originalSenderID=" + super.getSenderId() + " originalSeqN=" + super.getSeqN() + " ackIds=" + ackIds + "]";
	}

}
