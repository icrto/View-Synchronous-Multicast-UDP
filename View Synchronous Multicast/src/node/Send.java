package node;

import java.io.IOException;
import java.util.Scanner;

public class Send implements Runnable{

	Thread sender;
	VSM vsm;
	Scanner scanner;
	
	Send(VSM vsm){
		sender = new Thread(this, "my runnable thread");
		this.vsm = vsm;
		
	    scanner = new Scanner(System.in);
	    
	    sender.start();
	}
	
	@Override
	public void run() {
		
		try {
			while(true) {  // Para ficar aqui preso sempre pronto a enviar 
				String read = scanner.next();
			//	System.out.println("Sending " + new String(hi.getData(), hi.getOffset(), hi.getLength()));
				vsm.sendVSM(read);
			}
		} catch (IOException e) {
    	 		System.out.println("my thread interrupted");
			e.printStackTrace();
		}
		System.out.println("mythread run is over" );
	
	}

}
