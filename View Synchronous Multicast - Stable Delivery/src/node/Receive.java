package node;

public class Receive implements Runnable{

	Thread receiver;
	VSMLD vsm;

	Receive(VSMLD vsm){
		receiver = new Thread(this, "my runnable thread");
		this.vsm = vsm;
		receiver.start();
	}

	@Override
	public void run() {

		while(true) {  // Para ficar aqui preso sempre a receber 
			
			System.out.println("Delivered: " + vsm.recvVSM());	
		}
	}


}
