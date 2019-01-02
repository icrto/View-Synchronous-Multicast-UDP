package controller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.Scanner;

import view.View;

import java.net.InetAddress;

public class Controller {
	private static DatagramSocket publisherSocket;
	private static DatagramPacket sendPacket;
	private static InetAddress IPAddress;
	private static View currentView;
	public static void main(String[] args) throws UnknownHostException, IOException {
		publisherSocket = new DatagramSocket();
		IPAddress = InetAddress.getByName("localhost");
		currentView = new View();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(System.in);
		String line;
		String[] items;
		boolean changeView = false;

		System.out.println("Controller App Started");
		System.out.println("Usage:");
		System.out.println("join <node id1> <node id2> ... <node idn>");
		System.out.println("leave <node id> <node id2> ... <node idn>");
		System.out.println(currentView.toString());
		while(true) {
			line = scan.nextLine();

			if(line.indexOf("join") != -1) {
				items = line.split(" ");
				for(int i = 1; i < items.length; i++) {
					changeView |= currentView.join(Integer.parseInt(items[i]));
				}
				if(changeView) {
					currentView.changeViewID();
					changeView();
				}
				System.out.println(currentView.toString());
				changeView = false;
			}
			else if(line.indexOf("leave") != -1) {
				items = line.split(" ");
				for(int i = 1; i < items.length; i++) {
					changeView |= currentView.leave(Integer.parseInt(items[i]));
				}
				if(changeView) {
					currentView.changeViewID();
					changeView();
				}
				System.out.println(currentView.toString());
				changeView = false;
			}
			else {
				System.out.println("INVALID COMMAND. PLEASE TRY AGAIN.");
				System.out.println("Usage:");
				System.out.println("join <node id1> <node id2> ... <node idn>");
				System.out.println("leave <node id> <node id2> ... <node idn>");
			}
		}
	}
	public static void changeView() throws IOException {
		
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		ObjectOutput oo = new ObjectOutputStream(bStream); 
		oo.writeObject(currentView);
		oo.close();

		byte[] serializedMessage = bStream.toByteArray();
		sendPacket = new DatagramPacket(serializedMessage, serializedMessage.length, IPAddress, 6789);
		publisherSocket.send(sendPacket);	
	}
}
