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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import networkEmulation.NetworkEmulationMulticastSocket;
import view.View;
import vsmMessage.AckMessage;
import vsmMessage.Message;
import vsmMessage.PayloadMessage;

public class VSM extends Thread {

	private static final boolean DEBUG_PRINT = false;

	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	private int nodeId;
	private volatile ArrayList<PayloadMessage> stableMessages = new ArrayList<PayloadMessage>();
	private ArrayList<PayloadMessage> unstableMessages = new ArrayList<PayloadMessage>();
	private ArrayList<AckMap> ackMaps = new ArrayList<AckMap>();
	private boolean changingView = false;
	private Group group;
	private View currentView;
	private InetAddress UDPgroup;
	private int UDPport;
	private NetworkEmulationMulticastSocket s;
	private int seqNumber = 1;

	public VSM(int iD, String UDPmulticastIp, int port, double dropRate, double avgDelay, double stdDelay) {
		
		this.nodeId = iD;
		UDPport = port;
		try {
			UDPgroup = InetAddress.getByName(UDPmulticastIp);
			s = new NetworkEmulationMulticastSocket(port, dropRate, avgDelay, stdDelay);
			s.joinGroup(UDPgroup);
		} catch (IOException e) {
			System.out.println("ERROR: Failed to join UDP multicast group");
		}
		//System.out.println("antes de retrieve");
		group = new Group(nodeId);
		//System.out.println("depois de group");
		currentView = group.retrieveCurrentView(); // this should block until the view is received by the controller
		
		// TODO: check if sanity check below makes sense
		if(currentView.getID() != 1) {
			System.out.println("ERROR: first retrieved view is not view 1");
			System.exit(1);
		}
		
		
		
		System.out.println(nodeId);
	//	System.out.println("depois de retrieve");

		//Testing
//		currentView = new View();
//		currentView.join(1);
//		currentView.join(2);
//		currentView.join(3);
	}
	
	/* **************************************
	 * 										*
	 * 		Receiver thread code 			*
	 * 										*
	 ****************************************/

	@Override
	public void run() {
		System.out.println("Receiver thread starting...");
		// Receiver thread code goes here

		byte[] buffer = new byte[2000]; // Need to be careful with this value 
		DatagramPacket recv;
		Message msg = null;
		
		while(true) {
			updateView();
			recv = new DatagramPacket(buffer, buffer.length);

			try {
				s.receive(recv);
			} catch (IOException e) {
				System.out.println("ERROR: Failed to receive UDP datagram, continued...");
				continue;
			}

			try {
				msg = bytesToMessage(recv.getData());
			} catch (ClassNotFoundException | IOException e) {
				System.out.println("ERROR: Failed to deserialize byte array to a message object, continued...");
				continue;
			}

			//Testing
			if(DEBUG_PRINT) {
				if(msg instanceof PayloadMessage) System.out.println("DEBUG: Received payload: \"" + ((PayloadMessage)msg).getPayload() + "\"");
				else if(msg instanceof AckMessage) System.out.println("DEBUG: Received ack for message sent from " + (((AckMessage)msg).getAckSenderId()) + 
						" with sequence number " + (((AckMessage)msg).getAckSeqN()));
			}

			// Handle received msg
			if(msg.getMessageType() == Message.PAYLOAD_MESSAGE) {
				if(DEBUG_PRINT) System.out.println("DEBUG: Received payload message, starting to process it...");
				try {
					handlePayloadMessage((PayloadMessage)msg);
				} catch (IOException e) {
					System.out.println("ERROR: Failed to send ack (serialization or socket problem), continued...");
				}
			} else if (msg.getMessageType() == Message.ACK_MESSAGE) {
				if(DEBUG_PRINT) System.out.println("DEBUG: Received ack message, starting to process it...");
				handleAckMessage((AckMessage)msg);
			} else {
				System.out.println("ERROR: Received message with unknown type, continued...");
				continue;
			}

		}
	}
	
	
	//Verficar aqui os duplicados

	private void handlePayloadMessage(PayloadMessage msg) throws IOException {
		// Check message for duplicates or if it is from the correct view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message from previous view, discarded..");
			return;
		}
		if(!currentView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message that wasn't sent from a view member, discarded..");
			return;
		}
		/* TODO: more checks needed
		 * 		- what if the view id is > than current view?
		 * 		- check for duplicates? 
		 * 		- etc
		 *
		 * Need to store all messages untio view change
		 *
		 */
		

