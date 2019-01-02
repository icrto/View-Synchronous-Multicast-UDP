package vsmMessage;

import java.io.Serializable;

public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2668866006290649765L;
	
	public static final int PAYLOAD_MESSAGE = 1;
	public static final int ACK_MESSAGE = 2;
	
	private int viewId = -1;
	private int messageType = -1;
	private int senderId = -1;
	
	public Message(int viewId, int messageType, int senderId) {
		super();
		this.setViewId(viewId);
		this.messageType = messageType;
		this.senderId = senderId;
	}

	public int getViewId() {
		return viewId;
	}

	public void setViewId(int viewId) {
		this.viewId = viewId;
	}
	
	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}

}
