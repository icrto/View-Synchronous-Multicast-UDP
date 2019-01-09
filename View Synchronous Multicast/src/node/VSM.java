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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.*;

import networkEmulation.NetworkEmulationMulticastSocket;
import view.View;
import util.*;
import vsmMessage.AckMessage;
import vsmMessage.FlushMessage;
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

	private LinkedBlockingQueue<View> viewQueue = new LinkedBlockingQueue<View>();

	private HashSet<FlushMessage> receivedFlushes = new HashSet<FlushMessage>();

	private Group group;
	private View currentView;
	private InetAddress UDPgroup;
	private int UDPport;
	private NetworkEmulationMulticastSocket s;
	private int timeout;
	private int seqNumber = 1;

	private boolean becameEmpty = true;
	private boolean excluded = false;

	private View mostRecentNotInstalledView = null;
	private boolean unstableMsgsSent = false; // TODO: change to false when installed all new views

	private long unstableMsgsSentTime = 0;



	public VSM(int nNodes, int iD, String UDPmulticastIp, int port, int timeout, double dropRate, double avgDelay, double stdDelay) {

		this.nodeId = iD;
		UDPport = port;
		this.timeout = timeout;
		try {
			UDPgroup = InetAddress.getByName(UDPmulticastIp);
			s = new NetworkEmulationMulticastSocket(port, dropRate, avgDelay, stdDelay);
			s.joinGroup(UDPgroup);
		} catch (IOException e) {
			System.out.println("ERROR: Failed to join UDP multicast group");
		}

		group = new Group(this, nodeId);

		currentView = new View(1);
		for(int i = 1; i < nNodes + 1; i++) {
			this.currentView.getNodes().add(i);
		}

		System.out.println(currentView.toString());

		//		if(currentView.getID() != 1) {
		//			System.out.println("ERROR: first retrieved view is not view 1");
		//			System.exit(1);
		//		}

		System.out.println("This node has ID " + nodeId);
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

		Message msg = null;




		while(true) {

			if(excluded) {
				continue;
				// TODO: ver se é preciso fazer alguma coisa para fazer join
			} else if(!viewQueue.isEmpty()) { // If the queue isn't empty then view change algorithm is run

				mostRecentNotInstalledView = getLastElement(viewQueue); // TODO: don't do this every time

				if(!mostRecentNotInstalledView.getNodes().contains(nodeId)) {
					if(DEBUG_PRINT) System.out.println("DEBUG: Node got excluded");
					excludeNode(); 
					continue;
				}

				View intersectionView = new View(-1);

				HashSet<Integer> intersectionNodeIds = new HashSet<Integer>(currentView.getNodes());
				intersectionNodeIds.retainAll(mostRecentNotInstalledView.getNodes());

				intersectionView.setNodes(intersectionNodeIds);




				//				if(!unstableMsgsSent) {
				//					updateUnstableMsgsAcks(mostRecentNotInstalledView);
				//					// TODO: sendUnstableMsgs();
				//					unstableMsgsSentTime = System.currentTimeMillis();
				//					unstableMsgsSent = true; 
				//				} 
				//
				//				if(unstableMsgsSent && unstableMsgsSentTime + timeout <= System.currentTimeMillis()) {
				//					// TODO: suspect de quem não recebeu ack
				//				}

				// All messages are stable
				if(undeliveredMessagesAcks.isEmpty() && deliveredMessagesAcks.isEmpty() && becameEmpty) {
					becameEmpty = false; // Only send FLUSH when sets become empty and not every time they're empty
					sendFlush();
				}

				// Timeout = 1 => blocks as little as possible (1 ms)
				msg = receiveMsg(1);
				if(msg != null) {

					if(msg instanceof PayloadMessage) {
						//if(DEBUG_PRINT) System.out.println("DEBUG: Received payload message, starting to process it...");
						try {
							handlePayloadMessage((PayloadMessage)msg);
						} catch (IOException e) {
							System.out.println("ERROR: Failed to send ack (serialization or socket problem), continued...");
						}
					} else if (msg instanceof AckMessage) {
						//if(DEBUG_PRINT) System.out.println("DEBUG: Received ack message, starting to process it...");
						handleAckMessage((AckMessage)msg);
					} else if(msg instanceof FlushMessage) { 
						handleFlushMessage((FlushMessage)msg, intersectionView);
					} else if (msg instanceof PayloadAcksMessage) {
						handlePayloadAcksMessage((AckMessage)msg);
					} else {
						System.out.println("ERROR: Received message with unknown type, continued...");
						continue;
					}

				}

			} else { // Normal operation

				// timeout = 0 => blocks until message received
				msg = receiveMsg(1);
				if(msg == null) continue;

				// Handle received msg
				if(msg instanceof PayloadMessage) {
					//if(DEBUG_PRINT) System.out.println("DEBUG: Received payload message, starting to process it...");
					try {
						handlePayloadMessage((PayloadMessage)msg);
					} catch (IOException e) {
						System.out.println("ERROR: Failed to send ack (serialization or socket problem), continued...");
					}
				} else if (msg instanceof AckMessage) {
					//if(DEBUG_PRINT) System.out.println("DEBUG: Received ack message, starting to process it...");
					handleAckMessage((AckMessage)msg);
				} else if(msg instanceof FlushMessage) {
					//handleFlushMessage((FlushMessage)msg); // TODO: Check this!
				} else if (msg instanceof PayloadAcksMessage) {
					//if(DEBUG_PRINT) System.out.println("DEBUG: Received payload with acks message, starting to process it...");
					handlePayloadAcksMessage((AckMessage)msg);
				} else {
					System.out.println("ERROR: Received message with unknown type, continued...");
					continue;
				}

			}

		}
	}
	
	
	//Verficar aqui os duplicados

	private void handlePayloadMessage(PayloadMessage msg) throws IOException {
		if(DEBUG_PRINT) System.out.println("DEBUG: Received payload message " + msg); 

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
				if(acks.message.getSenderId() == msg.getAckSenderId() && ((PayloadMessage)acks.message).getSeqN() == msg.getAckSeqN()) {
					acks.ackIds.add(msg.getSenderId());
				}
			}
		}
		if(!currentView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received message that wasn't sent from a view member, discarded..");
			return;
		}
		// TODO: more checks needed?


		/* 
		 * Searches for the message being acked and registers the ack
		 * Also transfers the message to the stable buffer if all acks have been received
		 */

		lock.lock();
		for(MessageAcks acks :undeliveredMessagesAcks) {
			if(acks.message.getSenderId() == msg.getAckSenderId() && ((PayloadMessage)acks.message).getSeqN() == msg.getAckSeqN()) {
				acks.ackIds.add(msg.getSenderId());
				lock.unlock();
				return;
			}
		}
		lock.unlock();

		lock.lock();
		for(MessageAcks acks :deliveredMessagesAcks) {
			if(acks.message.getSenderId() == msg.getAckSenderId() && ((PayloadMessage)acks.message).getSeqN() == msg.getAckSeqN()) {
				acks.ackIds.add(msg.getSenderId());
				if(acks.ackIds.equals(currentView.getNodes())) {
					if(DEBUG_PRINT) System.out.println("DEBUG: message " + acks.getMessage() + " was transferred from delivered set to stable message set...");
					stableMessages.add((PayloadMessage)acks.message);
					deliveredMessagesAcks.remove(acks);
					if(deliveredMessagesAcks.isEmpty() && undeliveredMessagesAcks.isEmpty()) {
						if(DEBUG_PRINT) System.out.println("DEBUG: Both delivered and undelivered message sets became empty");
						becameEmpty = true;
					}
				}
				lock.unlock();
				return;
			}
		}
		lock.unlock();
	}

	private void handlePayloadAcksMessage(AckMessage msg) {


	}

	private void handleFlushMessage(FlushMessage msg, View intersectionView) {

		if(DEBUG_PRINT) System.out.println("Received flush: " + msg);

		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received flush from previous view, discarded..");
			return;
		}
		// Future view
		if(msg.getViewId() > currentView.getID()) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received flush from future view, discarded..");
			return;
		}

		if(!intersectionView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received flush from node that doesn't belong to intersection, discarded..");
			return;
		}
		/*
		 * TODO: check if more verifications are needed
		 * maybe check for duplicate flushes? is it really needed? or each node only sends one flush?
		 */

		lock.lock();
		if(undeliveredMessagesAcks.isEmpty() && deliveredMessagesAcks.isEmpty()) {
			lock.unlock();
			HashSet<Tuple<Integer, Integer>> flushStableMsgsIDs = msg.getStableMsgsIDs();
			HashSet<Tuple<Integer, Integer>> stableMsgsIDs = createTupleSet(stableMessages);


			// Flush valid if node that flushed had the same stable messages
			if(flushStableMsgsIDs.equals(stableMsgsIDs)) {

				receivedFlushes.add(msg);
				System.out.println("received flushes fifo updated" + receivedFlushes);

				if(receivedFlushes.size() == intersectionView.getNodes().size()) { 
					installNewView();
				}
			} else {
				return;
			}
		} else {
			lock.unlock();
		}

	}

	/* **************************************
	 * 										*
	 * 			VSM API Methods 				*
	 * 										*
	 ****************************************/

	public void sendVSM(String payload) throws IOException {
		// Build and send a datagram with serialized Payload Message

		/*TODO: if node is excluded warn the user that it is excluded 
		 * node can still send messages (check if it makes sense) and they are discarded by other nodes (this is working, Isabel checked it)
		 * but excluded node still receives and delivers its own message --> THIS NEEDS TO BE CORRECTED
		 */
		
		
		// Block until there is no new view to install
		while(!viewQueue.isEmpty());

		PayloadMessage message = new PayloadMessage(currentView.getID(), nodeId, seqNumber, payload);
		seqNumber++;

		lock.lock();
		undeliveredMessagesAcks.add(new MessageAcks(message));
		if(DEBUG_PRINT) System.out.println("DEBUG: Added " + message.toString() + " to undelivered HashSet - SELF-DELIVERY");
		notEmpty.signal();
		lock.unlock();

		sendMsg(message);

		// Send ack for msg "message"
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
			payload = new String(((PayloadMessage)msgAcks.getMessage()).getPayload());
			undeliveredMessagesAcks.remove(msgAcks);
			if(msgAcks.ackIds.size() == currentView.getNodes().size()) {
				if(DEBUG_PRINT) System.out.println("DEBUG: message " + msgAcks.message + " was transferred from undelivered set to stable message set...");
				stableMessages.add((PayloadMessage)msgAcks.message);
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

	// Method to be called by Group thread when it receives a new view from controller
	public void addViewToQueue(View view) {
		viewQueue.add(view);
	}


	/* ***************************************
	 * 										*
	 * 			Auxiliary Methods 			*
	 * 										*
	 ****************************************/

	// Remove acks from nodes that left in next views
	private void updateUnstableMsgsAcks(View mostRecentNotInstalledView) {
		lock.lock();
		for(MessageAcks msgAcks:undeliveredMessagesAcks) {
			msgAcks.ackIds.retainAll(mostRecentNotInstalledView.getNodes());
		}
		for(MessageAcks msgAcks:deliveredMessagesAcks) {
			msgAcks.ackIds.retainAll(mostRecentNotInstalledView.getNodes());
			if(msgAcks.ackIds.equals(mostRecentNotInstalledView.getNodes())) {
				if(DEBUG_PRINT) System.out.println("DEBUG: message " + msgAcks.getMessage() + " was transferred from delivered set to stable message set - VIEW CHANGE");
				stableMessages.add((PayloadMessage)msgAcks.message);
				deliveredMessagesAcks.remove(msgAcks);
				if(deliveredMessagesAcks.isEmpty() && undeliveredMessagesAcks.isEmpty()) {
					if(DEBUG_PRINT) System.out.println("Both delivered and undelivered message sets became empty");
					becameEmpty = true;
				}
			}
		}
		lock.unlock();
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

	private static <T> T getLastElement(final Iterable<T> elements) {
		final Iterator<T> itr = elements.iterator();
		T lastElement = itr.next();

		while(itr.hasNext()) {
			lastElement = itr.next();
		}

		return lastElement;
	}

	private void sendAck(PayloadMessage msg) throws IOException {
		AckMessage ackMessage = new AckMessage(currentView.getID(), nodeId, msg.getSenderId(), msg.getSeqN());
		sendMsg(ackMessage);
	}

	private void sendFlush() {
		FlushMessage flush = new FlushMessage(currentView.getID() , nodeId,  createTupleSet(stableMessages));
		sendMsg(flush);
	}

	private HashSet<Tuple<Integer, Integer>> createTupleSet(HashSet<PayloadMessage> msgs) {
		HashSet<Tuple<Integer, Integer>> stableMsgsIDs = new HashSet<Tuple<Integer, Integer>>();
		lock.lock();
		for(PayloadMessage msg: msgs) {
			stableMsgsIDs.add(new Tuple<Integer, Integer>(msg.getSenderId(), msg.getSeqN()));
		}
		lock.unlock();
		return stableMsgsIDs;
	}


	// Timeout = 0 => blocks
	private Message receiveMsg(int timeout) {
		byte[] buffer = new byte[2048]; // TODO: Choose size for receiver buffer
		DatagramPacket recv;
		Message msg = null;
		recv = new DatagramPacket(buffer, buffer.length);

		try {
			s.setSoTimeout(timeout);
		} catch (SocketException e1) {
			System.out.println("ERROR: Could not set MulticastSocket timeout");
		}


		try {
			s.receive(recv);
		} catch (SocketTimeoutException e) {
			return msg;
		} catch (IOException e1) {
			System.out.println("ERROR: Failed to receive UDP datagram, continued...");
			return null;
		}

		try {
			msg = bytesToMessage(recv.getData());
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("ERROR: Failed to deserialize byte array to a message object, continued...");
			return null;
		}

		return msg;
	}

	private void sendMsg(Message msg) {
		byte[] bytes = null;
		try {
			bytes = messageToBytes(msg);
		} catch (IOException e) {
			System.out.println("ERROR: Could not serialize message: " + msg);
			System.exit(-1);
		}
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, UDPgroup, UDPport);
		try {
			s.send(packet);
		} catch (IOException e) {
			System.out.println("ERROR: Could not send message: " + msg);
			System.exit(-1);
		}
		if(DEBUG_PRINT) {
			if(msg instanceof PayloadMessage) {
				System.out.println("DEBUG: payload message sent: " + (PayloadMessage)msg);
			} else if (msg instanceof AckMessage) {
				System.out.println("DEBUG: ack message sent: " + (AckMessage)msg);
			} else if(msg instanceof FlushMessage) {
				System.out.println("DEBUG: flush message sent: " + (FlushMessage)msg);
			} else if (msg instanceof PayloadAcksMessage) {
				System.out.println("DEBUG: payload acks message sent: " + (PayloadAcksMessage)msg);
			} else {
				System.out.println("ERROR: Sent message of unknown type");
			}
		}
	}

	private void installNewView() {
		currentView = viewQueue.element();
		viewQueue.remove(currentView);
		receivedFlushes = new HashSet<FlushMessage>(); // Delete all received flushes
		seqNumber = 1;
		stableMessages = new HashSet<PayloadMessage>();
		mostRecentNotInstalledView = null;
		becameEmpty = true;
	
		if(futureViewMessagesAcks != null) {
			MessageAcks futureMsg =  futureViewMessagesAcks.first();
			while(futureMsg.getMessage().getViewId() == currentView.getID()) {
				undeliveredMessagesAcks.add(futureMsg);
				futureViewMessagesAcks.remove(futureMsg);
				futureMsg = futureViewMessagesAcks.first();
				if(futureMsg == null) break;
			}
		}

		if(DEBUG_PRINT) System.out.println("DEBUG: Installed view: " + currentView);
	}
	private void excludeNode() {

		// TODO: int nodeId;

		undeliveredMessagesAcks = new HashSet<MessageAcks>();
		deliveredMessagesAcks = new HashSet<MessageAcks>(); // Delivered but unstable
		stableMessages = new HashSet<PayloadMessage>();
		futureViewMessagesAcks = new TreeSet<MessageAcks>();

		viewQueue = new LinkedBlockingQueue<View>();

		receivedFlushes = new HashSet<FlushMessage>();

		seqNumber = 1;

		becameEmpty = true;
		excluded = true;

		mostRecentNotInstalledView = null;
		unstableMsgsSent = false; 

		unstableMsgsSentTime = 0;

		// TODO: excluded tem de voltar a false quando nó reentra

	}


	/* **************************************
	 * 										*
	 * 			Auxiliary Classes			*
	 * 										*
	 ****************************************/


	@SuppressWarnings("unused")
	private class MessageAcks implements Comparable<MessageAcks> {
		public Message message = null;
		public HashSet<Integer> ackIds = new HashSet<Integer>(); // TODO: Check if it's better to remove ids instead of adding them 
		public MessageAcks(PayloadMessage message) {
			super();
			this.message = message;
		}

		public Message getMessage() {
			return message;
		}

		public void setMessage(Message message) {
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
