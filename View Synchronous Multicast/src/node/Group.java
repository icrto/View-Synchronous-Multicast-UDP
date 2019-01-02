package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import view.View;

public class Group {
	private View currentView = null;
	private int nodeID;
	private int basePort = 60000;
	private Object obj = new Object();

	public Group(int ID) {
		this.nodeID = ID;
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				ServerSocket welcomeSocket = null;
				Socket connectionSocket;
				ObjectInputStream input = null;
				try {
					welcomeSocket = new ServerSocket(basePort + nodeID);
					try {
						connectionSocket = welcomeSocket.accept(); //accept outside while loop -> server can only accept 1 connection per client
						input = new ObjectInputStream(connectionSocket.getInputStream());
						
						while(true) {
							try {
								synchronized(obj) {
									currentView = (View)input.readObject();
									System.out.println("RECEIVED: " + currentView.toString());
								}
							} catch (IOException e) {
								System.err.println("Connection Failed");
								System.exit(-1);
							} catch (ClassNotFoundException e1) {
								System.err.println("Class Not Found");
								System.exit(-1);
							}
						}
					}catch (IOException e2) {
						System.err.println("Accept Failed: " + (basePort + nodeID));
						System.exit(-1);
					}
				} catch (IOException e3) {
					System.err.println("Could Not Listen on Port: " + (basePort + nodeID));
					System.exit(-1);
				}	    
			}
		});  
		t1.start();
	}

	public View retrieveCurrentView() { //VSM calls this method to get the most recent view before sending a message
		synchronized (obj){
			return currentView;
		}
	}


	public void suspect() {

	}



}
