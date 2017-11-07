package application;

public class AppLayerObject {
	private int packetCorruptionProbability;
	private int packetDropProbability;
	private int protocolMode;

	public AppLayerObject(int packetCorruptionProbability, int packetDropProbability, int protocolMode) {
		this.packetCorruptionProbability = packetCorruptionProbability;
		this.packetDropProbability = packetDropProbability;
		this.protocolMode = protocolMode;
	}

	public int getPacketDropProbability() {
		return packetDropProbability;
	}

	public void setPacketDropProbability(int packetDropProbability) {
		this.packetDropProbability = packetDropProbability;
	}

	public int getProtocolMode() {
		return protocolMode;
	}

	public void setProtocolMode(int protocolMode) {
		this.protocolMode = protocolMode;
	}

	public int getPacketCorruptionProbability() {
		return packetCorruptionProbability;
	}

	public void setPacketCorruptionProbability(int packetCorruptionProbability) {
		this.packetCorruptionProbability = packetCorruptionProbability;
	}
}
