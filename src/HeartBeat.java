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
	
	public Boolean checkConnection (Graph g) {
		Boolean action = null;
		
		if (!g.connected(fromNodeID)) {
			//received heartBeat from a disconnected node
			action = true;
		}
		
		return action;
	}

}
