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
	 final Map<Character, Integer> distanceVector;
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
		for (Character node : this.distanceVector.keySet()) {
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
			for (Character node : this.distanceVector.keySet()) {
				try {
				Integer cost = g.getDistance(this.nodeID, node);
				if (cost == null) {
					cost = Integer.MAX_VALUE;
				}
				//new cost = cost to get to [node] from via node, plus cost to get to via node
				Integer newCost = this.distanceVector.get(node) + g.getDistance(this.nodeID, this.nodeID);
				
				if (cost > newCost) {
					g.updateDistance(this.nodeID, node, newCost);
					this.updated = true;
				}
				} catch (InputMismatchException e) {
					//it will fail for getting its nodeID from knownNodes
					//e.printStackTrace();
				} catch (NullPointerException e) {
					//catches some cases where g.getDistance has not been set yet
					//do nothing, later DVs will fill it in and be able to continue
					
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
