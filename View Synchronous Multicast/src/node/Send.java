package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

public class Send implements Runnable{

	Thread sender;
	MulticastSocket s;
	InetAddress group;
	Scanner scanner;
	
	Send(InetAddress group,MulticastSocket s ){
		sender = new Thread(this, "my runnable thread");
		this.group = group;
		this.s = s;
	    System.out.println("my sender created " + sender);
	    scanner = new Scanner(System.in);
	    
	    sender.start();
	}
	
	
	@Override
	public void run() {
		
		try {
			while(true) {
				String read = scanner.next();
				DatagramPacket hi = new DatagramPacket(read.getBytes(), read.length(), group, 6789);
			//	System.out.println("Sending " + new String(hi.getData(), hi.getOffset(), hi.getLength()));
				s.send(hi);
			}
		} catch (IOException e) {
    	 		System.out.println("my thread interrupted");
			e.printStackTrace();
		}
		System.out.println("mythread run is over" );
	
	}

}
