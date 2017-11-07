package network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

import application.AppLayerObject;
import dataLinkLayer.TransportLayerNew;
import packet.PacketData;

public class NetworkLayerNew {
	
	private InetAddress m_IPAddress;
	private DatagramSocket m_socket = null;
	private int m_localport = 0;
	private int m_remoteport = 0;
	private TransportLayerNew m_transportLayer = null;
	private byte[] packetBuffer = new byte[1024];// data array used to store the UDP data of the received packets.
	private int packetCorruptionProbability;
	private int packetDropProbability;
	private int numberPacketDrops = 0;
	private Random rand;
	
	public NetworkLayerNew(AppLayerObject appObject,int localport, int remoteport) {
		m_localport = localport;
		m_remoteport = remoteport;
		this.packetCorruptionProbability = appObject.getPacketCorruptionProbability();
		this.packetDropProbability = appObject.getPacketDropProbability();
		rand = new Random();
		try {
			m_IPAddress = InetAddress.getByName("localhost");
			m_socket = new DatagramSocket(localport);
		} catch (Exception e) {
			System.out.println("Cannot create socket: " + e);
		}
	}
	
	public DatagramSocket getM_socket() {
		return m_socket;
	}

	public void setM_socket(DatagramSocket m_socket) {
		this.m_socket = m_socket;
	}

	public void setTransportLayer(TransportLayerNew tl) {
		m_transportLayer = tl;
	}
	
	public void setSocketTimeoutForPacket() throws SocketException {
		m_socket.setSoTimeout(TransportLayerNew.TIME_OUT);
	}
	
	public void send(PacketData packet, boolean noLoss){
		if (!noLoss) {
			// simulate random loss of packet and packet corruption
			
			int randnumForCorruption = rand.nextInt(100); // range 0-10
			int randnumForPacketDrop = rand.nextInt(100); // range 0-10
			
			if (randnumForCorruption < packetCorruptionProbability) {
				System.out.println("** Drop Corrupt Number ***"+randnumForCorruption);
				try {
					PacketData pkt = PacketData.createCorruptPacket(packet.getSeqNum(), "ASFGGHHJBNHJ");
					DatagramPacket p = new DatagramPacket(pkt.getUDPdata(), pkt.getUDPdata().length, m_IPAddress, m_remoteport);
					m_socket.send(p);
					return;
				} catch (Exception e) {
					System.out.println("Error sending packet: " + e);
				}
			}
			if (randnumForPacketDrop < packetDropProbability) {
				System.out.println("** Drop Random Number ***"+randnumForPacketDrop);
				numberPacketDrops++;
				return;
			}
		}
		try {
			DatagramPacket p = new DatagramPacket(packet.getUDPdata(), packet.getUDPdata().length, m_IPAddress, m_remoteport);
			m_socket.send(p);
		} catch (Exception e) {
			System.out.println("Error sending packet: " + e);
		}
	}

	public int getNumberPacketDrops() {
		return numberPacketDrops;
	}

	public void setNumberPacketDrops(int numberPacketDrops) {
		this.numberPacketDrops = numberPacketDrops;
	}

	// Interface provided to the data link layer to pick
	// up message received
	public byte[] receive() throws SocketTimeoutException,Exception {
		DatagramPacket receiverPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
		m_socket.receive(receiverPacket);
		return packetBuffer;
	}

}
