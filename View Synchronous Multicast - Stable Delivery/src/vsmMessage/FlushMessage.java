package vsmMessage;

import java.util.HashSet;

import util.Tuple;

public class FlushMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4319923073690206255L;

	private HashSet<Tuple<Integer, Integer>> stableMsgsIDs = new HashSet<Tuple<Integer, Integer>>();

	public FlushMessage(int viewId, int senderId, HashSet<Tuple<Integer, Integer>> stableMsgsIDs) {
		super(viewId, senderId);
		this.stableMsgsIDs = stableMsgsIDs;
	}

	public HashSet<Tuple<Integer, Integer>> getStableMsgsIDs() {
		return stableMsgsIDs;
	}

	public void setStableMsgsIDs(HashSet<Tuple<Integer, Integer>> stableMsgsIDs) {
		this.stableMsgsIDs = stableMsgsIDs;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((stableMsgsIDs == null) ? 0 : stableMsgsIDs.hashCode());
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
		FlushMessage other = (FlushMessage) obj;
		if (stableMsgsIDs == null) {
			if (other.stableMsgsIDs != null)
				return false;
		} else if (!stableMsgsIDs.equals(other.stableMsgsIDs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FlushMessage [viewID=" + super.getViewId() + " senderID=" + super.getSenderId() + " stableMsgsIDs=" + stableMsgsIDs + "]";
	}

}
