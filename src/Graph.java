
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



class Graph implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final char thisNodeID;
	private final Map<Character, Integer> allAdjacentNodes;
	private final List<Character> disconnectedAdjacentNodes;
	private final List<Character> connectedAdjacentNodes;
	private final List<Character> knownNodes;
	//List of all the nodes this node is aware of
	private final Map<Character, Map<Character, Integer>> distTable;
	//distTable is a 2d map so can map char,char to a distance
	//first char is the via node, second char is the to node
	private final Map<Character, Integer> distanceVector;

	Graph(char nodeID) {
		this.thisNodeID = nodeID;
		
		this.allAdjacentNodes = new HashMap<Character, Integer>();
		this.disconnectedAdjacentNodes = new ArrayList<Character>();
		this.connectedAdjacentNodes = new ArrayList<Character>();
		this.knownNodes = new ArrayList<Character>();
		this.distTable = new HashMap<Character, Map<Character, Integer>>();
		this.distanceVector = new HashMap<Character, Integer>();
	}
	
	boolean connected(Character nodeID) throws InputMismatchException {
		if (!this.allAdjacentNodes.keySet().contains(nodeID)) {
			throw new InputMismatchException();
		}
		return this.connectedAdjacentNodes.contains(nodeID);
	}
	
	void addAdjacentNode(Character nodeID, Integer directCost) throws InputMismatchException {
		if (this.allAdjacentNodes.containsKey(nodeID) 
				|| this.disconnectedAdjacentNodes.contains(nodeID) 
				|| this.connectedAdjacentNodes.contains(nodeID)) {
			throw new InputMismatchException();
		}
		//add node with cost
		this.allAdjacentNodes.put(nodeID, directCost);
		//newly added adjacent nodes start as disconnected
		this.disconnectedAdjacentNodes.add(nodeID);
	}

	void connectAdjacentNode(Character nodeID) throws InputMismatchException {
		if (!this.disconnectedAdjacentNodes.contains(nodeID) 	//must be a disconnected node to connect
				|| this.connectedAdjacentNodes.contains(nodeID)) {//safety check
			throw new InputMismatchException();
		}
		System.out.println("connecting: "+nodeID);

		this.connectedAdjacentNodes.add(nodeID);		//add to one
		this.disconnectedAdjacentNodes.remove(nodeID);	//remove from the other
		this.distTable.put(nodeID, new HashMap<Character, Integer>());
		//once the adjacent node is connected, it becomes known in the network
		try {
		addKnownNode(nodeID);
		} catch (InputMismatchException e) {
			//Catch adding in an already known node
		}
		//add direct cost to distTable
		//since all nodes at this point are adjacent nodes, the via node and to node are the same
		updateDistance(nodeID, nodeID, this.allAdjacentNodes.get(nodeID));
	}

	void disconnectAdjacentNode(Character nodeID) throws InputMismatchException {
		if (!this.connectedAdjacentNodes.contains(nodeID) || !this.distTable.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.connectedAdjacentNodes.remove(nodeID);
		this.distTable.remove(nodeID);
		for (Character adjacent : this.connectedAdjacentNodes) {
			//remove c entries in other rows
			this.distTable.get(adjacent).remove(nodeID);
		}
		//all adjacent nodes are in knownNodes, so delete there too
		this.knownNodes.remove(nodeID);
		updateDV();
	}

	void addKnownNode(Character nodeID) throws InputMismatchException {
		if (nodeID.equals(this.thisNodeID) || this.knownNodes.contains(nodeID) || this.distanceVector.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.knownNodes.add(nodeID);
		this.distanceVector.put(nodeID, null);
		//adding a new entry to distanceVector means there is no min yet
	}

	void deleteKnownNode(Character nodeID) throws InputMismatchException {
		if (!this.knownNodes.contains(nodeID) || !this.distanceVector.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		if (this.connectedAdjacentNodes.contains(nodeID)) {
			//if node to be deleted is an adjacent, let this function handle it
			disconnectAdjacentNode(nodeID);
		} else {
			this.distanceVector.remove(nodeID);
			for (Character adjacent : this.connectedAdjacentNodes) {
				this.distTable.get(adjacent).remove(nodeID);
			}
			this.knownNodes.remove(nodeID);
		}
		//deleting from distanceVector means the min entry is deleted without side-effects
	}

	boolean knowsNode(Character nodeID) {
		return this.knownNodes.contains(nodeID);
	}

	void updateDistance(Character viaNode, Character toNode, int distance) throws InputMismatchException {
		if (!this.connectedAdjacentNodes.contains(viaNode) || !this.knownNodes.contains(toNode)) {
			throw new InputMismatchException();
		}
		this.distTable.get(viaNode).put(toNode, distance);
		updateDV();
	}

	Integer getDistance(Character viaNode, Character toNode) {
		if (!this.connectedAdjacentNodes.contains(viaNode) || !this.knownNodes.contains(toNode)) {
			throw new InputMismatchException();
		}
		//will return null if there is no cost yet
		return this.distTable.get(viaNode).get(toNode);
	}

	void updateDV() {
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

	DistanceVector getDV() {
		return new DistanceVector(this.thisNodeID, this.distanceVector);
	}

	void printDT() {
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

	void printDV() {
		System.out.println("----------------------------------");
		System.out.println("Distance Vectors");
		System.out.println("----------------------------------");
		for (char known : this.knownNodes) {
			System.out.printf("%c\t|\t%d\n", known, this.distanceVector.get(known));
		}
		System.out.println("----------------------------------");
	}
}
