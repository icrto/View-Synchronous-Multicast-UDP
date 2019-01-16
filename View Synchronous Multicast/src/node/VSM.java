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
import simulation.Measurements;
import util.*;
import vsmMessage.AckFlushMessage;
import vsmMessage.AckMessage;
import vsmMessage.FlushMessage;
import vsmMessage.Message;
import vsmMessage.PayloadAcksMessage;
import vsmMessage.PayloadMessage;

public class VSM extends Thread {

	//0 -> no prints
	//1 -> only installed view and exclusion
	//2 -> only installed view, exclusion and flushes
	//3 -> all prints
	private static final int DEBUG_PRINT = 0;

	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	private int nodeId;
	private int nrNodes;

	private HashSet<MessageAcks> undeliveredMessagesAcks = new HashSet<MessageAcks>();
	private HashSet<MessageAcks> deliveredMessagesAcks = new HashSet<MessageAcks>(); // Delivered but unstable
	private HashSet<PayloadMessage> stableMessages = new HashSet<PayloadMessage>();
	private SortedSet<MessageAcks> futureViewMessagesAcks = new TreeSet<MessageAcks>();

	private LinkedBlockingQueue<View> viewQueue = new LinkedBlockingQueue<View>();

	private HashSet<FlushMessage> receivedFlushes = new HashSet<FlushMessage>();
	private HashSet<FlushMessage> futureFlushes = new HashSet<FlushMessage>();

	private HashSet<AckFlushMessage> ackFlushes = new HashSet<AckFlushMessage>();

	private Group group;
	private Measurements measure = null;
	private View currentView;
	private InetAddress UDPgroup;
	private int UDPport;
	private NetworkEmulationMulticastSocket s;
	private int timeout;
	private int seqNumber = 1;

	private boolean becameEmpty = true;
	private boolean excluded = false;

	private View mostRecentNotInstalledView = null;
	private View intersectionView = null;
	private boolean unstableMsgsSent = false; // TODO: change to false when installed all new views

	private long flushSentTime = 0;
	private boolean flushSent = false;

	private long unstableMsgsSentTime = 0;	
	private String filePath;
	private int nrStableMsgs;
	private int nrNonStableMsgs;

	private boolean viewInstalled;

	public VSM(int nNodes, int iD, String UDPmulticastIp, int port, int timeout, double dropRate, double avgDelay, double stdDelay) {
		this.nrNodes = nNodes;
		this.nodeId = iD;
		UDPport = port;
		this.timeout = timeout;
		try {
			UDPgroup = InetAddress.getByName(UDPmulticastIp);
			s = new NetworkEmulationMulticastSocket(port, dropRate, avgDelay, stdDelay);
			s.joinGroup(UDPgroup);
		} catch (IOException e) {
			System.out.println("N" + nodeId + " " + "ERROR: Failed to join UDP multicast group");
		}

		group = new Group(this, nodeId);

		currentView = new View(1);
		for(int i = 1; i < nNodes + 1; i++) {
			this.currentView.getNodes().add(i);
		}

		System.out.println("N" + nodeId + " " + currentView.toString());

//		//Testes para verificar condições iniciais 
//		futureViewMessagesAcks.add(new MessageAcks(new PayloadMessage(2, 1, 5, "Mensagem 1")));
//		futureViewMessagesAcks.add(new MessageAcks(new PayloadMessage(2, 1, 6, "Mensagem 2")));
//		futureViewMessagesAcks.add(new MessageAcks(new PayloadMessage(3, 1, 7, "Mensagem 3 da Vista 3")));
//		System.out.println("Num elementos em fut: " + futureViewMessagesAcks.size());
	}

