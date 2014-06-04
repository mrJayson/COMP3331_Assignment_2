
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;



public class Model implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Map<Character, BigDecimal> allAdjacentNodes;
	private final List<Character> connectedAdjacentNodes;
	private final Map<Character, Integer> connectionStatus;
	private final List<Character> disconnectedAdjacentNodes;
	//distTable is a 2d map so can map char,char to a distance
	//first char is the via node, second char is the to node
	private final Map<Character, BigDecimal> distanceVector;
	//List of all the nodes this node is aware of
	private final Map<Character, Map<Character, BigDecimal>> distTable;
	private final List<Character> knownNodes;
	private final int missedBeatsLimit = 3;
	private final Map<Character, BigDecimal> nodeUpdateCost;
	private final boolean poisonReversed;
	private final char thisNodeID;
	private boolean updated;

	public Model(char nodeID, boolean poisonReversed) {
		this.thisNodeID = nodeID;
		this.allAdjacentNodes = new HashMap<Character, BigDecimal>();
		this.nodeUpdateCost = new HashMap<Character, BigDecimal>();
		this.disconnectedAdjacentNodes = new ArrayList<Character>();
		this.connectedAdjacentNodes = new ArrayList<Character>();
		this.knownNodes = new ArrayList<Character>();
		this.distTable = new HashMap<Character, Map<Character, BigDecimal>>();
		this.distanceVector = new HashMap<Character, BigDecimal>();
		this.connectionStatus = new HashMap<Character, Integer>();
		this.poisonReversed = poisonReversed;
		this.updated = false;
	}

	public void addAdjacentNode(Character nodeID, BigDecimal directCost, BigDecimal updateCost) throws InputMismatchException {
		if (this.allAdjacentNodes.containsKey(nodeID) 
				|| this.disconnectedAdjacentNodes.contains(nodeID) 
				|| this.connectedAdjacentNodes.contains(nodeID)) {
			throw new InputMismatchException();
		}
		//add node with cost
		this.allAdjacentNodes.put(nodeID, directCost);
		this.nodeUpdateCost.put(nodeID, updateCost);
		//newly added adjacent nodes start as disconnected
		this.disconnectedAdjacentNodes.add(nodeID);
	}
	public void addConnectionStatus(Character nodeID) {
		if (!this.allAdjacentNodes.containsKey(nodeID) 
				|| !this.disconnectedAdjacentNodes.contains(nodeID) 
				|| this.connectedAdjacentNodes.contains(nodeID) 
				|| this.connectionStatus.containsKey(nodeID)) {
			throw new InputMismatchException();
		}

		this.connectionStatus.put(nodeID, 0);
	}

	public void addKnownNode(Character nodeID) throws InputMismatchException {
		if (nodeID.equals(this.thisNodeID) || this.knownNodes.contains(nodeID) || this.distanceVector.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.knownNodes.add(nodeID);
		Collections.sort(this.knownNodes);
		this.distanceVector.put(nodeID, null);
		//adding a new entry to distanceVector means there is no min yet
	}

	public int checkConnectionStatus(Character nodeID) {
		if (!this.connectionStatus.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		return this.connectionStatus.get(nodeID);
	}

	public void connectAdjacentNode(Character nodeID) throws InputMismatchException {
		if (!this.disconnectedAdjacentNodes.contains(nodeID) 	//must be a disconnected node to connect
				|| this.connectedAdjacentNodes.contains(nodeID)) {//safety check
			throw new InputMismatchException();
		}
		addConnectionStatus(nodeID);
		this.connectedAdjacentNodes.add(nodeID);		//add to one
		Collections.sort(this.connectedAdjacentNodes);
		this.disconnectedAdjacentNodes.remove(nodeID);	//remove from the other
		this.distTable.put(nodeID, new HashMap<Character, BigDecimal>());
		//once the adjacent node is connected, it becomes known in the network
		try {
			addKnownNode(nodeID);
		} catch (InputMismatchException e) {
			//Catch adding in an already known node
			//which is generally trying to add itself as known
		}
		//add direct cost to distTable
		//since all nodes at this point are adjacent nodes, the via node and to node are the same
		updateDistance(nodeID, nodeID, this.allAdjacentNodes.get(nodeID));
	}

	public boolean connected(Character nodeID) throws InputMismatchException {
		if (!this.allAdjacentNodes.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		return this.connectedAdjacentNodes.contains(nodeID);
	}

	public void deleteKnownNode(Character nodeID) throws InputMismatchException {
		if (!this.knownNodes.contains(nodeID) || !this.distanceVector.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		for (Character adjacent : this.connectedAdjacentNodes) {
			this.distTable.get(adjacent).remove(nodeID);
		}
		this.knownNodes.remove(nodeID);
		this.distanceVector.remove(nodeID);
		//deleting from distanceVector means the min entry is deleted without side-effects
	}

	public void disconnectAdjacentNode(Character nodeID) throws InputMismatchException {
		if (!this.connectedAdjacentNodes.contains(nodeID) || !this.distTable.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		removeConnectionStatus(nodeID);
		this.connectedAdjacentNodes.remove(nodeID);	//remove from one
		this.disconnectedAdjacentNodes.add(nodeID);	//add to the other
		this.distTable.remove(nodeID);
		for (Character adjacent : this.connectedAdjacentNodes) {
			//remove entries in other rows
			this.distTable.get(adjacent).remove(nodeID);
		}
		//all adjacent nodes are in knownNodes, so delete there too
		try {
			deleteKnownNode(nodeID);
		} catch (InputMismatchException e) {
			e.printStackTrace();
		}
		updateDV();
	}

	public List<Character> getDeadConnections() {
		List<Character> deadConnections = new ArrayList<Character>();
		for (Character connection : this.connectionStatus.keySet()) {
			if (this.connectionStatus.get(connection) >= this.missedBeatsLimit) {
				deadConnections.add(connection);
			}
		}
		return deadConnections;
	}

	public List<Character> getDisconnectedNodes() {
		return this.disconnectedAdjacentNodes;
	}

	public BigDecimal getDistance(Character viaNode, Character toNode) {
		if (!this.connectedAdjacentNodes.contains(viaNode) || !this.knownNodes.contains(toNode)) {
			throw new InputMismatchException();
		}
		//will return null if there is no cost yet
		return this.distTable.get(viaNode).get(toNode);
	}

	public DistanceVector getDV() {
		return new DistanceVector(this.thisNodeID, this.distanceVector);
	}

	public void incrementMissedBeat() {
		for (Character connection : this.connectionStatus.keySet()) {
			this.connectionStatus.put(connection, this.connectionStatus.get(connection)+1);
		}
	}

	public boolean isAdjacent(Character nodeID) {
		return this.allAdjacentNodes.containsKey(nodeID);
	}

	public void printDebug() {
		System.out.println("AllAdjacentNodes: "+this.allAdjacentNodes);
		System.out.println("nodeUpdateCost: "+this.nodeUpdateCost);
		System.out.print("DisconnectedAdjacentNodes: ");
		for (Character c : this.disconnectedAdjacentNodes) {
			System.out.print("|"+c);
		}
		System.out.println();
		System.out.print("ConnectedAdjacentNodes: ");
		for (Character c : this.connectedAdjacentNodes) {
			System.out.print("|"+c);
		}
		System.out.println();
		System.out.print("knownNodes: ");
		for (Character c : this.knownNodes) {
			System.out.print("|"+c);
		}
		System.out.println();
		System.out.println("distanceVector: "+this.distanceVector);
		System.out.println("distTable: "+this.distTable);


	}

	public void printDT() {
		System.out.println("----------------------------------");
		System.out.println("Distance Table");
		System.out.println("----------------------------------");
		System.out.printf("\t");
		for (char c : this.connectedAdjacentNodes) {
			System.out.printf("%c\t", c);
		}
		System.out.println();

		for (char known : this.knownNodes) {
			System.out.printf("%c\t", known);
			for (char adjacent : this.connectedAdjacentNodes) {
				System.out.printf("%s\t", this.distTable.get(adjacent).get(known).stripTrailingZeros().toPlainString());
			}
			System.out.println();
		}
		System.out.println("----------------------------------");
	}

	public void printDV() {
		System.out.println("----------------------------------");
		System.out.println("Distance Vectors");
		System.out.println("----------------------------------");
		System.out.println("To\t|\tVia\t|\tCost");
		BigDecimal min;
		Character nextHop;
		for (Character known : this.knownNodes) {
			min = BigDecimal.valueOf(Integer.MAX_VALUE);
			nextHop = null;
			for (char adjacent : this.connectedAdjacentNodes) {
				try {
					if (min.compareTo(this.distTable.get(adjacent).get(known)) > 0) {
						min = this.distTable.get(adjacent).get(known);
						nextHop = adjacent;
					}
				} catch (NullPointerException e) {
					//nulls represent infinite cost
					//just catch and do nothing
				}
			}
			if (min.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) == 0) {
				//means there is no min
				min = null;
			}
			if (nextHop != null) {
				System.out.printf("%c\t|\t%c\t|\t%s\n", known, nextHop, this.distanceVector.get(known).stripTrailingZeros().toPlainString());
			}

		}
		System.out.println("----------------------------------");
	}

	public void printDVWords() {
		BigDecimal min;
		Character nextHop;
		for (Character known : this.knownNodes) {
			min = BigDecimal.valueOf(Integer.MAX_VALUE);
			nextHop = null;
			for (char adjacent : this.connectedAdjacentNodes) {
				try {
					if (min.compareTo(this.distTable.get(adjacent).get(known)) > 0) {
						min = this.distTable.get(adjacent).get(known);
						nextHop = adjacent;
					}
				} catch (NullPointerException e) {
					//nulls represent infinite cost
					//just catch and do nothing
				}
			}
			if (min.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) == 0) {
				//means there is no min
				min = null;
			}
			if (nextHop != null) {
				System.out.printf("shortest path to node %c: the next hop is %c and the cost is %s\n", known, nextHop, this.distanceVector.get(known).stripTrailingZeros().toPlainString());
			}
		}
	}

	public void removeConnectionStatus(Character nodeID) {
		if (!this.allAdjacentNodes.containsKey(nodeID) 
				|| this.disconnectedAdjacentNodes.contains(nodeID) 
				|| !this.connectedAdjacentNodes.contains(nodeID)
				|| !this.connectionStatus.containsKey(nodeID)) {
			throw new InputMismatchException();
		}

		this.connectionStatus.remove(nodeID);
	}

	public void resetMissedBeats(Character nodeID) {
		if (!this.allAdjacentNodes.containsKey(nodeID) 
				|| this.disconnectedAdjacentNodes.contains(nodeID) 
				|| !this.connectedAdjacentNodes.contains(nodeID)
				|| !this.connectionStatus.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.connectionStatus.put(nodeID, 0);
	}

	public void update() {
		if (poisonReversed) {
			this.updated = true;
			for (Character c : this.connectedAdjacentNodes) {
				updateDistance(c,c,this.nodeUpdateCost.get(c));
			}
		}
		updateDV();
	}

	public boolean updated() {
		return this.updated;
	}

	public void updateDistance(Character viaNode, Character toNode, BigDecimal distance) throws InputMismatchException {
		if (!this.connectedAdjacentNodes.contains(viaNode) || !this.knownNodes.contains(toNode)) {
			throw new InputMismatchException();
		}

		if (viaNode.equals(toNode)) {
			//if the adjacent Node is changed, it affects all other
			//costs on that via path,
			//so must update
			//System.out.println("viaNode "+viaNode);
			BigDecimal oldCost = getDistance(viaNode, toNode);
			if (oldCost == null) {//means this is the first entry into this cell
				this.distTable.get(viaNode).put(toNode, distance);
			} else {
				for (Character c : this.distTable.get(viaNode).keySet()) {
					//this.distTable.get(viaNode).put(c, this.distTable.get(viaNode).get(c) - oldCost + distance);
					this.distTable.get(viaNode).put(c, new BigDecimal(Integer.MAX_VALUE));
				}
				this.distTable.get(viaNode).put(toNode, distance);
			}

		} else {
			this.distTable.get(viaNode).put(toNode, distance);
		}

		updateDV();
	}

	public void updateDV() {
		BigDecimal min;
		for (char known : this.knownNodes) {
			min = new BigDecimal(Integer.MAX_VALUE);
			for (char adjacent : this.connectedAdjacentNodes) {
				try {
					if (min.compareTo(this.distTable.get(adjacent).get(known)) > 0) {
						min = this.distTable.get(adjacent).get(known);
					}
				} catch (NullPointerException e) {
					//nulls represent infinite cost
					//just catch and do nothing
				}
			}
			if (min.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) == 0) {
				//means there is no min
				min = null;
			}
			this.distanceVector.put(known, min);
		}
	}
}
