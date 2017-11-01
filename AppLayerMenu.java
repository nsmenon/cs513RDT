import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Scanner;

import application.AppLayerObject;
import dataLinkLayer.TransportLayerNew;

public class AppLayerMenu {

	public static final int PORT_ONE = 9888;
	public static final int PORT_TWO = 9887;

	public static void main(String[] args) {

		Scanner scanInputValues = new Scanner(System.in);
		System.out.println("RDP Portal Initiation");
		
		AppLayerObject appObject = null;
		boolean flag = true;
		while (flag) {
			System.out.println("Protocol to used. [0] GBN, [1] SR");
			int protMode = scanInputValues.nextInt();
			
			System.out.println("Are you going to send or receive file now. Press 1 to send or 0 to receive");
			int mode = scanInputValues.nextInt();
			
			System.out.println("Window size - Number of packets that can be sent without acknowledgemnet");
			Integer windowSize = scanInputValues.nextInt();
			
			System.out.println("Probability of loss during packet sending.Range - [0..100]%");
			int packetLossProbability = scanInputValues.nextInt();

			System.out.println("Probability of corruption for packet.Range - [0..100]%");
			int packetcorruptionProbability = scanInputValues.nextInt();
			
			switch (mode) {
			case 1:
				flag = false;
				appObject = new AppLayerObject(packetcorruptionProbability, packetLossProbability, windowSize, protMode);
				sendMode(appObject);
				break;

			case 0:
				flag = false;
				appObject = new AppLayerObject(packetcorruptionProbability, packetLossProbability,0, protMode);
				receiveMode(appObject);
				break;
			default:
				break;
			}
		}

	}

	private static void sendMode(AppLayerObject appObject) {
		try {
			TransportLayerNew tpLayer = new TransportLayerNew(appObject, PORT_TWO, PORT_ONE);
			FileInputStream fpStream = new FileInputStream("testfile");
			tpLayer.dataLinkSend(fpStream, appObject.getProtocolMode());
			fpStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void receiveMode(AppLayerObject appObject) {
		try {
			TransportLayerNew tpLayer = new TransportLayerNew(appObject, PORT_ONE, PORT_TWO);
			FileOutputStream fos = new FileOutputStream("output.txt");
			tpLayer.dataLinkReceive(fos,appObject.getProtocolMode());
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
