import java.io.Serializable;



public class HeartBeat implements Sendable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final char fromNodeID;
	
	public HeartBeat(char nodeID) {
		this.fromNodeID = nodeID;
	}
	
	public char getNodeID() {
		return this.fromNodeID;
	}

}
