package controller;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import view.View;

public class Controller {
	private Scanner scan = new Scanner(System.in);
	private View currentView;
	private int nrNodes;
	private int basePort = 60000;
	private ArrayList<Socket> sockets = new ArrayList<Socket>();
	private ArrayList<ObjectOutputStream> outputStreams = new ArrayList<ObjectOutputStream>();

	public Controller(int nodes){

		this.nrNodes = nodes;
		this.currentView = new View(1);

		//create sockets
		for(int i = 1; i < nrNodes + 1; i++) {

			this.currentView.getNodes().add(i);

			try {
				Socket item = new Socket("localhost", basePort + i);
				sockets.add(item);
				outputStreams.add(new ObjectOutputStream(item.getOutputStream()));
			} catch (IOException e) {
				System.err.println("Could Not Listen on Port: " + (basePort + i));
				System.exit(-1);
			}

		}


	}

	public View getCurrentView() {
		return currentView;
	}

	public void setCurrentView(View currentView) {
		this.currentView = currentView;
	}

	public int getNrNodes() {
		return nrNodes;
	}

	public void setNrNodes(int nrNodes) {
		this.nrNodes = nrNodes;
	}

	public int getBasePort() {
		return basePort;
	}

	public void setBasePort(int basePort) {
		this.basePort = basePort;
	}

	public ArrayList<Socket> getSockets() {
		return sockets;
	}

	public void setSockets(ArrayList<Socket> sockets) {
		this.sockets = sockets;
	}

	public ArrayList<ObjectOutputStream> getOutputStreams() {
		return outputStreams;
	}

	public void setOutputStreams(ArrayList<ObjectOutputStream> outputStreams) {
		this.outputStreams = outputStreams;
	}

	/**
	 * Method that processes input from System.in. Only accepts join and leave commands
	 * @params
	 * @return
	 */
	public void processInput() {
		String line;
		String[] items;
		boolean sendNewView = false;
		if(scan.hasNextLine()) {
			line = scan.nextLine();

			if((line.indexOf("join") != -1) || (line.indexOf("leave") != -1)) {
				items = line.split(" ");
				for(int i = 1; i < items.length; i++) {
					if(items[0].equals("join")) {
						sendNewView = currentView.join(Integer.parseInt(items[i]));		
					}
					else if(items[0].equals("leave")) {
						sendNewView = currentView.leave(Integer.parseInt(items[i]));		
					}

					if(sendNewView) {
						sendNewView();
					}
					System.out.println(currentView.toString());
				}
			}
			else {
				System.out.println("INVALID COMMAND. PLEASE TRY AGAIN.");
				System.out.println("Usage:");
				System.out.println("join <node id1> <node id2> ... <node idn>");
				System.out.println("leave <node id> <node id2> ... <node idn>");
			}
		}
	}

	/**
	 * Method that sends the new view to Group layer on each node via TCP
	 * @param 
	 * @return
	 * @throws IOException
	 */
	public void sendNewView(){
		for(ObjectOutputStream obj: this.outputStreams) {
			try {
				obj.writeObject(this.currentView);
				obj.flush();
				obj.reset();
			} catch (IOException e) {
				System.err.println("Connection Failed");
				System.exit(-1);
			}
		}
	}
}
