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

import view.View;
import networkEmulation.NetworkEmulationMulticastSocket;
import util.*;
import vsmMessage.*;

public class VSMLD extends VSM {

	private static final boolean DEBUG_PRINT = true;

	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	private int nodeId;

	/* 
	 * Fluxo das mensagens:
	 * -> unstableMessagesAcks -> stableMessages -> deliveredMessagesAcks
	 */
	private HashSet<MessageAcks> unstableMessagesAcks = new HashSet<MessageAcks>();
	private HashSet<PayloadMessage> stableMessages = new HashSet<PayloadMessage>();
	private HashSet<PayloadMessage> deliveredMessages = new HashSet<PayloadMessage>();

	private SortedSet<MessageAcks> futureViewMessagesAcks = new TreeSet<MessageAcks>();

	private LinkedBlockingQueue<View> viewQueue = new LinkedBlockingQueue<View>();

	private HashSet<FlushMessage> receivedFlushes = new HashSet<FlushMessage>();
	private HashSet<FlushMessage> futureFlushes = new HashSet<FlushMessage>();

	@SuppressWarnings("unused")
	private Group group;
	private View currentView;
	private InetAddress UDPgroup;
	private int UDPport;
	private NetworkEmulationMulticastSocket s;
	private int seqNumber = 1;

	private boolean excluded = false;

	private View mostRecentNotInstalledView = null;
	private boolean flushSent = false; // TODO: change to false when installed all new views

	private int nrStableMsgs;
	private int nrNonStableMsgs;


