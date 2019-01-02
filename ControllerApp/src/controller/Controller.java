package controller;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import view.View;

public class Controller {
	private static View currentView;
	private static Integer nrNodes;
	private static int basePort = 60000;
	private static Socket item;
	private static ArrayList<Socket> sockets = new ArrayList<Socket>();
	private static ArrayList<ObjectOutputStream> outputStreams = new ArrayList<ObjectOutputStream>();
	
	public static void main(String[] args){
		if(args.length < 1) {
			System.out.println("Usage:");
			System.out.println("java -jar controller.jar <number of nodes>");
			System.exit(-1);
		}
        nrNodes = Integer.parseInt(args[0]);

        //create sockets
		for(int i = 0; i < nrNodes; i++) {
			try {
				item = new Socket("localhost", basePort + i);
				sockets.add(item);
				outputStreams.add(new ObjectOutputStream(item.getOutputStream()));
			} catch (IOException e) {
				System.err.println("Could Not Listen on Port: " + (basePort + i));
				System.exit(-1);
			}
			
		}
		
		currentView = new View();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(System.in);
		String line;
		String[] items;

		System.out.println("Controller App Started");
		System.out.println("Usage:");
		System.out.println("join <node id1> <node id2> ... <node idn>");
		System.out.println("leave <node id> <node id2> ... <node idn>");
		System.out.println(currentView.toString());
		//loop to read command line input
		while(true) {
			line = scan.nextLine();

			if(line.indexOf("join") != -1) {
				items = line.split(" ");
				for(int i = 1; i < items.length; i++) {
					if(currentView.join(Integer.parseInt(items[i]))) {
						sendNewView();
					}
					System.out.println(currentView.toString());
				}
			}
			else if(line.indexOf("leave") != -1) {
				items = line.split(" ");
				for(int i = 1; i < items.length; i++) {
					if(currentView.leave(Integer.parseInt(items[i]))) {
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
	public static void sendNewView(){
		for(ObjectOutputStream obj: outputStreams) {
			try {
				obj.writeObject(currentView);
				obj.flush();
				obj.reset();
			} catch (IOException e) {
				System.err.println("Connection Failed");
				System.exit(-1);
			}
		}
	}
}
