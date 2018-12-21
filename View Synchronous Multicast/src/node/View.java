package node;

import java.util.ArrayList;

public class View {
	
	private int id;
	
	private ArrayList<Integer> nodes;

	public View(int id, ArrayList<Integer> nodes) {
		super();
		this.id = id;
		this.nodes = nodes;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ArrayList<Integer> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<Integer> nodes) {
		this.nodes = nodes;
	}
	
	
	// Adicionar equals
	
}
