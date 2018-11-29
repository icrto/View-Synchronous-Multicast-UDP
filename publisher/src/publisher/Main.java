package publisher;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.setProperty("java.net.preferIPv4Stack" , "true");
		String msg = "Hello";
		InetAddress group = InetAddress.getByName("228.0.0.4");
		@SuppressWarnings("resource")
		MulticastSocket s = new MulticastSocket(6789);
		s.joinGroup(group);
		DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
		while(true) {
			System.out.println("Sending " + new String(hi.getData(), hi.getOffset(), hi.getLength()));
			s.send(hi);
		}
	}
}
