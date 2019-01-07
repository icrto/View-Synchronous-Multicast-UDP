package vsmMessage;

public class AckMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4884149991447793032L;
	
	private int ackSenderId = -1;
	private int ackSeqN = -1;
	
	public AckMessage(int viewId, int senderId, int ackSenderId, int ackSeqN) {
		super(viewId, senderId);
		this.ackSenderId = ackSenderId;
		this.ackSeqN = ackSeqN;
	}

	public int getAckSenderId() {
		return ackSenderId;
	}

	public void setAckSenderId(int ackSenderId) {
		this.ackSenderId = ackSenderId;
	}

	public int getAckSeqN() {
		return ackSeqN;
	}

	public void setAckSeqN(int ackSeqN) {
		this.ackSeqN = ackSeqN;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ackSenderId;
		result = prime * result + ackSeqN;
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
		AckMessage other = (AckMessage) obj;
		if (ackSenderId != other.ackSenderId)
			return false;
		if (ackSeqN != other.ackSeqN)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "PayloadMessage [viewID= " + super.getViewId() + " senderID=" 
				+ super.getSenderId() + " ackedMsgSeqN=" + ackSeqN + " ackedMsgSenderID=" + ackSenderId + "]";
	}

}
