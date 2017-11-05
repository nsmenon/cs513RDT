
package dataLinkLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import application.AppLayerObject;
import network.NetworkLayerNew;
import packet.PacketData;


public class TransportLayerNew {
	private NetworkLayerNew m_networkLayer = null;
	private static int base = 0;
	
	private  int nextSeqNumSender = 0;
	private  int nextSeqNumReceiver = 0;
	private  int windowSize = 10;
	
	public final static int TIME_OUT = 5000;
	Thread sendPacketsThread,monitorACKsThread; 
	
	// use a list to cache the packets sent but unACKed
	private static List<PacketData> cache = new LinkedList<PacketData>();
	
	// lock used to keep mutual exclusive
	private static Object lock = new Object();

	final static int WINDOW_SIZE = 8;
    final static int ACK = 1;
    final static int NAK = 0;
    
    static Timer timeoutTimer;
    static int[] windowSender;
    static int startWindowSender;
    static int[] windowReceiver;
    static int startWindowReceiver;
    static int numberOfTimeouts;
    static int totalPacketsSent;
    static int corruptAcks;
    static double totalBytesSent;
    int noDuplicatePackets;
    byte[][] segmentedFile;
    

	public TransportLayerNew(AppLayerObject appObject, int senderPort, int receiverPort) {
		this.m_networkLayer = new NetworkLayerNew(appObject,senderPort, receiverPort);
		this.m_networkLayer.setTransportLayer(this);
		this.windowSize = appObject.getWindowSize();
	}

	public void dataLinkSend(final FileInputStream tobeSentStream, int protMode) throws Exception {
		threadsForSender(tobeSentStream,protMode);
	}

