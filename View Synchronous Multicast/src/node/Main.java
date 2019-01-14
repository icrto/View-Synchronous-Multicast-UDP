package node;

import java.io.IOException;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws IOException {
		// ARGS: 0->id  1->ip_group  2->port  3->dropRate 4->avgDelay  5->stdDelay
		
		if(args.length < 7) {
			System.out.println("Usage: java -jar node.jar <nNodes> <ID> <IPMulticast> <port> <dropRate> <avgDelay> <stdDelay>"
					+ " [<filePath> <nrStableMsgs> <nrNonStableMsgs>]");
			System.exit(-1);
		}
	
		System.setProperty("java.net.preferIPv4Stack" , "true");

		int nNodes = Integer.parseInt(args[0]);
		int ID = Integer.parseInt(args[1]);
		String IPmulticast = args[2];
		int port = Integer.parseInt(args[3]);
		int timeout = 500; // TODO: change to parse from args[] (millis)
		double dropRate = Double.parseDouble(args[4]);
		double avgDelay = Double.parseDouble(args[5]);
		double stdDelay = Double.parseDouble(args[6]);
		
		
		if(validIP(IPmulticast) != true) {
			System.out.println("IP isn't valid!");
			System.exit(-1);
		}
		
		if((ID < 0 ) || (port < 0) || (dropRate < 0) || (avgDelay < 0) || (stdDelay < 0) ) {
			System.out.println("Use positive numbers!");
			System.exit(-1);
		}
		
		VSM vsm;
		
		if(args.length > 7) {
			String filePath = args[7];
			int nrStableMsgs = Integer.parseInt(args[8]);
			int nrNonStableMsgs = Integer.parseInt(args[9]);
			
			vsm = new VSM(nNodes, ID, IPmulticast, port, timeout, dropRate, avgDelay, stdDelay, filePath, nrStableMsgs, nrNonStableMsgs);
		}
		else {
			vsm = new VSM(nNodes, ID, IPmulticast, port, timeout, dropRate, avgDelay, stdDelay);
		}

			
		
		vsm.start();

		Receive rcv = new Receive(vsm);
		
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);

		 
		 while(rcv.receiver.isAlive()) 
		 {
			 if(scanner.hasNext()) {
					String read = scanner.nextLine();
					//	System.out.println("Sending " + new String(hi.getData(), hi.getOffset(), hi.getLength()));
					vsm.sendVSM(read);
				}
		 }
		 System.out.println("One or two Threads run is over" );


	}
	
	public static boolean validIP (String ip) {
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	
}


