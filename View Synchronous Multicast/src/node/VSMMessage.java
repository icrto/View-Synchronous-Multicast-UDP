package node;

import java.io.Serializable;

public class VSMMessage implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1295987829600477791L;
	
	private int viewId;
	
	private int senderId;
	
	private int seqN;
	
	private String payload;

	public VSMMessage(int viewId, int senderId, int seqN, String payload) {
		super();
		this.viewId = viewId;
		this.senderId = senderId;
		this.seqN = seqN;
		this.payload = payload;
	}

	public int getViewId() {
		return viewId;
	}

	public void setViewId(int viewId) {
		this.viewId = viewId;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
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
