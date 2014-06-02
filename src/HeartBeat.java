

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
	
	public boolean newConnection (Model g) {
		boolean newConnection;
		if (g.connected(fromNodeID)) {
			//already connected, reset missedBeats counter
			g.resetMissedBeats(fromNodeID);
			newConnection = false;
		}
		else {
			//received heartBeat from a disconnected node
			newConnection = true;
		}
		return newConnection;
	}

}