		// Add message to unstable message buffer and add entry in ackMap
		unstableMessages.add(msg);
		ackMaps.add(new AckMap(msg.getSenderId(), msg.getSeqN()));

		// Send ack
		AckMessage ackMessage = new AckMessage(currentView.getID(), Message.ACK_MESSAGE, nodeId, msg.getSenderId(), msg.getSeqN());
		byte[] bytes = messageToBytes(ackMessage);

		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, UDPgroup, UDPport);
		s.send(packet);
		if(DEBUG_PRINT) System.out.println("DEBUG: ack sent");
	}

	private void handleAckMessage(AckMessage msg) {
		// TODO Auto-generated method stub
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message from previous view, discarded..");
			return;
		}
		if(!currentView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message that wasn't sent from a view member, discarded..");
			return;
		}
		/* TODO: more checks needed
		 * 		- what if the view id is > than current view?
		 * 		- how to check for duplicates? 
		 * 		- etc
		 */

		for(int i = 0; i < ackMaps.size(); i++) {
			AckMap ackMap = ackMaps.get(i);
			if(ackMap.senderId == msg.getAckSenderId() && ackMap.seqN == msg.getAckSeqN()) {
				if(ackMap.ackIds.contains(msg.getSenderId())) {
					return;
				} else {
					ackMap.ackIds.add(msg.getSenderId());
				}
				if(ackMap.ackIds.size() == currentView.getNodes().size()) {
					int j;
					for(j = 0; j < unstableMessages.size(); j++) {
						if(unstableMessages.get(j).getSeqN() == ackMap.seqN && unstableMessages.get(j).getSenderId() == ackMap.senderId)
							break;
					}
					if(DEBUG_PRINT) System.out.println("DEBUG: a message has become stable, transfering to stable message list...");
					lock.lock();
					stableMessages.add(unstableMessages.get(j));
					unstableMessages.remove(j);
					ackMaps.remove(i);
					notEmpty.signal();
					lock.unlock();
				}
				return;
			}
		}
	}
	
	/* **************************************
	 * 										*
	 * 			VSM API Methods 				*
	 * 										*
	 ****************************************/

	public void sendVSM(String payload) throws IOException {
		// Build and send a datagram with serialized Payload Message
		/* TODO: check if it's needed to self-deliver right away. 
		 * Right now self delivery occurs but only after the message becomes stable just as any other message.
		 * In the slides self-delivery is done right away to ensure liveness?
		 */

		updateView();
		while(changingView);

		PayloadMessage message = new PayloadMessage(currentView.getID(), nodeId, seqNumber, payload);
		seqNumber++;
		byte[] bytes = messageToBytes(message);

		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, UDPgroup, UDPport);
		s.send(packet);
	}

	public String recvVSM() {
		// Return a stable message or wait until there is one available
		// TODO: change busy waiting below to conditions variables
		String payload = null;
		
		lock.lock();
		try {
			while(stableMessages.size() == 0) {
				notEmpty.await();
			}
			payload = new String(stableMessages.get(0).getPayload());
			stableMessages.remove(0);
		} catch (InterruptedException e) {
			System.out.println("ERROR: failed to wait using condition variable");
		} finally {
			lock.unlock();
		}

		return payload;
	}
	

	/* **************************************
	 * 										*
	 * 			Auxiliary Methods 			*
	 * 										*
	 ****************************************/
	
	private void updateView() {
		//		View retrievedView = group.retrieveCurrentView();
		//		if(currentView.equals(retrievedView)) return;
		//		else {
		//			changingView = false;
		//			/* TODO: Change view!!!!!!!!!!!!
		//			 * 
		//			 * Notes:
		//			 * 		- maybe reset seqN to zero
		//			 */
		//			
		//			
		//		}
	}

	private byte[] messageToBytes(Message msg) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] bytes = null;
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(msg);
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

	private Message bytesToMessage(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		Message message = null;
		try {
			in = new ObjectInputStream(bis);
			message = (Message) in.readObject(); 
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


	/* **************************************
	 * 										*
	 * 			Auxiliary Classes			*
	 * 										*
	 ****************************************/


	//Começar a construção do array ao contrario. Começar com todos e ir removendo
	private class AckMap {
		public int senderId = -1;
		public int seqN = -1;
		public ArrayList<Integer> ackIds = new ArrayList<Integer>();
		public AckMap(int senderId, int seqN) {
			super();
			this.senderId = senderId;
			this.seqN = seqN;
		}
	}
}
