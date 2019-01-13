package controller;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import membershipService.SuspectMessage;
import membershipService.View;

public class Controller {
	private Scanner scan = new Scanner(System.in);
	private View currentView;
	private int nrNodes;
	private int basePort = 60000;
	private InetAddress addr;
	private ArrayList<Socket> sockets = new ArrayList<Socket>();
	private int timeout = 1;
	private ArrayList<ObjectOutputStream> outputStreams = new ArrayList<ObjectOutputStream>();
	private ArrayList<ObjectInputStream> inputStreams = new ArrayList<ObjectInputStream>();
	private final Lock lock = new ReentrantLock();
	private boolean listening = true;

	private HashSet<SuspectData> suspectMsgs = new HashSet<SuspectData>();

	public Controller(int nodes){
		this.nrNodes = nodes;
		this.currentView = new View(1);

		//create sockets
		for(int i = 1; i < nrNodes + 1; i++) {

			this.currentView.getNodes().add(i);

			try {
				Socket item = new Socket("localhost", basePort + i);
				sockets.add(item);
				item.setSoTimeout(timeout);
				outputStreams.add(new ObjectOutputStream(item.getOutputStream()));
				inputStreams.add(new ObjectInputStream(item.getInputStream()));
			} catch (IOException e) {
				System.err.println("Could Not Listen on Port: " + (basePort + i));
				System.exit(-1);
			}

		}
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				SuspectMessage aux = null;
				while(listening) {
					for(int i = 1; i < nrNodes + 1; i++) {
						try {
							if(inputStreams.get(i).available() > 0) {
								try {
									aux = (SuspectMessage) inputStreams.get(i).readObject();
									handleSuspectMessage(aux);
								} catch (IOException e) {
									System.err.println("Connection Failed");
									System.exit(-1);
								} catch (ClassNotFoundException e1) {
									System.err.println("Class Not Found");
									System.exit(-1);
								}
							}
						} catch (IOException e) {
							System.err.println("Could Not Get Data from Port: " + (basePort + i));
							System.exit(-1);
						}

					}
				}  
				//close all sockets
				for(int i = 1; i < nrNodes + 1; i++) {
					try {
						inputStreams.get(i).close();
					} catch (IOException e) {
						System.err.println("Could Not Close Socket: " + (basePort + i));
						System.exit(-1);
					}
				}
			}
		});  
		t1.start();

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

	public InetAddress getAddr() {
		return addr;
	}

	public void setAddr(InetAddress addr) {
		this.addr = addr;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
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

	public ArrayList<ObjectInputStream> getInputStreams() {
		return inputStreams;
	}

	public void setInputStreams(ArrayList<ObjectInputStream> inputStreams) {
		this.inputStreams = inputStreams;
	}

	public boolean getListeningStatus() {
		return listening;
	}

	public void setListeningStatus(boolean listening) {
		this.listening = listening;
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
						lock.lock();
						sendNewView = currentView.join(Integer.parseInt(items[i]));		
						lock.unlock();
					}
					else if(items[0].equals("leave")) {
						lock.lock();
						sendNewView = currentView.leave(Integer.parseInt(items[i]));		
						lock.unlock();
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
				lock.lock();
				obj.writeObject(this.currentView);
				lock.unlock();
				obj.flush();
				obj.reset();
			} catch (IOException e) {
				System.err.println("Connection Failed");
				System.exit(-1);
			}
		}
	}

	public void handleSuspectMessage(SuspectMessage aux) {
		//TODO: finish implementation
		
		
		
		
	}

	/* **************************************
	 * 										*
	 * 			Auxiliary Class				*
	 * 										*
	 ****************************************/

	private class SuspectData {
		private HashSet<SuspectMessage> suspectMsgs = new HashSet<SuspectMessage>();
		private long timestamp;

		public SuspectData(long timestamp) {
			this.timestamp = timestamp;
		}

		public HashSet<SuspectMessage> getSuspectMsgs() {
			return suspectMsgs;
		}

		public void setSuspectMsgs(HashSet<SuspectMessage> suspectMsgs) {
			this.suspectMsgs = suspectMsgs;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

	}
}
