package application;
public class AppLayerObject {
	private Integer maxSegmentSize = 4;
	private Double failureProbability = 0.1;
	private Integer WINDOW_SIZE = 2;
	private Integer TIMER = 30;

	public Integer getMaxSegmentSize() {
		return maxSegmentSize;
	}

	public AppLayerObject(Integer maxSegmentSize, Double failureProbability, Integer wINDOW_SIZE, Integer tIMER) {
		this.maxSegmentSize = maxSegmentSize;
		this.failureProbability = failureProbability;
		WINDOW_SIZE = wINDOW_SIZE;
		TIMER = tIMER;
	}

	public void setMaxSegmentSize(Integer maxSegmentSize) {
		this.maxSegmentSize = maxSegmentSize;
	}

	public Double getFailureProbability() {
		return failureProbability;
	}

	public void setFailureProbability(Double failureProbability) {
		this.failureProbability = failureProbability;
	}

	public Integer getWINDOW_SIZE() {
		return WINDOW_SIZE;
	}

	public void setWINDOW_SIZE(Integer wINDOW_SIZE) {
		WINDOW_SIZE = wINDOW_SIZE;
	}

	public Integer getTIMER() {
		return TIMER;
	}

	public void setTIMER(Integer tIMER) {
		TIMER = tIMER;
	}

}
