package membershipService;

public class SuspectMessage {
	private int nodeID;
	private int suspectedNodeID;

	public SuspectMessage(int nodeID, int suspectedNodeID) {
		this.nodeID = nodeID;
		this.suspectedNodeID = suspectedNodeID;
	}

	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}

	public int getSuspectedNodeID() {
		return suspectedNodeID;
	}

	public void setSuspectedNodeID(int suspectedNodeID) {
		this.suspectedNodeID = suspectedNodeID;
	}

}
