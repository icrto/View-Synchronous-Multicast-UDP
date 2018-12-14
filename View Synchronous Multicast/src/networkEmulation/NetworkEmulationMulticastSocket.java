package networkEmulation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class NetworkEmulationMulticastSocket extends MulticastSocket {
	
	double dropRate = 0;
	double avgDelay = 0;
	double stdDelay = 0;

	public NetworkEmulationMulticastSocket(int port, double dropRate, double avgDelay, double stdDelay) throws IOException {
		super(port);
		this.dropRate = dropRate;
		this.avgDelay = avgDelay;
		this.stdDelay = stdDelay;
	}
	
	public void receive(DatagramPacket p) throws IOException {
		while(true) {
			super.receive(p);
			if(!lost()) {
				try {
					Thread.sleep(delayMillis());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
		}
	}
	
	public void send(DatagramPacket p) throws IOException {
		super.send(p);
	}

	private boolean lost() {
		// TODO: return true with droprate probability
		return false;
	}
	
	private int delayMillis() {
		// TODO: return delay using normal distribution
		return 0;
	}
	

}