	public VSMLD(int nNodes, int iD, String UDPmulticastIp, int port, int timeout, double dropRate, double avgDelay, double stdDelay) {
		super(nNodes, iD, UDPmulticastIp, port, timeout, dropRate, avgDelay, stdDelay);
		this.nodeId = iD;
		UDPport = port;
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
		System.out.println("This node has ID " + nodeId);
		
		
		nrNonStableMsgs = 5;
		nrStableMsgs = 5;
		int nrTotalMsgs = this.nrNonStableMsgs + this.nrStableMsgs;
		//non stable
		for(int i = 1; i < this.nrNonStableMsgs + 1; i++) {
			//create msg and its own ACK
			unstableMessagesAcks.add(new MessageAcks(new PayloadMessage(currentView.getID(), nodeId, i, "Msg " + nodeId + " " + (i)), this.nodeId));					
		}
		//stable
		for(int i = this.nrNonStableMsgs + 1; i < nrTotalMsgs + 1; i++) {
			stableMessages.add(new PayloadMessage(currentView.getID(), 1, i, "Msg " + i));
		}
		
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
			} else if(!viewQueue.isEmpty()) { // If the queue isn't empty then view change algorithm is run

				mostRecentNotInstalledView = getLastElement(viewQueue); 

				// Discover if got excluded 
				if(!mostRecentNotInstalledView.getNodes().contains(nodeId)) {
					if(DEBUG_PRINT) System.out.println("DEBUG: Node got excluded");
					excludeNode();
					System.exit(0);
					continue;
				}

				// Compose the intersection between current view and latest view to install
				View intersectionView = new View(-1);
				HashSet<Integer> intersectionNodeIds = new HashSet<Integer>(currentView.getNodes());
				intersectionNodeIds.retainAll(mostRecentNotInstalledView.getNodes());
				intersectionView.setNodes(intersectionNodeIds);


				if(!flushSent) {
					for(FlushMessage fmsg:futureFlushes) {
						handleFlushMessage(fmsg, intersectionView);
					}
					futureFlushes = new HashSet<FlushMessage>();
							
					updateUnstableMsgsAcks(mostRecentNotInstalledView);
					sendFlush();
					flushSent = true; 
				} 

				// Timeout = 1 => blocks as little as possible (1 ms)
				msg = receiveMsg(1);
				if(msg != null) {
					if(msg instanceof PayloadMessage) {
						handlePayloadMessage((PayloadMessage)msg);
					} else if (msg instanceof AckMessage) {
						handleAckMessage((AckMessage)msg);
					} else if(msg instanceof FlushMessage) { 
						handleFlushMessage((FlushMessage)msg, intersectionView);
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
					handlePayloadMessage((PayloadMessage)msg);
				} else if (msg instanceof AckMessage) {
					handleAckMessage((AckMessage)msg);
				} else if(msg instanceof FlushMessage) {
					futureFlushes.add((FlushMessage)msg);
				} else {
					System.out.println("ERROR: Received message with unknown type, continued...");
					continue;
				}

			}

		}
	}


	private void handlePayloadMessage(PayloadMessage msg) {
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
		if(unstableMessagesAcks.contains(msgAcks) || deliveredMessages.contains(msg) || stableMessages.contains(msg)) {
			if(DEBUG_PRINT) System.out.println("DEBUG: Received duplicate message, discarded..");
			lock.unlock();
			return;
		}
		lock.unlock();


		// Add message to undelivered message buffer
		lock.lock();
		unstableMessagesAcks.add(new MessageAcks(msg, msg.getSenderId(), this.nodeId));
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


		/* 
		 * Searches for the message being acked and registers the ack
		 * Also transfers the message to the stable buffer if all acks have been received
		 */

		lock.lock();
		for(MessageAcks acks :unstableMessagesAcks) {
			if(acks.message.getSenderId() == msg.getAckSenderId() && ((PayloadMessage)acks.message).getSeqN() == msg.getAckSeqN()) {
				acks.ackIds.add(msg.getSenderId());
				if(acks.ackIds.equals(currentView.getNodes())) {
					if(DEBUG_PRINT) System.out.println("DEBUG: message " + acks.getMessage() + " was transferred from undelivered set to stable message set...");
					stableMessages.add((PayloadMessage)acks.message);
					notEmpty.signal();
					deliveredMessages.remove(acks.getMessage());
				}
				lock.unlock();
				return;
			}
		}
		lock.unlock();
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

		lock.lock();
		receivedFlushes.add(msg);

		// caso tenha recebido todos os flushes
		if(receivedFlushes.size() == intersectionView.getNodes().size()) {
			HashSet<Tuple<Integer,Integer>> stableMsgIds = new HashSet<Tuple<Integer,Integer>>();

			// Cria conjunto com mensagens estáveis
			for(FlushMessage fMsg:receivedFlushes) {
				stableMsgIds.addAll(fMsg.getStableMsgsIDs());
			}

			// Para cada mensagem em unstableMessages verifica se fica estável e entrega ou estão descarta
			for(MessageAcks msgAcks:unstableMessagesAcks) {
				if(stableMsgIds.contains(new Tuple<Integer,Integer>(msgAcks.getMessage().getSenderId(), msgAcks.getMessage().getSeqN()))) {
					if(DEBUG_PRINT) System.out.println("DEBUG: message " + msgAcks.getMessage() + " was transferred from unstable set to stable message set - VIEW CHANGE");
					stableMessages.add((PayloadMessage)msgAcks.message);
					notEmpty.signal();
				} else {
					if(DEBUG_PRINT) System.out.println("DEBUG: message " + msgAcks.getMessage() + " was discarded - VIEW CHANGE");
					// TODO: o que fazer ao descartar mensagens em relação à self delivery?
				}
			}
			unstableMessagesAcks.removeAll(unstableMessagesAcks);
			installNewView();
		}
		lock.unlock();

	}

	/* **************************************
	 * 										*
	 * 			VSM API Methods 				*
	 * 										*
	 ****************************************/

	public void sendVSM(String payload) throws IOException {
		// Build and send a datagram with serialized Payload Message

		// Block until there is no new view to install
		synchronized(viewQueue) {
			while(!viewQueue.isEmpty()) {
				try {
					viewQueue.wait();
				} catch (InterruptedException e) {
					System.out.println("ERROR: Coulnt not wait for viewQueue to become empty");
					System.exit(-1);
				}
			}
		}

		PayloadMessage message = new PayloadMessage(currentView.getID(), nodeId, seqNumber, payload);
		seqNumber++;

		lock.lock();
		unstableMessagesAcks.add(new MessageAcks(message, this.nodeId));
		if(DEBUG_PRINT) System.out.println("DEBUG: Added " + message.toString() + " to undelivered HashSet - SELF-DELIVERY");
		lock.unlock();

		sendMsg(message);
	}

	public String recvVSM() {
		// Return a stable message or wait until there is one available
		String payload = null;

		lock.lock();
		try {
			while(stableMessages.size() == 0) {
				notEmpty.await();
			}
			PayloadMessage msg = stableMessages.iterator().next();
			payload = new String(msg.getPayload());
			deliveredMessages.add(msg);
			stableMessages.remove(msg);
			if(DEBUG_PRINT) System.out.println("DEBUG: message " + msg + " was transferred from stable set to delivered message set...");
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
		HashSet<MessageAcks> toRemove = new HashSet<MessageAcks>();
		for(MessageAcks msgAcks:unstableMessagesAcks) {
			msgAcks.ackIds.retainAll(mostRecentNotInstalledView.getNodes());
			if(msgAcks.ackIds.equals(mostRecentNotInstalledView.getNodes())) {
				if(DEBUG_PRINT) System.out.println("DEBUG: message " + msgAcks.getMessage() + " was transferred from unstable set to stable message set - VIEW CHANGE");
				stableMessages.add((PayloadMessage)msgAcks.message);
				notEmpty.signal();
				toRemove.add(msgAcks);
			}
		}
		unstableMessagesAcks.removeAll(toRemove);
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

	private void sendAck(PayloadMessage msg) {
		AckMessage ackMessage = new AckMessage(currentView.getID(), nodeId, msg.getSenderId(), msg.getSeqN());
		sendMsg(ackMessage);
	}

	private void sendFlush() {
		HashSet<Tuple<Integer,Integer>> stableMsgs = createTupleSet(stableMessages);
		stableMsgs.addAll(createTupleSet(deliveredMessages));
		FlushMessage flush = new FlushMessage(currentView.getID() , nodeId,  stableMsgs);
		receivedFlushes.add(flush);
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

	private Message receiveMsg(int timeout) { // Timeout = 0 => blocks
		byte[] buffer = new byte[2048]; 
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
			} else {
				System.out.println("ERROR: Sent message of unknown type");
			}
		}
	}

	private void installNewView() {
		currentView = viewQueue.element();
		viewQueue.remove(currentView);
		synchronized(viewQueue){
			if(viewQueue.isEmpty()) {
				viewQueue.notify();
			}
		}
		receivedFlushes = new HashSet<FlushMessage>(); // Delete all received flushes
		stableMessages = new HashSet<PayloadMessage>();
		unstableMessagesAcks = new HashSet<MessageAcks>();
		deliveredMessages = new HashSet<PayloadMessage>();
		seqNumber = 1;
		flushSent = false;
		mostRecentNotInstalledView = null;
		

		//		if(futureViewMessagesAcks != null) {
		//			MessageAcks futureMsg =  futureViewMessagesAcks.first();
		//			while(futureMsg.getMessage().getViewId() == currentView.getID()) {
		//				unstableMessagesAcks.add(futureMsg);
		//				futureViewMessagesAcks.remove(futureMsg);
		//				futureMsg = futureViewMessagesAcks.first();
		//				if(futureMsg == null) break;
		//			}
		//		}

		if(DEBUG_PRINT) System.out.println("DEBUG: Installed view: " + currentView);
	}
	private void excludeNode() {


		unstableMessagesAcks = new HashSet<MessageAcks>();
		deliveredMessages = new HashSet<PayloadMessage>();
		stableMessages = new HashSet<PayloadMessage>();
		futureViewMessagesAcks = new TreeSet<MessageAcks>();

		viewQueue = new LinkedBlockingQueue<View>();

		receivedFlushes = new HashSet<FlushMessage>();

		seqNumber = 1;

		excluded = true;

		mostRecentNotInstalledView = null;
		flushSent = false; 


	}


	/* **************************************
	 * 										*
	 * 			Auxiliary Classes			*
	 * 										*
	 ****************************************/


	@SuppressWarnings("unused")
	private class MessageAcks implements Comparable<MessageAcks> {
		public PayloadMessage message = null;
		public HashSet<Integer> ackIds = new HashSet<Integer>();
		public MessageAcks(PayloadMessage message) {
			super();
			this.message = message;
		}
		//constructor to add msg with ACKs from sender and node itself
		public MessageAcks(PayloadMessage message, int ID1, int ID2) {
			super();
			this.message = message;
			this.ackIds.add(ID1);
			this.ackIds.add(ID2);
		}
		public MessageAcks(PayloadMessage message, int ID) {
			super();
			this.message = message;
			this.ackIds.add(ID);
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
		private VSMLD getOuterType() {
			return VSMLD.this;
		}


		@Override
		public int compareTo(MessageAcks messageAcks) {
			return message.getViewId();
		}

	}
}
