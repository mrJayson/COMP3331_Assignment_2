import java.util.InputMismatchException;


public class ConnectionSignal implements Message {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean connection;
	private char nodeID;
	
	public ConnectionSignal (boolean connection, char nodeID) {
		this.connection = connection;
		this.nodeID = nodeID;
	}

	@Override
	public void execute(Graph g) {
		if (this.connection == true) {
			//connect
			try {
			g.connectAdjacentNode(nodeID);
			} catch (InputMismatchException e) {
				//connection signal to connect node may have come
				//from multiple sources at once
				//so only the first will run
				//the rest will fall in here
			}
		}
		else if (this.connection == false) {
			//disconnect
			g.disconnectAdjacentNode(nodeID);
			
		}

	}

}
