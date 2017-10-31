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

		// System.out.println("Enter the Maximum Segement size. Max allowed value is
		// 10");
		// Integer mssSize = scanInputValues.nextInt();

		// System.out.println("Probability of loss during packet sending. Value ranges
		// from (0.1 - 1)");
		// Double probability = scanInputValues.nextDouble();

		System.out.println("Window size - Number of packets sent without acking");
		Integer windowSize = scanInputValues.nextInt();

		// System.out.println("Time (ms) before Resending all the non-acked packets");
		// Integer timeout = scanInputValues.nextInt();

		// AppLayerObject appObject = new AppLayerObject(mssSize, probability,
		// windowSize, timeout);
		AppLayerObject appObject = new AppLayerObject(null, null, windowSize, null);
		boolean flag = true;
		while (flag) {
			System.out.println("Are you going to send or receive file now. Press 1 to send or 0 to receive");
			Integer mode = scanInputValues.nextInt();
			switch (mode) {
			case 1:
				flag = false;
				sendMode(appObject);
				break;

			case 0:
				flag = false;
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
			tpLayer.dataLinkSend(fpStream);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void receiveMode(AppLayerObject appObject) {
		try {
			TransportLayerNew tpLayer = new TransportLayerNew(appObject, PORT_ONE, PORT_TWO);
			FileOutputStream fos = new FileOutputStream("output.txt");
			tpLayer.dataLinkReceive(fos);
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
