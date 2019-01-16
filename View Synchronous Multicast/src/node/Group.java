package node;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import view.View;

public class Group {
	private int nodeID;
	private int basePort = 60000;
	private ServerSocket welcomeSocket = null;
	private Socket connectionSocket;
	private ObjectInputStream input = null;
	private PrintWriter output = null;

	public Group(VSM vsm, int ID) {
		this.nodeID = ID;
		//this.vsm = vsm;

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					welcomeSocket = new ServerSocket(basePort + nodeID);
					try {
						connectionSocket = welcomeSocket.accept(); //accept outside while loop -> server can only accept 1 connection per client
						input = new ObjectInputStream(connectionSocket.getInputStream());
						while(true) {
							try {
								View newView = (View)input.readObject();
								System.out.println("N" + nodeID + " RECEIVED: " + newView.toString());
								vsm.addViewToQueue(newView);

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


	/**
	 * Method to inform Controller that a node has to leave the view 
	 */
	public void installedView() {
		try {
			output = new PrintWriter(connectionSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println("Output Stream Creation Failed");
			System.exit(-1);
		}
		output.println("Olá");
		System.out.println("avisou controller");
	}



}
