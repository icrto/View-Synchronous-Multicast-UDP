package node;

public class Main {

	public static void main(String[] args) {
		// ARGS: 0->id  1->ip_group  2->port  3->dropRate 4->avgDelay  5->stdDelay
		
		if(args.length != 6) {
			System.out.println("Usage: java -jar node.jar <ID> <IPMulticast> <port> <dropRate> <avgDelay> <stdDelay>");
			System.exit(-1);
		}
	
		System.setProperty("java.net.preferIPv4Stack" , "true");

		VSM vsm = new VSM(args[0], args[1], args[2], args[3], args[4], args[5]);
		vsm.start();

		Receive rcv = new Receive(vsm);
		Send snd = new Send(vsm);
		 
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
