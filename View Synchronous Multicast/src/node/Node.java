package node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Node {
	
	int id;
	String ip_group;
	int socket;
	MulticastSocket s;
	InetAddress group;
	
	
	public Node(String id, String ip_group, String socket)  throws IOException  {
		super();
		this.id = Integer.parseInt(id);
		this.ip_group = ip_group;
		this.socket = Integer.parseInt(socket);
		
		this.group = InetAddress.getByName(this.ip_group);  //228.5.6.7
		this.s = new MulticastSocket(this.socket);
		s.joinGroup(group);
		
	}


	public InetAddress getGroup() {
		return group;
	}


	public void setGroup(InetAddress group) {
		this.group = group;
	}


	public MulticastSocket getS() {
		return s;
	}


	public void setS(MulticastSocket s) {
		this.s = s;
	}
	
	



}
