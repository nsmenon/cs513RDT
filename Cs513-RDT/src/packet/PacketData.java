package packet;

// common packet class used by both SENDER and RECEIVER

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class PacketData {
	
	// constants
	private final int maxDataLength = 500;
	private final int SeqNumModulo = 32;
	
	// data members
	private int type;
	private int seqnum;
	private String data;
	private long checkSum;
	
	//////////////////////// CONSTRUCTORS //////////////////////////////////////////
	
	// hidden constructor to prevent creation of invalid packets
	private PacketData(int Type, int SeqNum, String strData, long checkSumValue) throws Exception {
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
		return new PacketData(0, SeqNum, new String(),0l);
	}
	
	public static PacketData createPacket(int SeqNum, String data) throws Exception {
		return new PacketData(1, SeqNum, data,calculateCheckSum(data.getBytes()));
	}
	
	public static PacketData createEOT(int SeqNum) throws Exception {
		return new PacketData(2, SeqNum, new String(),0l);
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
	
	public long getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(long checkSum) {
		this.checkSum = checkSum;
	}
	
	//////////////////////////// UDP HELPERS ///////////////////////////////////////
	
	public byte[] getUDPdata() {
		ByteBuffer buffer = ByteBuffer.allocate(520);
		buffer.putInt(type);
        buffer.putInt(seqnum);
        buffer.putLong(checkSum);
        buffer.putInt(data.length());
        buffer.put(data.getBytes(),0,data.length());
		return buffer.array();
	}
	
	public static PacketData parseUDPdata(byte[] UDPdata) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
		int type = buffer.getInt();
		int seqnum = buffer.getInt();
		long checkSum = buffer.getLong();
		int length = buffer.getInt();
		byte data[] = new byte[length];
		buffer.get(data, 0, length);
		return new PacketData(type, seqnum, new String(data),checkSum);
	}
	
	private static long calculateCheckSum(byte bytes[]) {
		Checksum checksum = new CRC32();
		checksum.update(bytes, 0, bytes.length);
		return checksum.getValue();
	}
}