	// API provided to upper layer (application layer) to
	// receive a message. 
	public synchronized void dataLinkReceive(FileOutputStream fos, int protMode) throws Exception {
		if (protMode == 0) {
		int endOfFile = 0;
        int packetsDamaged = 0;
        int noDuplicatePackets = 0;
        List<Integer> sequenceList = new ArrayList<>();
			while (endOfFile != 1) {
				PacketData packetReceived = receiveFromNetworkLayer();
				System.out.println("Arrival " + packetReceived.getSeqNum());
				switch (packetReceived.getType()) {
				case 1:
					// length of the data packets should be larger than 0
					if (packetReceived.getLength() <= 0) {
						System.out.println("Invalid packet length for data received!!!");
						break;
					} else {
						if (packetReceived.getSeqNum() == nextSeqNumReceiver) {
							if(sequenceList.contains(packetReceived.getSeqNum())) {
								noDuplicatePackets++;
							}else {
								sequenceList.add(packetReceived.getSeqNum());
							}
							writeToFile(packetReceived, fos);
							System.out.println("Debug:ACK sent for Packet with Seq [" +packetReceived.getSeqNum() + "]");
							sendACK(nextSeqNumReceiver % 32);
							nextSeqNumReceiver++;
							break;
						} else {
								sequenceList.add(packetReceived.getSeqNum());
								if(sequenceList.contains(packetReceived.getSeqNum())) {
									noDuplicatePackets++;
								}else {
									sequenceList.add(packetReceived.getSeqNum());
								}
							System.out.println(" An an out of Order Packet Received.Discarded, sending last correctly recived Packet ack");
							if (nextSeqNumReceiver == 0)
								break;
							else
								sendACK(nextSeqNumReceiver % 32 - 1);
							break;
						}
					}
				case 2:
					if (packetReceived.getLength() != 0) {
						System.out.println("Invalid packet length for EOT received!!!. Something is not right here");
						break;
					} else {
						if (packetReceived.getSeqNum() == nextSeqNumReceiver % 32) {
							endOfFile = 1;
							System.out.println("Last Packet of the window received");
							System.out.println("PACKETS DAMAGED: " + packetsDamaged);
							 System.out.println("TOTAL # OF ACKS DROPPED: " + m_networkLayer.getNumberPacketDrops());
							System.out.println("TOTAL # OF DUPLICATE PACKETS RECEIVED: " + noDuplicatePackets);
							sendEOT(packetReceived.getSeqNum());
							fos.close();
							break;
						} else {
							if (nextSeqNumReceiver == 0)
								break;
							else
								sendACK(nextSeqNumReceiver % 32 - 1);
							break;
						}
	
					}
				case 3 :
					System.out.println("Packet with Sequence Number "+packetReceived.getSeqNum()+" is corrupt. Sending last correctly recived Packet ack");
					packetsDamaged++;
					if (nextSeqNumReceiver == 0)
						break;
					else
						sendACK(nextSeqNumReceiver % 32 - 1);
					break;
				}
			}
		}else {
			  	 int totalPacketsReceived = 0;
		         int packetsDamaged = 0;
		         int noDuplicatePackets = 0;
		         
		         List<byte[]> segmentedFile = new ArrayList<byte[]>();
		         List<Integer>segmentedFileOrder = new ArrayList<Integer>();
		         startWindowReceiver = 0;
		         windowReceiver = new int[WINDOW_SIZE];
		         Arrays.fill(windowReceiver, NAK);
		         while(true){
		         	//receive packet
		            PacketData pkt = receiveFromNetworkLayer();
		            if(pkt.getType() == 2)
		            	break;
		            totalPacketsReceived++;
		            if (pkt.getType() != 3) {
		               int packetSeqNum = pkt.getSeqNum();
					if (!segmentedFileOrder.contains(packetSeqNum)) {
						segmentedFileOrder.add(packetSeqNum);
						segmentedFile.add(pkt.getData());
					}else {
						noDuplicatePackets++;
						continue;
					}
		               ackPacket(packetSeqNum);
		               sendAcknowledgement();
		               adjustWindow(packetSeqNum);
		               System.out.println("Debug:ACK sent for Packet with Seq [" +packetSeqNum + "] Receiver Window - "+windowReceiver);
		            }
		            else{
		            	System.out.println("Packet with Sequence Number "+pkt.getSeqNum()+" is corrupt. Do nothing wait for timeout");
		               packetsDamaged++;
		            }
		         }
		         reassembleFile(segmentedFile, segmentedFileOrder);
		         System.out.println("TOTAL # OF PACKETS CORRUPTED: " + packetsDamaged);
		         System.out.println("TOTAL # OF ACKS DROPPED: " + m_networkLayer.getNumberPacketDrops());
		         System.out.println("TOTAL # OF DUPLICATE PACKETS RECEIVED: " + noDuplicatePackets);
		}
	}
	
	
	private void threadsForSender(final FileInputStream fis, int protocolMode) throws InterruptedException {
		if(protocolMode == 0) {
		sendPacketsThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					sendPackets(fis, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		monitorACKsThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					monitorACKs();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// start the threads
		sendPacketsThread.start();
		monitorACKsThread.start();

		// wait for threads finish
		sendPacketsThread.join();
		monitorACKsThread.join();
	
		} else {
			sendPacketsThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						selectiveRepeatSend(fis);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			monitorACKsThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						monitorACKsSelectiveRepeat(); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			// start the threads
			sendPacketsThread.start();
			monitorACKsThread.start();

			// wait for threads finish
			sendPacketsThread.join();
			monitorACKsThread.join();
		}

	}

	public PacketData receiveFromNetworkLayer() throws SocketTimeoutException,Exception {
		byte[] receivedData = m_networkLayer.receive();
		if (null == receivedData)
			return null;
		PacketData ACKPackets = PacketData.parseUDPdata(receivedData);
		return ACKPackets;
	}

	public void sendToNetworkLayer(PacketData packet, boolean noLoss) {
		m_networkLayer.send(packet,noLoss);
	}
	
	
	private void sendPackets(FileInputStream fis, int i) {
		try {
			PacketData p;
			byte[] buffer = new byte[496];
			totalPacketsSent = 0;
			totalBytesSent = 0;
			while (true) {
				Thread.sleep(0);
				if (nextSeqNumSender >= base + windowSize)
					continue;
				// here should be mutex
				synchronized (lock) {
					int ret = fis.read(buffer);
					if (ret < 0) {
						p = PacketData.createEOT(nextSeqNumSender);
						sendToNetworkLayer(p,true);
						System.out.println("Last Packet in the window created " + p.getSeqNum());
						cache.add(p);
						fis.close();
						break;
					} else {
						p = PacketData.createPacket(nextSeqNumSender, new String(buffer, 0, ret));
						sendToNetworkLayer(p,false);
						System.out.println("Sending Packet with " + p.getSeqNum());
						cache.add(p);
						nextSeqNumSender++;
						totalPacketsSent++;
						totalBytesSent += p.getUDPdata().length;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
				System.out.println("Error ocuured while sending packet");
		}
	}

 private void monitorACKs() throws Exception {	
	 while (true) {
			try {
				m_networkLayer.setSocketTimeoutForPacket();
				PacketData ACKPackets = receiveFromNetworkLayer();
				if (ACKPackets.getType() == 2) {
					System.out.println("Ack for Last Packet with seq [" + ACKPackets.getSeqNum() + "] received. The current transaction is now complete");
					System.out.println("TOTAL # OF PACKETS SENT: " + totalPacketsSent);
					System.out.println("TOTAL # OF BYTES SENT: " + totalBytesSent);
			        System.out.println("TOTAL # OF PACKET RETRANSIMISSIONS: "+numberOfTimeouts);
			        System.out.println("TOTAL # OF PACKETS DROPPED: " + m_networkLayer.getNumberPacketDrops());
					break;
				}
				synchronized (lock) {
					int diff = ACKPackets.getSeqNum() - (base % 32) + 1;
					if (diff > 0) { //ignore duplicate acks
						base = base + diff; // update the base
						for (int i = 0; i < diff; i++) {
							System.out.println("Ack for Packet with seq [" + ACKPackets.getSeqNum() + "] received.");
							System.out.println("Removed Packet with seq [" + cache.get(i).getSeqNum() + "] from senders queue");
							cache.remove(i);
							if (cache.isEmpty()) {
								break;
							}
						}
					}
				}
			} catch (SocketTimeoutException ex) {
				synchronized (lock) {
					for (int i = 0; i < cache.size(); i++) {
						sendToNetworkLayer(cache.get(i),false);
						System.out.println("Ack for Packet with Seq [" + cache.get(i).getSeqNum() + "] timed out. Resending packet");
						 numberOfTimeouts++;
						 totalPacketsSent++;
						 totalBytesSent += cache.get(i).getUDPdata().length;
					}
				}
			}

		}
	}

 
	private  void selectiveRepeatSend(FileInputStream fis) throws Exception {
		segmentedFile = segmentation(fis);
		numberOfTimeouts = 0;
		timeoutTimer = new Timer(true);
		windowSender = new int[segmentedFile.length];
		Arrays.fill(windowSender, NAK);
		startWindowSender = 0;

		// send first window of packets
		for (int i = 0; i < WINDOW_SIZE; i++) {
			if (i < segmentedFile.length) {
				nextSeqNumSender = i %32;
				sendPacket(nextSeqNumSender, segmentedFile[i]);
			}
		}
		
	}

	private void monitorACKsSelectiveRepeat() throws Exception {
		corruptAcks = 0;
		while (true) {
			PacketData ACKPackets = receiveFromNetworkLayer();
			ackPackets(ACKPackets);
			System.out.println("Ack for Packet with seq [" + ACKPackets.getSeqNum() + "] received.");
			System.out.println("Removed Packet with seq [" + ACKPackets.getSeqNum() + "] from senders window");
			int windowMoved = adjustWindow();
			// send packets that are now in window
			for (int i = windowMoved; i > 0; i--) {
				sendPacket(startWindowSender + WINDOW_SIZE - i, segmentedFile[startWindowSender + WINDOW_SIZE - i]);
			}
			// check if all packets are acked and we are done
			if (allPacketsAcked()) {
				nextSeqNumSender++;
				PacketData p = PacketData.createEOT(nextSeqNumSender);
				sendToNetworkLayer(p, true);
				System.out.println("Last Packet in the window sent " + p.getSeqNum());
				
				System.out.println("TOTAL # OF PACKETS SENT: " + totalPacketsSent);
				System.out.println("TOTAL # OF BYTES SENT: " + totalBytesSent);
		        System.out.println("TOTAL # OF PACKET RETRANSIMISSIONS: "+numberOfTimeouts);
		        System.out.println("TOTAL # OF PACKETS DROPPED: " + m_networkLayer.getNumberPacketDrops());
		        System.out.println("TOTAL # OF CORRUPT ACKS RECEIVED: " + corruptAcks);
				break;
			}

		}
	}
	
	private static void ackPackets(PacketData pkt){	
        int seq = pkt.getSeqNum();
        String packetString = new String(pkt.getData());
        if(packetString.contains("Window")) {
        int index = packetString.indexOf("Window: ")+("Window: ".length());
        for(int i = seq; i < seq+WINDOW_SIZE; i++){
           int ack = Integer.parseInt(packetString.substring(index, index+1).trim());
           if(ack == ACK){
              windowSender[i] = ACK;
           }
           index++;
        }}else {
        	corruptAcks++;
        }
     }
     
	private void sendPacket(int seq, byte[] message) throws Exception {
		PacketData pkt = PacketData.createPacket(seq, new String(message));
		sendToNetworkLayer(pkt, false);
		totalPacketsSent++;
		totalBytesSent+=pkt.getUDPdata().length;
		timeoutTimer.schedule(new PacketTimeout(seq, message), TIME_OUT);
	}

	 private static boolean allPacketsAcked(){
         boolean allAcked = true;
         for(int i = 0; i < windowSender.length; i++){
            if(windowSender[i] == NAK)
               allAcked = false;
         }
         return allAcked;
      }
      
      private static int adjustWindow() throws Exception{
         int windowMoved = 0;
         while(true){
            if(windowSender[startWindowSender] == ACK){
               if(startWindowSender+WINDOW_SIZE < windowSender.length){
                  startWindowSender++;
                  windowMoved++;
               }
               else
                  break;  
            }
            else
               break;
         }
         return windowMoved;
      }
 
	 private static byte[][] segmentation(FileInputStream stream) throws Exception{
         int size = (int)Math.ceil((double)(stream.available())/(496));
         byte[][] segmentedFile = new byte[size][496];
         for(int i = 0; i < size; i++){
            for(int j = 0; j < 496; j++){
               if(stream.available() != 0)
                  segmentedFile[i][j] = (byte)stream.read();
               else
                  segmentedFile[i][j] = 0;
            }
         }
         stream.close();
         return segmentedFile;
      }
 
 
	public static int getCheckSum(int seqNumber, String payload) {
		return seqNumber + getNumber(payload);
	}

	private static int getNumber(String message) {
		int size = message.length();
		int sum = 0;
		for (int i = 0; i < size; i++) {
			Character c = message.charAt(i);
			sum += Character.getNumericValue(c);
		}
		return sum;
	}

	// Send EOT packet to NetworkLayer
	private void sendEOT(int seqNum) throws Exception {
		PacketData eot = PacketData.createEOT(seqNum);
		sendToNetworkLayer(eot, true);
	}

	// send the ACKs packet to NetworkLayer
	private void sendACK(int seqNum) throws Exception {
		PacketData ack = PacketData.createACK(seqNum,0);
		sendToNetworkLayer(ack, false);
		System.out.println("Sending Ack for Packet with Seq [" + seqNum + "]");
	}

	// use output stream to write packets' data to the specified file
	private void writeToFile(PacketData p, FileOutputStream fos) {
		try {
			System.out.println("Debug:Write to file " + p.getSeqNum());
			fos.write(p.getData());
		} catch (IOException e) {
			System.out.println("I/O exception while writing to file!!!");
		}
	}
	
	
    private static void ackPacket(int seqNum){
      	//ACK packet in window
         if(startWindowReceiver <= seqNum){
            if(seqNum-startWindowReceiver < WINDOW_SIZE)
               windowReceiver[seqNum-startWindowReceiver] = ACK;
         }
      }
      
      private  void adjustWindow(int seqNum){
      	//shift window
         while(true){
            if(windowReceiver[0] == ACK){
               for(int i = 0; i < WINDOW_SIZE-1; i++){
                  windowReceiver[i] = windowReceiver[i+1];
               }
               windowReceiver[WINDOW_SIZE-1] = NAK;
               startWindowReceiver++;
            }
            else
               break;
         }
      }
      
	private String sendAcknowledgement() throws Exception {
		String ack = ("Seq: " + startWindowReceiver + "  Window: ");
		for (int i = 0; i < WINDOW_SIZE; i++) {
			ack += windowReceiver[i];
		}
		ack += "\r\n";
		byte[] ackData = new byte[ack.length()];
		ackData = ack.getBytes();
		PacketData pkt = PacketData.createPacket(startWindowReceiver, new String(ackData));
		sendToNetworkLayer(pkt, false);
		return ack;
	}
   	
      private static void reassembleFile(List<byte[]> segmentedFile, List<Integer> segmentedFileOrder) throws Exception {
         FileOutputStream filestream = new FileOutputStream(new File("output"));
         for(int i = 0; i < segmentedFileOrder.size(); i++){
            int index = segmentedFileOrder.indexOf(i);
            String msg = new String(segmentedFile.get(index));
            String data0 = msg.replaceAll("\00", "");
            String data = data0.replaceAll("\01", "");
            byte[] byteData = new byte[data.length()];
            byteData = data.getBytes();
            filestream.write(byteData);
         }
         filestream.close();
      }
      
      
	private class PacketTimeout extends TimerTask{
        private int seq;
        private byte[] message;
     	
        public PacketTimeout(int seq, byte[] message){
           this.seq = seq;
           this.message = message;
        }
     	
        public void run(){
           //if packet has not been ACKed
           if(windowSender[seq] == 0){
              numberOfTimeouts++;
              System.out.println("Ack for Packet with Seq [" + seq + "] timed out. Resending packet");
              try{
                 sendPacket(seq, message);
              }
               catch (Exception e){}
           }
        }
     }

}
