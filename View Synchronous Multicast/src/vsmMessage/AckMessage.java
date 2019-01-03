package vsmMessage;

public class AckMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4884149991447793032L;
	
	private int ackSenderId = -1;
	private int ackSeqN = -1;
	
	public AckMessage(int viewId, int messageType, int senderId, int ackSenderId, int ackSeqN) {
		super(viewId, messageType, senderId);
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

}
