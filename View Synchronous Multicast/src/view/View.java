package view;
import java.io.Serializable;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class View implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int id = 0;
	private Set<Integer> nodes;
	public View() throws SocketException, UnknownHostException {
		this.nodes = new HashSet<Integer>();
	}
	public int getID() {
		return id;
	}
	public Set<Integer> getNodes() {
		return nodes;
	}
	public void setNodes(Set<Integer> nodes) {
		this.nodes = nodes;
	}
	public boolean join(int node) {
		return this.nodes.add(node);
	}
	public boolean leave(int node) {
		return this.nodes.remove(node);
	}
	public void changeViewID() {
		id++;
	}
	@Override
	public String toString() {
		return "View [id = " + id + ", nodes = " + nodes + "]";
	}
	@Override
	public boolean equals(Object obj) {
		View v2 = (View)obj;
		if((id == v2.getID()) && nodes.equals(v2.getNodes())) {
			return true;
		}
		else return false;
	}


}
