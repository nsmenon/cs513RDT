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
			int packetLossProbability = 0;
			int packetcorruptionProbability = 0;
			
			
			if(!(protMode == 1 && mode == 0)) {
			System.out.println("Probability of loss during packet sending.Range - [0..100]%");
			 packetLossProbability = scanInputValues.nextInt();

			System.out.println("Probability of corruption for packet.Range - [0..100]%");
			packetcorruptionProbability = scanInputValues.nextInt();
			
			}else {
				packetLossProbability =  0;
				packetcorruptionProbability = 0;
			}

			switch (mode) {
			case 1:
				appObject = new AppLayerObject(packetcorruptionProbability, packetLossProbability,protMode);
				sendMode(appObject);
				System.out.println("Sending Complete...");
				break;
			case 0:
				appObject = new AppLayerObject(packetcorruptionProbability, packetLossProbability,protMode);
				receiveMode(appObject);
				System.out.println("File received successfully...");
				break;
			default:
				break;

			}
		}

		
	}

	private static void sendMode(AppLayerObject appObject) {
		try {
			Scanner scanInputValues = new Scanner(System.in);
			System.out.println("Enter the name of file you wish to send");
			String fileName = scanInputValues.next();
			TransportLayerNew tpLayer = new TransportLayerNew(appObject, PORT_TWO, PORT_ONE);
			FileInputStream fpStream = new FileInputStream(fileName);
			tpLayer.dataLinkSend(fpStream, appObject.getProtocolMode());
			fpStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void receiveMode(AppLayerObject appObject) {
		try {
			TransportLayerNew tpLayer = new TransportLayerNew(appObject, PORT_ONE, PORT_TWO);
			FileOutputStream fos = new FileOutputStream("output");
			tpLayer.dataLinkReceive(fos, appObject.getProtocolMode());
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