	//constructor for measure mode
	public VSM(int nNodes, int iD, String UDPmulticastIp, int port, int timeout, double dropRate, double avgDelay, double stdDelay, String filePath, int nrStableMsgs, int nrNonStableMsgs, String variable) {

		this.nrNodes = nNodes;
		this.nodeId = iD;
		UDPport = port;
		this.timeout = timeout;
		try {
			UDPgroup = InetAddress.getByName(UDPmulticastIp);
			s = new NetworkEmulationMulticastSocket(port, dropRate, avgDelay, stdDelay);
			s.joinGroup(UDPgroup);
		} catch (IOException e) {
			System.out.println("N" + nodeId + " " + "ERROR: Failed to join UDP multicast group");
		}
		this.filePath = filePath;
		this.nrStableMsgs = nrStableMsgs;
		this.nrNonStableMsgs = nrNonStableMsgs;
		group = new Group(this, nodeId);


		currentView = new View(1);
		for(int i = 1; i < nNodes + 1; i++) {
			this.currentView.getNodes().add(i);
		}
		System.out.println("N" + nodeId + " " + currentView.toString());




		int nrTotalMsgs = this.nrNonStableMsgs + this.nrStableMsgs;

		//non stable
		for(int i = 1; i < this.nrNonStableMsgs + 1; i++) {
			//create msg and its own ACK
			undeliveredMessagesAcks.add(new MessageAcks(new PayloadMessage(currentView.getID(), nodeId, i, "Msg " + nodeId + " " + (i)), this.nodeId));					
		}

		//stable
		for(int i = this.nrNonStableMsgs + 1; i < nrTotalMsgs + 1; i++) {
			stableMessages.add(new PayloadMessage(currentView.getID(), 1, i, "Msg " + i));
		}

		try {
			measure = new Measurements(filePath, this.nodeId, this.nrNodes, this.nrStableMsgs, this.nrNonStableMsgs, variable);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getNodeId() {
		return nodeId;
	}

	/* **************************************
	 * 										*
	 * 		Receiver thread code 			*
	 * 										*
	 ****************************************/



	@Override
	public void run() {

		if(DEBUG_PRINT > 2) System.out.println("N" + nodeId + " " + "Receiver thread starting...");
		// Receiver thread code goes here

		while(true) {

			if(viewInstalled) {
				Message msg = null;

				// timeout = 1 => blocks until message received
				msg = receiveMsg(1);
				if (msg == null)
					return;

				// Handle received msg
				if (msg instanceof FlushMessage) {
					sendMsg(new AckFlushMessage(currentView.getID(),nodeId, msg.getSenderId()));
				}
				continue;
			}

			if(excluded) {
				continue;
				// TODO: ver se é preciso fazer alguma coisa para fazer join
			} else if(!viewQueue.isEmpty()) { // If the queue isn't empty then view change algorithm is run

				changeViewProcess();


			} else { // Normal operation

				arrivalMessages(1);

			}

		}

	}


	private void changeViewProcess() {

		Message msg = null;
		mostRecentNotInstalledView = getLastElement(viewQueue); // TODO: don't do this every time

		// Discover if got excluded
		if (!mostRecentNotInstalledView.getNodes().contains(nodeId)) {
			if (DEBUG_PRINT  > 0)
				System.out.println("N" + nodeId + " " + "DEBUG: Node got excluded");
			excludeNode(); // TODO: implement join later
			System.exit(1);
		}

		// Compute the intersection between current view and latest view to install
		lock.lock();
		intersectionView = new View(-1);
		HashSet<Integer> intersectionNodeIds = new HashSet<Integer>(currentView.getNodes());
		intersectionNodeIds.retainAll(mostRecentNotInstalledView.getNodes());
		intersectionView.setNodes(intersectionNodeIds);

		if(DEBUG_PRINT > 0) {
			System.out.println("Undelivered size: " + undeliveredMessagesAcks.size());
			System.out.println("Delivered size: " + deliveredMessagesAcks.size());
			System.out.println("Stable size: " + stableMessages.size());

			System.out.println(deliveredMessagesAcks.toString());

		}
		lock.unlock();


		for(FlushMessage fmsg: futureFlushes) {
			handleFlushMessage(fmsg);
		}
		futureFlushes = new HashSet<FlushMessage>();

		// if unstable messages where not sent
		if (!unstableMsgsSent) {

			updateUnstableMsgsAcks(mostRecentNotInstalledView);
			sendUnstableMsgs();
			unstableMsgsSentTime = System.currentTimeMillis();
			unstableMsgsSent = true;
		}


		//		if (unstableMsgsSent && unstableMsgsSentTime + timeout <= System.currentTimeMillis()) {
		//			// TODO: suspect de quem não recebeu ack
		//			System.out.println("Devia ter feito suspect");
		//		}
		lock.lock();
		if (unstableMsgsSent && unstableMsgsSentTime + timeout <= System.currentTimeMillis()) {
			unstableMsgsSent = false;
		}



		// All messages are stable
		//		lock.lock();
		if (undeliveredMessagesAcks.isEmpty() && deliveredMessagesAcks.isEmpty() && becameEmpty) {
			becameEmpty = false; // Only send FLUSH when sets become empty and not every time they're empty
			sendFlush();
			flushSentTime = System.currentTimeMillis();
		}
		lock.unlock();

		if(!becameEmpty && flushSentTime + timeout <= System.currentTimeMillis()) {
			becameEmpty = true;
		}


		arrivalMessages(0);

		return;

	}

	private void arrivalMessages(int state) {   //state:   0-> mudança de vista   1-> funcionamento normal

		Message msg = null;

		// timeout = 1 => blocks until message received
		msg = receiveMsg(1);
		if (msg == null)
			return;

		// Handle received msg
		if (msg instanceof PayloadAcksMessage) {
			handlePayloadAcksMessage((PayloadAcksMessage) msg);
		} else if (msg instanceof PayloadMessage) {
			handlePayloadMessage((PayloadMessage) msg);
		} else if (msg instanceof AckMessage) {
			handleAckMessage((AckMessage) msg);
		} else if (msg instanceof FlushMessage) {
			if(state == 1) {
				futureFlushes.add((FlushMessage)msg);
			}else if(state == 0) {
				handleFlushMessage((FlushMessage) msg);
			}
		} else if (msg instanceof AckFlushMessage) {
			handleAckFlushMessage((AckFlushMessage) msg);
		} else {
			System.out.println("N" + nodeId + " " + "ERROR: Received message with unknown type, continued...");
			return;
		}

		return;

	}


	//Verificar aqui os duplicados




	private void handlePayloadMessage(PayloadMessage msg) {
		if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received payload message " + msg); 

		MessageAcks msgAcks = new MessageAcks(msg);
		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received message from previous view, discarded..");
			return;
		}
		// Future view -> store in set
		if(msg.getViewId() > currentView.getID()) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received future view message, stored..");
			futureViewMessagesAcks.add(msgAcks);
			return;
		}
		// Doesn't belong to current view
		if(!currentView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received message that wasn't sent from a view member, discarded..");
			return;
		}
		// Check for duplicates
		lock.lock();
		if(undeliveredMessagesAcks.contains(msgAcks) || deliveredMessagesAcks.contains(msgAcks) || stableMessages.contains(msg)) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received duplicate message, discarded..");
			lock.unlock();
			return;
		}
		lock.unlock();
		// TODO: more checks needed?


