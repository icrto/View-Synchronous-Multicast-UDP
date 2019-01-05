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
import java.util.*;
import java.util.concurrent.locks.*;

import networkEmulation.NetworkEmulationMulticastSocket;
import view.View;
import vsmMessage.AckMessage;
import vsmMessage.Message;
import vsmMessage.PayloadAcksMessage;
import vsmMessage.PayloadMessage;

public class VSM extends Thread {

	private static final boolean DEBUG_PRINT = true;

	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	private int nodeId;

	private HashSet<MessageAcks> undeliveredMessagesAcks = new HashSet<MessageAcks>();
	private HashSet<MessageAcks> deliveredMessagesAcks = new HashSet<MessageAcks>(); // Delivered but unstable
	private HashSet<PayloadMessage> stableMessages = new HashSet<PayloadMessage>();
	private SortedSet<MessageAcks> futureViewMessagesAcks = new TreeSet<MessageAcks>();

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

		byte[] buffer = new byte[2000]; // TODO: Choose size for receiver buffer
		DatagramPacket recv;
		Message msg = null;

		while(true) {
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
			//
			//			//Testing
			//			if(DEBUG_PRINT) {
			//				if(msg instanceof PayloadMessage) System.out.println("DEBUG: Received payload: \"" + ((PayloadMessage)msg).getPayload() + "\"");
			//				else if(msg instanceof AckMessage) System.out.println("DEBUG: Received ack for message sent from " + (((AckMessage)msg).getAckSenderId()) + 
			//						" with sequence number " + (((AckMessage)msg).getAckSeqN())); 
			//				else if(msg instanceof PayloadAcksMessage) System.out.println("DEBUG: Received payload: \"" + ((PayloadMessage)msg).getPayload() + "\" with acks " 
			//						+ ((PayloadAcksMessage)msg).getAckIds().toString());
			//			}

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
			} else if (msg.getMessageType() == Message.PAYLOAD_ACKS_MESSAGE) {
				if(DEBUG_PRINT) System.out.println("DEBUG: Received payload with acks message, starting to process it...");
				handlePayloadAcksMessage((AckMessage)msg);
			} else {
				System.out.println("ERROR: Received message with unknown type, continued...");
				continue;
			}

		}
	}

	private void handlePayloadMessage(PayloadMessage msg) throws IOException {
		MessageAcks msgAcks = new MessageAcks(msg);
		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message from previous view, discarded..");
			return;
		}
		// Future view -> store in set
		if(msg.getViewId() > currentView.getID()) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received future view message, stored..");
			futureViewMessagesAcks.add(msgAcks);
			return;
		}
		// Doesn't belong to current view
		if(!currentView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message that wasn't sent from a view member, discarded..");
			return;
		}
		// Check for duplicates
		lock.lock();
		if(undeliveredMessagesAcks.contains(msgAcks) || deliveredMessagesAcks.contains(msgAcks) || stableMessages.contains(msg)) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received duplicate message, discarded..");
			lock.unlock();
			return;
		}
		lock.unlock();
		// TODO: more checks needed?


		// Add message to undelivered message buffer - SELF-DELIVERY
		lock.lock();
		undeliveredMessagesAcks.add(new MessageAcks(msg));
		if(DEBUG_PRINT) System.out.println("DEBUG: Added " + msg.toString() + " to undelivered HashSet");
		notEmpty.signal();
		lock.unlock();

		sendAck(msg);
	}

	private void handleAckMessage(AckMessage msg) {
		if(DEBUG_PRINT) System.out.println("DEBUG: Received ack " + msg); 

		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message from previous view, discarded..");
			return;
		}
		// Future view
		// TODO: Check about ack sender not being made!!!!
		if(msg.getViewId() > currentView.getID()) {
			for(MessageAcks acks: futureViewMessagesAcks) { 
				if(acks.message.getSenderId() == msg.getAckSenderId() && acks.message.getSeqN() == msg.getAckSeqN()) {
					acks.ackIds.add(msg.getSenderId());
				}
			}
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

		/* 
		 * Searches for the message being acked and registers the ack
		 * Also transfers the message to the stable buffer if all acks have been received
		 */

		lock.lock();
		for(MessageAcks acks :undeliveredMessagesAcks) {
			if(acks.message.getSenderId() == msg.getAckSenderId() && acks.message.getSeqN() == msg.getAckSeqN()) {
				acks.ackIds.add(msg.getSenderId());
				return;
			}
		}
		lock.unlock();

		lock.lock();
		for(MessageAcks acks :deliveredMessagesAcks) {
			if(acks.message.getSenderId() == msg.getAckSenderId() && acks.message.getSeqN() == msg.getAckSeqN()) {
				acks.ackIds.add(msg.getSenderId());
				if(acks.ackIds.size() == currentView.getNodes().size()) {
					if(DEBUG_PRINT) System.out.println("DEBUG: message " + acks.getMessage() + " was transferred from delivered set to stable message set...");
					stableMessages.add(acks.message);
					deliveredMessagesAcks.remove(acks);
				}
				lock.unlock();
				return;
			}
		}
		lock.unlock();
	}

	private void handlePayloadAcksMessage(AckMessage msg) {
		// TODO Auto-generated method stub

	}

	/* **************************************
	 * 										*
	 * 			VSM API Methods 				*
	 * 										*
	 ****************************************/

	public void sendVSM(String payload) throws IOException {
		// Build and send a datagram with serialized Payload Message

		updateView();
		while(changingView);

		PayloadMessage message = new PayloadMessage(currentView.getID(), nodeId, seqNumber, payload);
		seqNumber++;
		byte[] bytes = messageToBytes(message);

		lock.lock();
		undeliveredMessagesAcks.add(new MessageAcks(message));
		if(DEBUG_PRINT) System.out.println("DEBUG: Added " + message.toString() + " to undelivered HashSet - SELF-DELIVERY");
		notEmpty.signal();
		lock.unlock();

		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, UDPgroup, UDPport);
		s.send(packet);
		
		sendAck(message);
	}

	public String recvVSM() {
		// Return a stable message or wait until there is one available
		String payload = null;

		lock.lock();
		try {
			while(undeliveredMessagesAcks.size() == 0) {
				notEmpty.await();
			}
			MessageAcks msgAcks = undeliveredMessagesAcks.iterator().next();
			payload = new String(msgAcks.getMessage().getPayload());
			undeliveredMessagesAcks.remove(msgAcks);
			if(msgAcks.ackIds.size() == currentView.getNodes().size()) {
				if(DEBUG_PRINT) System.out.println("DEBUG: message " + msgAcks.message + " was transferred from undelivered set to stable message set...");
				stableMessages.add(msgAcks.message);
			} else {
				if(DEBUG_PRINT) System.out.println("DEBUG: message " + msgAcks.message + " was transferred from undelivered set to delivered message set...");
				deliveredMessagesAcks.add(msgAcks);
			}
		} catch (InterruptedException e) {
			System.out.println("ERROR: failed to wait using condition variable");
		} finally {
			lock.unlock();
		}

		return payload;
	}


	/* ***************************************
	 * 										*
	 * 			Auxiliary Methods 			*
	 * 										*
	 ****************************************/

	private void updateView() {
		//		View retrievedView = group.retrieveCurrentView();
		//		if(currentView.equals(retrievedView)) return;
		//		else {
		//			changingView = true;
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

	private void sendAck(PayloadMessage msg) throws IOException {
		AckMessage ackMessage = new AckMessage(currentView.getID(), Message.ACK_MESSAGE, nodeId, msg.getSenderId(), msg.getSeqN());
		byte[] bytes = messageToBytes(ackMessage);

		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, UDPgroup, UDPport);
		s.send(packet);
		if(DEBUG_PRINT) System.out.println("DEBUG: ack " + ackMessage + " sent");
	}


	/* **************************************
	 * 										*
	 * 			Auxiliary Classes			*
	 * 										*
	 ****************************************/

	@SuppressWarnings("unused")
	private class MessageAcks implements Comparable<MessageAcks> {
		public PayloadMessage message = null;
		public HashSet<Integer> ackIds = new HashSet<Integer>(); // TODO: Check if it's better to remove ids instead of adding them 
		public MessageAcks(PayloadMessage message) {
			super();
			this.message = message;
		}

		public PayloadMessage getMessage() {
			return message;
		}

		public void setMessage(PayloadMessage message) {
			this.message = message;
		}

		public HashSet<Integer> getAckIds() {
			return ackIds;
		}

		public void setAckIds(HashSet<Integer> ackIds) {
			this.ackIds = ackIds;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			//			result = prime * result + ((ackIds == null) ? 0 : ackIds.hashCode());
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MessageAcks other = (MessageAcks) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			//			if (ackIds == null) {
			//				if (other.ackIds != null)
			//					return false;
			//			} else if (!ackIds.equals(other.ackIds))
			//				return false;
			if (message == null) {
				if (other.message != null)
					return false;
			} else if (!message.equals(other.message))
				return false;
			return true;
		}
		private VSM getOuterType() {
			return VSM.this;
		}


		@Override
		public int compareTo(MessageAcks messageAcks) {
			return message.getViewId();
		}

	}
}
