package packet;

// common packet class used by both SENDER and RECEIVER

import java.nio.ByteBuffer;

public class PacketData {
	
	// constants
	private final int maxDataLength = 496;
	private final int SeqNumModulo = 32;
	
	// data members
	private int type;
	private int seqnum;
	private String data;
	private int checkSum;
	
	//////////////////////// CONSTRUCTORS //////////////////////////////////////////
	
	// hidden constructor to prevent creation of invalid packets
	private PacketData(int Type, int SeqNum, String strData, int checkSumValue) throws Exception {
		// if data seqment larger than allowed, then throw exception
		if (strData.length() > maxDataLength)
			throw new Exception("data too large (max 500 chars)");
			
		type = Type;
		seqnum = SeqNum % SeqNumModulo;
		data = strData;
		checkSum = checkSumValue;
	}
	
	// special packet constructors to be used in place of hidden constructor
	public static PacketData createACK(int SeqNum) throws Exception {
		return new PacketData(0, SeqNum, new String(),0);
	}
	
	public static PacketData createPacket(int SeqNum, String data) throws Exception {
		return new PacketData(1, SeqNum, data,getCheckSum(SeqNum,data));
	}
	
	public static PacketData createEOT(int SeqNum) throws Exception {
		return new PacketData(2, SeqNum, new String(),0);
	}
	
	///////////////////////// PACKET DATA //////////////////////////////////////////
	
	public int getType() {
		return type;
	}
	
	public int getSeqNum() {
		return seqnum;
	}
	
	public int getLength() {
		return data.length();
	}
	
	public byte[] getData() {
		return data.getBytes();
	}
	public String getDataAsString() {
		return data;
	}
	
	public int getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(int checkSum) {
		this.checkSum = checkSum;
	}
	
	//////////////////////////// UDP HELPERS ///////////////////////////////////////
	
	public byte[] getUDPdata() {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.putInt(type);
        buffer.putInt(seqnum);
        buffer.putInt(checkSum);
        buffer.putInt(data.length());
        buffer.put(data.getBytes(),0,data.length());
		return buffer.array();
	}
	
	public static PacketData parseUDPdata(byte[] UDPdata) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
		int type = buffer.getInt();
		int seqnum = buffer.getInt();
		int checkSum = buffer.getInt();
		int length = buffer.getInt();
		byte data[] = new byte[length];
		buffer.get(data, 0, length);
		return new PacketData(type, seqnum, new String(data),checkSum);
	}
	
	public static int getCheckSum(int seqNumber,String payload) {
		return seqNumber + getNumber(payload);
	}

	private static int getNumber(String message) {
		int size = message.length();
		int sum = 0;
		for (int i = 0; i < size; i++){
			Character c = message.charAt(i);
			sum += Character.getNumericValue(c);
		}
		return sum;
	}
}