		// Add message to undelivered message buffer along with its own ack and ack from sender
		lock.lock();
		undeliveredMessagesAcks.add(new MessageAcks(msg, msg.getSenderId(), this.nodeId));
		unstableMsgsSent = false;

		//discard all flushes if new message arrives
		receivedFlushes = new HashSet<FlushMessage>();

		if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Added " + msg.toString() + " to undelivered HashSet");
		notEmpty.signal();
		lock.unlock();

		sendAck(msg);

	}

	private void handleAckMessage(AckMessage msg) {
		if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received ack " + msg); 

		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received message from previous view, discarded..");
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
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received message that wasn't sent from a view member, discarded..");
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

		//check if message became stable
		lock.lock();
		HashSet<MessageAcks> toRemove = new HashSet<MessageAcks>();
		for(MessageAcks acks :deliveredMessagesAcks) {
			if(acks.message.getSenderId() == msg.getAckSenderId() && ((PayloadMessage)acks.message).getSeqN() == msg.getAckSeqN()) {
				acks.ackIds.add(msg.getSenderId());
				if(viewQueue.isEmpty() && acks.ackIds.equals(currentView.getNodes())) { //normal operation
					if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: message " + acks.getMessage() + " was transferred from delivered set to stable message set...");
					stableMessages.add((PayloadMessage)acks.message);
					//deliveredMessagesAcks.remove(acks);
					toRemove.add(acks);
					if(deliveredMessagesAcks.isEmpty() && undeliveredMessagesAcks.isEmpty()) {
						if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Both delivered and undelivered message sets became empty");
						becameEmpty = true;
					}
				}else if(!viewQueue.isEmpty() && (intersectionView != null) && acks.ackIds.equals(intersectionView.getNodes())) { //changing view
					if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: message " + acks.getMessage() + " was transferred from delivered set to stable message set...");
					stableMessages.add((PayloadMessage)acks.message);
					//deliveredMessagesAcks.remove(acks);
					toRemove.add(acks);
					if(deliveredMessagesAcks.isEmpty() && undeliveredMessagesAcks.isEmpty()) {
						if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Both delivered and undelivered message sets became empty");
						becameEmpty = true;
					}
				}
				deliveredMessagesAcks.removeAll(toRemove);
				lock.unlock();
				return;
			}
		}
		lock.unlock();
	}

	private void handlePayloadAcksMessage(PayloadAcksMessage msg) {
		if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received payload acks message " + msg); 

		PayloadMessage payloadMsg = new PayloadMessage(msg.getViewId(), msg.getSenderId(), msg.getSeqN(), msg.getPayload());
		MessageAcks msgAcks = new MessageAcks(payloadMsg);

		//		MessageAcks msgAcks = new MessageAcks(msg);

		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received message from previous view, discarded..");
			return;
		}
		// Future view -> store in set
		if(msg.getViewId() > currentView.getID()) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received future view message, stored..");
			futureViewMessagesAcks.add(msgAcks);
			return;
		}
		// Doesn't belong to current view
		if(!currentView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received message that wasn't sent from a view member, discarded..");
			return;
		}
		// Check for duplicates
		lock.lock();

		if(undeliveredMessagesAcks.contains(msgAcks) || deliveredMessagesAcks.contains(msgAcks) || stableMessages.contains(payloadMsg)) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received duplicate message, discarded..");
			lock.unlock();

			// Send ack even if we already had the msg
			//TODO: CHECK IF NEEDED
			sendAck((PayloadMessage)msg); 

			return;
		}
		lock.unlock();
		// TODO: more checks needed?


		// Add message to undelivered message buffer along with its own ack (ack from sender is already in msg)
		lock.lock();
		msgAcks.ackIds = msg.getAckIds();
		msgAcks.ackIds.add(this.nodeId);

		undeliveredMessagesAcks.add(msgAcks);
		unstableMsgsSent = false;

		//discard all flushes if new message arrives
		receivedFlushes = new HashSet<FlushMessage>();

		if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Added " + msg.toString() + " to undelivered HashSet");
		notEmpty.signal();
		lock.unlock();

		sendAck((PayloadMessage)msg);

	}

	private void handleFlushMessage(FlushMessage msg) {
		lock.lock();
		if(intersectionView == null) {
			lock.unlock();

			return; //TODO: talvez guardar o flush para mais tarde?
		}


		if(DEBUG_PRINT > 1) System.out.println("N" + nodeId + " " + "DEBUG: Received flush: " + msg);


		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT > 1) System.out.println("N" + nodeId + " " + "DEBUG: Received flush from previous view, discarded..");

			lock.unlock();
			return;
		}
		// Future view
		if(msg.getViewId() > currentView.getID()) {
			//TODO: save flush from future view in different set
			if(DEBUG_PRINT > 1) System.out.println("N" + nodeId + " " + "DEBUG: Received flush from future view, discarded..");
			lock.unlock();
			return;
		}

		if(!intersectionView.getNodes().contains(msg.getSenderId())) {
			if(DEBUG_PRINT > 1) System.out.println("N" + nodeId + " " + "DEBUG: Received flush from node that doesn't belong to intersection, discarded..");
			lock.unlock();
			return;
		}
		/*
		 * TODO: check if more verifications are needed
		 * maybe check for duplicate flushes? is it really needed? or each node only sends one flush?
		 */



		HashSet<Tuple<Integer, Integer>> flushStableMsgsIDs = msg.getStableMsgsIDs();
		HashSet<Tuple<Integer, Integer>> stableMsgsIDs = createTupleSet(stableMessages);
		stableMsgsIDs.addAll(createTupleSet2(undeliveredMessagesAcks));
		stableMsgsIDs.addAll(createTupleSet2(deliveredMessagesAcks));



		// Flush valid if node that flushed had the same stable messages
		if(flushStableMsgsIDs.equals(stableMsgsIDs)) {

			receivedFlushes.add(msg);
			if(DEBUG_PRINT > 1) System.out.println("N" + nodeId + " " + "DEBUG: Received flushes set updated" + receivedFlushes);
			if(ackFlushes.size() == intersectionView.getNodes().size()) {
				if(receivedFlushes.size() == intersectionView.getNodes().size()) { 
					installNewView();
				}
			}
		} else {
			lock.unlock();
			if(DEBUG_PRINT == 3) System.out.println("\n" + "FLUSH: Msgs dos Flushs diferentes das minhas.... \n");
			return;
		}
		lock.unlock();

		sendMsg(new AckFlushMessage(currentView.getID(),nodeId, msg.getSenderId()));

	}


	private void handleAckFlushMessage(AckFlushMessage msg) {
		if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received ackFlush " + msg); 

		// Previous view
		if(msg.getViewId() < currentView.getID()) {
			if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Received message from previous view, discarded..");
			return;
		}

		if(msg.getAckSenderId() == nodeId) ackFlushes.add(msg);		

		if(ackFlushes.size() == intersectionView.getNodes().size()) {
			if(receivedFlushes.size() == intersectionView.getNodes().size()) { 
				installNewView();
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

		/*TODO: if node is excluded warn the user that it is excluded 
		 * node can still send messages (check if it makes sense) and they are discarded by other nodes (this is working, Isabel checked it)
		 * but excluded node still receives and delivers its own message --> THIS NEEDS TO BE CORRECTED
		 */


		// Block until there is no new view to install
		synchronized(viewQueue) {
			while(!viewQueue.isEmpty()) {
				try {
					viewQueue.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		PayloadMessage message = new PayloadMessage(currentView.getID(), nodeId, seqNumber, payload);
		seqNumber++;

		lock.lock();
		undeliveredMessagesAcks.add(new MessageAcks(message, this.nodeId)); //put msg in undelivered buffer along with its own ack
		if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Added " + message.toString() + " to undelivered HashSet - SELF-DELIVERY");
		notEmpty.signal();
		lock.unlock();

		sendMsg(message);

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
			if(viewQueue.isEmpty() && msgAcks.ackIds.equals(currentView.getNodes())) { //normal operation
				if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: message " + msgAcks.message + " was transferred from undelivered set to stable message set...");
				stableMessages.add((PayloadMessage)msgAcks.message);
				if(deliveredMessagesAcks.isEmpty() && undeliveredMessagesAcks.isEmpty()) {
					if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Both delivered and undelivered message sets became empty");
					becameEmpty = true;
				}
			}else if(!viewQueue.isEmpty() && (intersectionView != null) && msgAcks.ackIds.equals(intersectionView.getNodes())) { //changing view
				if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: message " + msgAcks.message + " was transferred from undelivered set to stable message set...");
				stableMessages.add((PayloadMessage)msgAcks.message);
				if(deliveredMessagesAcks.isEmpty() && undeliveredMessagesAcks.isEmpty()) {
					if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Both delivered and undelivered message sets became empty");
					becameEmpty = true;
				}
			}
			else {
				if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: message " + msgAcks.message + " was transferred from undelivered set to delivered message set...");
				deliveredMessagesAcks.add(msgAcks);
			}
		} catch (InterruptedException e) {
			System.out.println("N" + nodeId + " " + "ERROR: failed to wait using condition variable");
		} finally {
			lock.unlock();
		}

		return payload;
	}

	// Method to be called by Group thread when it receives a new view from controller
	public void addViewToQueue(View view) {
		if(measure != null)
			measure.setInit(System.nanoTime());
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
		HashSet<MessageAcks> toRemove = new HashSet<MessageAcks>();
		for(MessageAcks msgAcks:deliveredMessagesAcks) {
			msgAcks.ackIds.retainAll(mostRecentNotInstalledView.getNodes());
			if(msgAcks.ackIds.equals(mostRecentNotInstalledView.getNodes())) { //msg became stable
				if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: message " + msgAcks.getMessage() + " was transferred from delivered set to stable message set - VIEW CHANGE");
				stableMessages.add((PayloadMessage)msgAcks.message);

				toRemove.add(msgAcks);
				if(deliveredMessagesAcks.isEmpty() && undeliveredMessagesAcks.isEmpty()) {
					if(DEBUG_PRINT == 3) System.out.println("N" + nodeId + " " + "DEBUG: Both delivered and undelivered message sets became empty");
					becameEmpty = true;
				}
			}
		}
		deliveredMessagesAcks.removeAll(toRemove);
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


	private void sendUnstableMsgs() {
		//		Random rd = new Random();
		//		try {
		//			Thread.sleep(500*nodeId);
		//		} catch (InterruptedException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		lock.lock();
		for(MessageAcks msgAcks:undeliveredMessagesAcks) {
			if(!msgAcks.getAckIds().equals(intersectionView.getNodes())) {
				PayloadAcksMessage payloadAcksMsg = 
						new PayloadAcksMessage(currentView.getID(), msgAcks.getMessage().getSenderId(), msgAcks.getMessage().getSeqN(), 
								msgAcks.getMessage().getPayload(), msgAcks.getAckIds());
				sendMsg(payloadAcksMsg);
			}

		}
		for(MessageAcks msgAcks:deliveredMessagesAcks) {
			PayloadAcksMessage payloadAcksMsg = 
					new PayloadAcksMessage(currentView.getID(), msgAcks.getMessage().getSenderId(), msgAcks.getMessage().getSeqN(), 
							msgAcks.getMessage().getPayload(), msgAcks.getAckIds());
			sendMsg(payloadAcksMsg);
		}
		lock.unlock();
	}

	private void sendFlush() {
		FlushMessage flush = new FlushMessage(currentView.getID() , nodeId,  createTupleSet(stableMessages));
		receivedFlushes.add(flush);
		ackFlushes.add(new AckFlushMessage(currentView.getID(), nodeId, nodeId));
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

	private HashSet<Tuple<Integer, Integer>> createTupleSet2(HashSet<MessageAcks> msgs) {
		HashSet<Tuple<Integer, Integer>> stableMsgsIDs = new HashSet<Tuple<Integer, Integer>>();
		lock.lock();
		for(MessageAcks msg: msgs) {
			stableMsgsIDs.add(new Tuple<Integer, Integer>(msg.getMessage().getSenderId(), msg.getMessage().getSeqN()));
		}
		lock.unlock();
		return stableMsgsIDs;
	}

	// Timeout = 0 => blocks
	private Message receiveMsg(int timeout) {
		byte[] buffer = new byte[40000]; // TODO: Choose size for receiver buffer
		DatagramPacket recv;
		Message msg = null;
		recv = new DatagramPacket(buffer, buffer.length);

		try {
			s.setSoTimeout(timeout);
		} catch (SocketException e1) {
			System.out.println("N" + nodeId + " " + "ERROR: Could not set MulticastSocket timeout");
		}


		try {
			s.receive(recv);
		} catch (SocketTimeoutException e) {
			return msg;
		} catch (IOException e1) {
			System.out.println("N" + nodeId + " " + "ERROR: Failed to receive UDP datagram, continued...");
			return null;
		}

		try {
			msg = bytesToMessage(recv.getData());
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("N" + nodeId + " " + "ERROR: Failed to deserialize byte array to a message object, continued...");
			return null;
		}

		return msg;
	}

	private void sendMsg(Message msg) {
		byte[] bytes = null;
		try {
			bytes = messageToBytes(msg);
		} catch (IOException e) {
			System.out.println("N" + nodeId + " " + "ERROR: Could not serialize message: " + msg);
			System.exit(-1);
		}
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, UDPgroup, UDPport);
		try {
			s.send(packet);
		} catch (IOException e) {
			System.out.println("N" + nodeId + " " + "ERROR: Could not send message: " + msg);
			System.exit(-1);
		}
		if(DEBUG_PRINT == 3) {
			if (msg instanceof PayloadAcksMessage) {
				System.out.println("N" + nodeId + " " + "DEBUG: payload acks message sent: " + (PayloadAcksMessage)msg);
			} else if(msg instanceof PayloadMessage) {
				System.out.println("N" + nodeId + " " + "DEBUG: payload message sent: " + (PayloadMessage)msg);
			} else if (msg instanceof AckMessage) {
				System.out.println("N" + nodeId + " " + "DEBUG: ack message sent: " + (AckMessage)msg);
			} else if(msg instanceof FlushMessage) {
				System.out.println("N" + nodeId + " " + "DEBUG: flush message sent: " + (FlushMessage)msg);
			} else if(msg instanceof AckFlushMessage) {
				System.out.println("N" + nodeId + " " + "DEBUG: ackFlush message sent: " + (AckFlushMessage)msg);
			} else {
				System.out.println("N" + nodeId + " " + "ERROR: Sent message of unknown type");
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
		seqNumber = 1;
		stableMessages = new HashSet<PayloadMessage>();
		mostRecentNotInstalledView = null;
		becameEmpty = true;
		intersectionView = null;

		if(!futureViewMessagesAcks.isEmpty()) {
			Iterator<MessageAcks> itr = futureViewMessagesAcks.iterator();
			MessageAcks futureMsg = itr.next();
			while(futureMsg.getMessage().getViewId() == currentView.getID()) {
				undeliveredMessagesAcks.add(futureMsg);
				itr.remove();
				if(itr.hasNext()) {
					futureMsg = itr.next();
				}else {
					break;
				}
			}
		}


		if(DEBUG_PRINT > 0) System.out.println("N" + nodeId + " " + "DEBUG: Installed view: " + currentView);
		if(measure != null) measure.setFinish(System.nanoTime());

		group.installedView();
		viewInstalled = true;

	}


	private void excludeNode() {

		// TODO: int nodeId;

		undeliveredMessagesAcks = new HashSet<MessageAcks>();
		deliveredMessagesAcks = new HashSet<MessageAcks>();
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
		public PayloadMessage message = null;
		public HashSet<Integer> ackIds = new HashSet<Integer>(); // TODO: Check if it's better to remove ids instead of adding them 
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
		private VSM getOuterType() {
			return VSM.this;
		}


		@Override
		public int compareTo(MessageAcks messageAcks) {
			return message.getViewId();
		}
		@Override
		public String toString() {
			return "MessageAcks [message=" + message + ", ackIds=" + ackIds + "]";
		}



	}
}
