
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
	private List<Character> adjacentNodes;
	private List<Character> knownNodes;
	//List of all the nodes this node is aware of
	private Map<Character, Map<Character, Integer>> distTable;
	//distTable is a 2d map so can map char,char to a distance
	//first char is the via node, second char is the to node
	private Map<Character, Integer> distanceVector;

	Graph() {
		this.knownNodes = new ArrayList<Character>();
		this.adjacentNodes = new ArrayList<Character>();
		this.distTable = new HashMap<Character, Map<Character, Integer>>();
		this.distanceVector = new HashMap<Character, Integer>();
	}

	void addAdjacentNode(Character nodeID) throws InputMismatchException{
		if (this.knownNodes.contains(nodeID) || this.distTable.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.adjacentNodes.add(nodeID);
		this.distTable.put(nodeID, new HashMap<Character, Integer>());
	}

	void deleteAdjacentNode(Character nodeID) throws InputMismatchException {

	}

	void addKnownNode(Character nodeID) throws InputMismatchException {
		if (this.knownNodes.contains(nodeID) || this.distanceVector.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.knownNodes.add(nodeID);
		this.distanceVector.put(nodeID, null);
	}

	void deleteKnownNode(Character nodeID) throws InputMismatchException {
		if (!this.knownNodes.contains(nodeID) || !this.distTable.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		this.knownNodes.remove(nodeID);
		this.distTable.remove(nodeID);
		for (Character character : this.knownNodes) {
			//remove c entries in other rows
			this.distTable.get(character).remove(nodeID);
		}
	}

	void updateDistance(Character viaNode, Character toNode, int distance) throws InputMismatchException {
		if (!this.adjacentNodes.contains(viaNode) || !this.knownNodes.contains(toNode)) {
			throw new InputMismatchException();
		}
		
		this.distTable.get(viaNode).put(toNode, distance);
		int min = Integer.MAX_VALUE;
		for (char adjacent : this.adjacentNodes) {
			System.out.println(adjacent);
			System.out.println(toNode);
			try {
				if (min > this.distTable.get(adjacent).get(toNode)) {
					min = this.distTable.get(adjacent).get(toNode);
				}
			} catch (NullPointerException e) {
				//nulls represent infinite cost
				//just catch and do nothing
			}
		}
		this.distanceVector.put(toNode, min);
	}

	void printDT() {
		System.out.println("----------------------------------");
		System.out.println("Distance Table");
		System.out.println("----------------------------------");
		System.out.printf("\t");
		for (char c : this.adjacentNodes) {
			System.out.printf("%c\t", c);
		}
		System.out.println();

		for (char known : this.knownNodes) {
			System.out.printf("%c\t", known);
			for (char adjacent : this.adjacentNodes) {
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
