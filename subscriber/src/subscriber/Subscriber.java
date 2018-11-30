package subscriber;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class Subscriber {
	
	public static void main(String[] args) throws IOException {
		 // join a Multicast group and send the group salutations
		
		System.setProperty("java.net.preferIPv4Stack" , "true");
	
		 InetAddress group = InetAddress.getByName("228.0.0.4");  //228.5.6.7
		 MulticastSocket s = new MulticastSocket(6789);
		 s.joinGroup(group);
		 
		// String msg = "Hello";
		// DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(),group, 6789);
		// s.send(hi);


		 // get their responses!
		 while(true) {
			 byte[] buf = new byte[1000];
			 DatagramPacket recv = new DatagramPacket(buf, buf.length);
			 s.receive(recv);
			 System.out.println(new String(recv.getData(), recv.getOffset(), recv.getLength()));	 
		 }
		
		 // OK, I'm done talking - leave the group...
		// s.leaveGroup(group);
	}

}
