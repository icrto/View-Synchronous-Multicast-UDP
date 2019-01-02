package node;

import java.io.IOException;

//import networkEmulation.NetworkEmulationMulticastSocket;

public class Main {

	public static void main(String[] args) throws IOException {
		// ARGS: 0->id  1->ip_group  2->socket  3->dropRate 4->avgDelay  5->stdDelay
	
		System.setProperty("java.net.preferIPv4Stack" , "true");
		
		if(args.length != 6) {
			System.out.println("Number of Args diff of 6!");
			System.exit(-1);
		}
		
		Node node = new Node(args[0], args[1], args[2]);
		//NetworkEmulationMulticastSocket emul = new NetworkEmulationMulticastSocket(args[2], args[3], args[4], args[5]);

		Receive rcv = new Receive(node.getS());
		Send snd = new Send(node.getGroup(), node.getS());
		 
		 try
		 {
			 while(rcv.receiver.isAlive() || snd.sender.isAlive())
			 {
				// System.out.println("Receiver is Alive"); 
				 Thread.sleep(1500);
			 }
		 }
		 catch(InterruptedException e)
		 {
			 System.out.println("One or two Threads interrupted");
		 }
		 System.out.println("One or two Threads run is over" );


	}
}
