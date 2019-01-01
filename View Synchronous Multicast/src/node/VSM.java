package node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;

import networkEmulation.NetworkEmulationMulticastSocket;

public class VSM {

	private int nodeId;
	private ArrayList<VSMMessage> msgBuffer;
	private boolean changingView = false;
	private Group group;
	private View currentView;
	private InetAddress UDPgroup;
	private int UDPport;
	private NetworkEmulationMulticastSocket s;
	private int seqNumber = 1;

	public VSM(int nodeId, String UDPmulticastIp, int port, double dropRate, double avgDelay, double stdDelay) throws IOException {

		System.setProperty("java.net.preferIPv4Stack" , "true");
		UDPgroup = InetAddress.getByName(UDPmulticastIp);
		UDPport = port;
		s = new NetworkEmulationMulticastSocket(port, dropRate, avgDelay, stdDelay);
		s.joinGroup(UDPgroup);

		this.nodeId = nodeId;
		msgBuffer = new ArrayList<VSMMessage>();
		//group = new group();
		currentView = group.retrieveCurrentView(); // this should block until the view is received by the controller

		// TODO: check if sanity check below makes sense
		if(currentView.getId() != 1) {
			System.out.println("ERROR: first retrieved view is not view 1");
			// TODO: abort (throw exception?)
		}


		// TODO: launch receiving thread
	}

	public void sendVSM(String payload) throws IOException {
		updateView();
		while(changingView);

		VSMMessage message = new VSMMessage(currentView.getId(), nodeId, seqNumber, payload);
		seqNumber++;
		byte[] bytes = messageToBytes(message);

		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, UDPgroup, UDPport);
		s.send(packet);
	}

	public String recvVSM() {

		/* TODO:
		 * 1º - retornar msg presente em buffer de stable msgs ou esperar até ter alguma coisa
		 */



		//byte[] buf = new byte[1000];
		//DatagramPacket recv = new DatagramPacket(buf, buf.length);
		//s.receive(recv);
		//System.out.println(new String(recv.getData(), recv.getOffset(), recv.getLength()));	 

		return null;
	}

	private void updateView() {
		View retrievedView = group.retrieveCurrentView();
		if(currentView.equals(retrievedView)) return;
		else {
			changingView = false;
			/* TODO: Change view!!!!!!!!!!!!
			 * 
			 * Notes:
			 * 		- maybe reset seqN to zero
			 */
			
			
		}
	}





	private byte[] messageToBytes(VSMMessage message) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] bytes = null;
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(message);
			out.flush();
			bytes = bos.toByteArray();
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		} 
		return bytes;
	}
	
	private VSMMessage bytesToMessage(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		VSMMessage message = null;
		try {
		  in = new ObjectInputStream(bis);
		  message = (VSMMessage) in.readObject(); 
		} finally {
		  try {
		    if (in != null) {
		      in.close();
		    }
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		}
		return message;
	}
}
