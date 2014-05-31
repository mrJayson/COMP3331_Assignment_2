import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;


public class DistanceVector implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final char nodeID;
	private final Map<Character, Integer> distanceVector;
	private boolean updated;

	public DistanceVector(char nodeID, Map<Character, Integer> distanceVector) {
		this.nodeID = nodeID;
		this.distanceVector = distanceVector;
		this.updated = false;
	}

	public char getNodeID() {
		return this.nodeID;
	}
	
	public boolean isUpdated() {
		return this.updated;
	}

	@Override
	public void execute(Graph g) {
		//given a DV, update g accordingly
		//if there are any new nodes from DV, add them into the graph
		for (Character node : distanceVector.keySet()) {
			try {
				g.addKnownNode(node);
			} catch (InputMismatchException e) {
				//added an existing node in,
				//skip over it
			}
		}

		try {
			//the DV is from the perspective of nodeID
			//and updates will affect that viaNode column only
			for (Character node : distanceVector.keySet()) {
				try {
				Integer cost = g.getDistance(nodeID, node);
				if (cost == null) {
					cost = Integer.MAX_VALUE;
				}
				//new cost = cost to get to [node] from via node, plus cost to get to via node
				Integer newCost = distanceVector.get(node) + g.getDistance(this.nodeID, this.nodeID);
				
				if (cost > newCost) {
					g.updateDistance(nodeID, node, newCost);
					this.updated = true;
				}
				} catch (InputMismatchException e) {
					//it will fail for getting its nodeID from knownNodes
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
