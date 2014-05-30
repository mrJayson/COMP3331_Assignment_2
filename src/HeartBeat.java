

public class HeartBeat implements Message {
	
	private char fromNodeID;
	
	public HeartBeat(char nodeID) {
		this.fromNodeID = nodeID;
	}
	
	public char getNodeID() {
		return this.fromNodeID;
	}

	@Override
	public void execute() {
	}

}
