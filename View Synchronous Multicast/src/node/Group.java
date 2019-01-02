package node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import view.View;

public class Group {
	private View currentView = null;

	public Group() throws SocketException {
		byte[] receiveData = new byte[1024];
		@SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket(6789);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true)
				{
					try {
						serverSocket.receive(receivePacket);
						ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
						synchronized (this){
							currentView = (View) iStream.readObject();
						}
						System.out.println("RECEIVED: " + currentView.toString());
						iStream.close();
					} catch (IOException | ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}	    
			}
		});  
		t1.start();
	}

	public View retrieveCurrentView() { //VSM calls this method to get the most recent view before sending a message
		synchronized (this){
			return currentView;
		}
	}


	public void suspect() {

	}



}
