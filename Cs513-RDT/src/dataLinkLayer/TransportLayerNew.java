package dataLinkLayer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import application.AppLayerObject;
import network.NetworkLayerNew;
import packet.PacketData;


public class TransportLayerNew {
	private static NetworkLayerNew m_networkLayer = null;
	private static int base = 0;
	private static int nextSeqNumSender = 0;
	private static int nextSeqNumReceiver = 0;

	private static int windowSize = 10;
	public final static int TIME_OUT = 5000;

	// use a list to cache the packets sent but unACKed
	private static List<PacketData> cache = new LinkedList<PacketData>();

	// lock used to keep mutual exclusive
	private static Object lock = new Object();

	private static FileOutputStream seqnum, ack, arrival;

	public TransportLayerNew(AppLayerObject appObject, int senderPort, int receiverPort) {
		m_networkLayer = new NetworkLayerNew(senderPort, receiverPort);
		m_networkLayer.setTransportLayer(this);
		 windowSize = appObject.getWINDOW_SIZE();
	}

	public void dataLinkSend(final FileInputStream tobeSentStream) throws Exception {
		seqnum = new FileOutputStream("seqnum.log");
		ack = new FileOutputStream("ack.log");
		threadsForSender(tobeSentStream);
		seqnum.close();
		ack.close();
	}

	// API provided to upper layer (application layer) to
	// receive a message. 
	public synchronized void dataLinkReceive(FileOutputStream fos) throws Exception {
		arrival = new FileOutputStream("arrival.log");
		int endOfFile = 0;
		while (endOfFile != 1) {
			PacketData packetReceived = receiveFromNetworkLayer();
			System.out.println("Arrival " + packetReceived.getSeqNum());
			arrival.write((Integer.toString(packetReceived.getSeqNum()) + '\n').getBytes());
			switch (packetReceived.getType()) {
			case 1:
				// length of the data packets should be larger than 0
				if (packetReceived.getLength() <= 0) {
					System.out.println("Invalid packet length for data received!!!");
					break;
				} else {
					long checkSumFromreceivedData = calculateCheckSum(packetReceived.getData());
					if (packetReceived.getSeqNum() == nextSeqNumReceiver && packetReceived.getCheckSum() == checkSumFromreceivedData) {
						writeToFile(packetReceived, fos);
						System.out.println("Debug:ACK sent for Packet with Seq [" +packetReceived.getSeqNum() + "]");
						sendACK(nextSeqNumReceiver % 32);
						nextSeqNumReceiver++;
						break;
					} else {
						System.out.println("Either packet is corrupted or an out of Order Packet Received.Discarded, sending last correctly recived Packet ack");
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
			}
		}
	}

	private static void threadsForSender(final FileInputStream fis) throws InterruptedException {
		Thread sendPacketsThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					sendPackets(fis, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// thread used to receive ACKs sent by receiver and process them
		Thread monitorACKsThread = new Thread(new Runnable() {
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

	}

	public static PacketData receiveFromNetworkLayer() throws SocketTimeoutException,Exception {
		byte[] receivedData = m_networkLayer.receive();
		if (null == receivedData)
			return null;
		PacketData ACKPackets = PacketData.parseUDPdata(receivedData);
		return ACKPackets;
	}

	public static void sendToNetworkLayer(PacketData packet, boolean noLoss) {
		m_networkLayer.send(packet.getUDPdata(),noLoss);
	}

	private static void sendPackets(FileInputStream fis, int i) {
		try {

			PacketData p;
			byte[] buffer = new byte[500];

			while (true) {
				if (nextSeqNumSender >= base + windowSize)
					continue;
				// here should be mutex
				synchronized (lock) {
					int ret = fis.read(buffer);
					if (ret < 0) {
						p = PacketData.createEOT(nextSeqNumSender);
						sendToNetworkLayer(p,true);
						System.out.println("Sending Complete. Sending window closed " + p.getSeqNum());
						cache.add(p);
						seqnum.write((Integer.toString(nextSeqNumSender) + '\n').getBytes());
						fis.close();
						break;
					} else {
						p = PacketData.createPacket(nextSeqNumSender, new String(buffer, 0, ret));
						sendToNetworkLayer(p,false);
						System.out.println("Sending Packet with " + p.getSeqNum());
						cache.add(p);
						seqnum.write((Integer.toString(nextSeqNumSender) + '\n').getBytes());
						nextSeqNumSender++;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
				System.out.println("Error ocuured while sending packet");
		}
	}
	
	private static long calculateCheckSum(byte bytes[]) {
		Checksum checksum = new CRC32();
		checksum.update(bytes, 0, bytes.length);
		return checksum.getValue();
	}

 private static void monitorACKs() throws Exception {
		while (true) {
			try {
				m_networkLayer.setSocketTimeoutForPacket();
				PacketData ACKPackets = receiveFromNetworkLayer();
				if (ACKPackets.getType() == 2) {
					ack.write((Integer.toString(ACKPackets.getSeqNum()) + '\n').getBytes());
					break;
				}
				synchronized (lock) {
					int diff = ACKPackets.getSeqNum() - (base % 32) + 1;
					if (diff > 0) { 
						ack.write((Integer.toString(ACKPackets.getSeqNum()) + '\n').getBytes());
						base = base + diff; // update the base
						for (int i = 0; i < diff; i++) {
							System.out.println("Removed Packet with seq [" + cache.get(i).getSeqNum() + " ] from senders queue");
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
						seqnum.write((Integer.toString(cache.get(i).getSeqNum()) + '\n').getBytes());
						System.out.println("Ack for Packet with Seq [" + cache.get(i).getSeqNum() + "] timed out. Resending packet");
					}
				}
			}

		}
	}

	// Send EOT packet to NetworkLayer
	private void sendEOT(int seqNum) throws Exception {
		PacketData eot = PacketData.createEOT(seqNum);
		sendToNetworkLayer(eot, true);
	}

	// send the ACKs packet to NetworkLayer
	private void sendACK(int seqNum) throws Exception {
		PacketData ack = PacketData.createACK(seqNum);
		sendToNetworkLayer(ack, true);
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

}
