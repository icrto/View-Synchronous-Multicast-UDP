package node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Main {
	
	

	public static void main(String[] args) throws IOException {
		
		System.setProperty("java.net.preferIPv4Stack" , "true");
		
		 InetAddress group = InetAddress.getByName("228.0.0.4");  //228.5.6.7
		 MulticastSocket s = new MulticastSocket(6789);
		 s.joinGroup(group);
		
		 Receive rcv = new Receive(s);
		 Send snd = new Send(group, s);
		 
		 try
		 {
			 while(rcv.receiver.isAlive())
			 {
				// System.out.println("Receiver is Alive"); 
				 Thread.sleep(1500);
			 }
		 }
		 catch(InterruptedException e)
		 {
			 System.out.println("Receiver interrupted");
		 }
		 System.out.println("Receiver run is over" );

		 try
		 {
			 while(snd.sender.isAlive())
			 {
				// System.out.println("Sender is Alive"); 
				 Thread.sleep(1500);
			 }
		 }
		 catch(InterruptedException e)
		 {
			 System.out.println("Sender interrupted");
		 }
		 System.out.println("Sender run is over" );
		 
		 
	       
	       
		
		
	}

}
