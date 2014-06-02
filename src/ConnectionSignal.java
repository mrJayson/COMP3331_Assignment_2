import java.util.InputMismatchException;


public class ConnectionSignal implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final boolean connection;
	private final char nodeID;
	private boolean relayable;

	public ConnectionSignal (boolean connection, char nodeID) {
		this.connection = connection;
		this.nodeID = nodeID;
		this.relayable = false;
	}
	
	public boolean connection() {
		return this.connection;
	}
	
	@Override
	public void execute(Graph g) {
		if (this.connection == true) {
			//connect
			try {
				g.connectAdjacentNode(nodeID);
				this.relayable = true;
			} catch (InputMismatchException e) {
				//connection signal to connect node may have come
				//from multiple sources at once
				//so only the first will run
				//the rest will fall in here
			}
		}
		else if (this.connection == false) {
			//disconnect
			//connection == false is only sent to other nodes
			//ConnectionSignal with connection == true is never relayed to other nodes
			if (g.isAdjacent(nodeID)) {
				try {
					g.disconnectAdjacentNode(nodeID);
					this.relayable = true;
				} catch (InputMismatchException e) {
					this.relayable = false;
					//catching duplicate disconnects
					//do not continue relaying if this is duplicate
				}
			} else {
				try {
				g.deleteKnownNode(nodeID);
				this.relayable = true;
				} catch (InputMismatchException e) {
					this.relayable = false;
					//catching duplicate disconnects
					//do not continue relaying if this is duplicate
				}
			}
			
		}

	}
	
	public char node() {
		return this.nodeID;
	}

	public boolean relayable () {
		return this.relayable;
	}

}
