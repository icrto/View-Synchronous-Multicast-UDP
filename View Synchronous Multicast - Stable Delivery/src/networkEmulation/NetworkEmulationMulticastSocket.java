package networkEmulation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Random;


public class NetworkEmulationMulticastSocket extends MulticastSocket {
	
	double dropRate = 0;
	double avgDelay = 0;
	double stdDelay = 0;

	public NetworkEmulationMulticastSocket(int port, double dropRate2, double avgDelay2, double stdDelay2) throws IOException {
		super(port);
		this.dropRate = dropRate2;
		this.avgDelay = avgDelay2;
		this.stdDelay = stdDelay2;
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
		//return true with droprate probability
		double rand_lost = Math.random() * this.dropRate;  //Criar um valor aleatorio entre [0;this.dropRate]
		
		if(rand_lost < this.dropRate) {
			return true;
		}else {
			return false;
		}
		
	}
	
	private int delayMillis() {
		// return delay using normal distribution
		Random std = new Random();
		
		double rand_std = std.nextGaussian()*this.stdDelay+this.avgDelay; 
		int millisDelay = (int) Math.round(rand_std);
		return millisDelay;
	}
	
	
	

}
