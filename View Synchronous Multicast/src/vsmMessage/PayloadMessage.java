package vsmMessage;

public class PayloadMessage extends Message{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8037060419734837110L;
	
	private int seqN = -1;	
	private String payload = null;

	public PayloadMessage(int viewId, int senderId, int seqN, String payload) {
		super(viewId, Message.PAYLOAD_MESSAGE, senderId);
		this.seqN = seqN;
		this.payload = payload;
	}

	public int getSeqN() {
		return seqN;
	}

	public void setSeqN(int seqN) {
		this.seqN = seqN;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		result = prime * result + seqN;
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
		PayloadMessage other = (PayloadMessage) obj;
		if (payload == null) {
			if (other.payload != null)
				return false;
		} else if (!payload.equals(other.payload))
			return false;
		if (seqN != other.seqN)
			return false;
		return true;
	}

}
