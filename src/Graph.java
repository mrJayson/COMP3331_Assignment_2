
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class Graph implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final char thisNodeID;
	private final Map<Character, Integer> allAdjacentNodes;
	private final Map<Character, Integer> nodeUpdateCost;
	private final List<Character> disconnectedAdjacentNodes;
	private final List<Character> connectedAdjacentNodes;
	private final List<Character> knownNodes;
	//List of all the nodes this node is aware of
	private final Map<Character, Map<Character, Integer>> distTable;
	//distTable is a 2d map so can map char,char to a distance
	//first char is the via node, second char is the to node
	private final Map<Character, Integer> distanceVector;
	private final Map<Character, Integer> connectionStatus;
	private final int missedBeatsLimit = 3;
	private final boolean poisonReversed;
	private boolean updated;

	public Graph(char nodeID, boolean poisonReversed) {
		this.thisNodeID = nodeID;
		this.allAdjacentNodes = new HashMap<Character, Integer>();
		this.nodeUpdateCost = new HashMap<Character, Integer>();
		this.disconnectedAdjacentNodes = new ArrayList<Character>();
		this.connectedAdjacentNodes = new ArrayList<Character>();
		this.knownNodes = new ArrayList<Character>();
		this.distTable = new HashMap<Character, Map<Character, Integer>>();
		this.distanceVector = new HashMap<Character, Integer>();
		this.connectionStatus = new HashMap<Character, Integer>();
		this.poisonReversed = poisonReversed;
		this.updated = false;
	}

	public boolean connected(Character nodeID) throws InputMismatchException {
		if (!this.allAdjacentNodes.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		return this.connectedAdjacentNodes.contains(nodeID);
	}

	public boolean updated() {
		return this.updated;
	}

	public void update() {
		if (poisonReversed) {
			this.updated = true;
			for (Character c : this.connectedAdjacentNodes) {
				updateDistance(c,c,this.nodeUpdateCost.get(c));
			}
		}
	}

	public void addAdjacentNode(Character nodeID, Integer directCost, Integer updateCost) throws InputMismatchException {
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

	public void removeConnectionStatus(Character nodeID) {
		if (!this.allAdjacentNodes.containsKey(nodeID) 
				|| this.disconnectedAdjacentNodes.contains(nodeID) 
				|| !this.connectedAdjacentNodes.contains(nodeID)
				|| !this.connectionStatus.containsKey(nodeID)) {
			throw new InputMismatchException();
		}

		this.connectionStatus.remove(nodeID);
	}

	public void incrementMissedBeat() {
		for (Character connection : this.connectionStatus.keySet()) {
			this.connectionStatus.put(connection, this.connectionStatus.get(connection)+1);
		}
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

	public void resetMissedBeats(Character nodeID) {
		if (!this.allAdjacentNodes.containsKey(nodeID) 
				|| this.disconnectedAdjacentNodes.contains(nodeID) 
				|| !this.connectedAdjacentNodes.contains(nodeID)
				|| !this.connectionStatus.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.connectionStatus.put(nodeID, 0);
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
		this.distTable.put(nodeID, new HashMap<Character, Integer>());
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

	public boolean isAdjacent(Character nodeID) {
		return this.allAdjacentNodes.containsKey(nodeID);
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

	public void addKnownNode(Character nodeID) throws InputMismatchException {
		if (nodeID.equals(this.thisNodeID) || this.knownNodes.contains(nodeID) || this.distanceVector.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.knownNodes.add(nodeID);
		Collections.sort(this.knownNodes);
		this.distanceVector.put(nodeID, null);
		//adding a new entry to distanceVector means there is no min yet
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

	public void updateDistance(Character viaNode, Character toNode, int distance) throws InputMismatchException {
		if (!this.connectedAdjacentNodes.contains(viaNode) || !this.knownNodes.contains(toNode)) {
			throw new InputMismatchException();
		}
		this.distTable.get(viaNode).put(toNode, distance);
		updateDV();
	}

	public Integer getDistance(Character viaNode, Character toNode) {
		if (!this.connectedAdjacentNodes.contains(viaNode) || !this.knownNodes.contains(toNode)) {
			throw new InputMismatchException();
		}
		//will return null if there is no cost yet
		return this.distTable.get(viaNode).get(toNode);
	}

	public void updateDV() {
		Integer min;
		for (char known : this.knownNodes) {
			min = Integer.MAX_VALUE;
			for (char adjacent : this.connectedAdjacentNodes) {
				try {
					if (min > this.distTable.get(adjacent).get(known)) {
						min = this.distTable.get(adjacent).get(known);
					}
				} catch (NullPointerException e) {
					//nulls represent infinite cost
					//just catch and do nothing
				}
			}
			if (min == Integer.MAX_VALUE) {
				//means there is no min
				min = null;
			}
			this.distanceVector.put(known, min);
		}
	}

	public DistanceVector getDV() {
		return new DistanceVector(this.thisNodeID, this.distanceVector);
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

	public void printDVWords() {
		Integer min;
		Character nextHop;
		for (Character known : this.knownNodes) {
			min = Integer.MAX_VALUE;
			nextHop = null;
			for (char adjacent : this.connectedAdjacentNodes) {
				try {
					if (min > this.distTable.get(adjacent).get(known)) {
						min = this.distTable.get(adjacent).get(known);
						nextHop = adjacent;
					}
				} catch (NullPointerException e) {
					//nulls represent infinite cost
					//just catch and do nothing
				}
			}
			if (min == Integer.MAX_VALUE) {
				//means there is no min
				min = null;
			}
			if (nextHop != null) {
				System.out.printf("shortest path to node %c: the next hop is %c and the cost is %d\n", known, nextHop, this.distanceVector.get(known));
			}
		}
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
				System.out.printf("%d\t", this.distTable.get(adjacent).get(known));
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
		Integer min;
		Character nextHop;
		for (Character known : this.knownNodes) {
			min = Integer.MAX_VALUE;
			nextHop = null;
			for (char adjacent : this.connectedAdjacentNodes) {
				try {
					if (min > this.distTable.get(adjacent).get(known)) {
						min = this.distTable.get(adjacent).get(known);
						nextHop = adjacent;
					}
				} catch (NullPointerException e) {
					//nulls represent infinite cost
					//just catch and do nothing
				}
			}
			if (min == Integer.MAX_VALUE) {
				//means there is no min
				min = null;
			}
			if (nextHop != null) {
				System.out.printf("%c\t|\t%c\t|\t%d\n", known, nextHop, this.distanceVector.get(known));
			}

		}
		System.out.println("----------------------------------");
	}
}
