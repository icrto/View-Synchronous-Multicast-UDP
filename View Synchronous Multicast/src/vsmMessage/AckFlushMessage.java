package vsmMessage;

public class AckFlushMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 438253091022300489L;

	private int ackSenderId = -1;

	public AckFlushMessage(int viewId, int senderId, int ackSenderId) {
		super(viewId, senderId);
		this.ackSenderId = ackSenderId;
	}

	public int getAckSenderId() {
		return ackSenderId;
	}

	public void setAckSenderId(int ackSenderId) {
		this.ackSenderId = ackSenderId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ackSenderId;
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
		AckFlushMessage other = (AckFlushMessage) obj;
		if (ackSenderId != other.ackSenderId)
			return false;
		return true;
	}
}
