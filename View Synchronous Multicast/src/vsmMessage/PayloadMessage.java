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

}
