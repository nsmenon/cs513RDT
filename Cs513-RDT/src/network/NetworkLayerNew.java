package network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

import dataLinkLayer.TransportLayerNew;

public class NetworkLayerNew {
	
	private InetAddress m_IPAddress;
	private DatagramSocket m_socket = null;
	private int m_localport = 0;
	private int m_remoteport = 0;
	private TransportLayerNew m_transportLayer = null;
	private byte[] packetBuffer = new byte[1024];// data array used to store the UDP data of the received packets.
	
	public NetworkLayerNew(int localport, int remoteport) {
		m_localport = localport;
		m_remoteport = remoteport;
		try {
			m_IPAddress = InetAddress.getByName("localhost");
			m_socket = new DatagramSocket(localport);
		} catch (Exception e) {
			System.out.println("Cannot create socket: " + e);
		}
	}
	
	public void setTransportLayer(TransportLayerNew tl) {
		m_transportLayer = tl;
	}
	
	public void setSocketTimeoutForPacket() throws SocketException {
		m_socket.setSoTimeout(TransportLayerNew.TIME_OUT);
	}
	
	public void send(byte[] payload, boolean noLoss) {
		if(!noLoss) {
		// simulate random loss of packet and packet corruption
		Random rand = new Random();
		int randnumForCorruption = rand.nextInt(10); // range 0-10
		int randnumForPacketDrop = rand.nextInt(10); // range 0-10
		/*if (randnumForCorruption < 3) {
			payload = ("Garbage value").getBytes();
		}*/
		if (randnumForPacketDrop < 3) {
			return;
		}
		}
		try {
			DatagramPacket p = new DatagramPacket(payload, payload.length, m_IPAddress, m_remoteport);
			m_socket.send(p);
		} catch (Exception e) {
			System.out.println("Error sending packet: " + e);
		}
	}

	// Interface provided to the data link layer to pick
	// up message received
	public byte[] receive() throws SocketTimeoutException,Exception {
		DatagramPacket receiverPacket = new DatagramPacket(packetBuffer, packetBuffer.length);

		m_socket.receive(receiverPacket);

		return packetBuffer;
	}

}
