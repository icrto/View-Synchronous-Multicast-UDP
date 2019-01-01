package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class Receive implements Runnable{
	
	Thread receiver;
	MulticastSocket s;
	
	Receive(MulticastSocket s){
		receiver = new Thread(this, "my runnable thread");
		this.s = s;
	    System.out.println("my receiver created" + receiver);
	    receiver.start();
	}

	@Override
	public void run() {
		try
		{
			 byte[] buf = new byte[1000];
			 while(true) {
				 DatagramPacket recv = new DatagramPacket(buf, buf.length);
				 s.receive(recv);
				 System.out.println("Recebi: " + new String(recv.getData(), recv.getOffset(), recv.getLength()));	
			 }

	     }
		 catch (IOException e) {
	    	 	System.out.println("receive interrupted");
			e.printStackTrace();
		}
	     System.out.println("receive run is over" );
	   }
		

}
