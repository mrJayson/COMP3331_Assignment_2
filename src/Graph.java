import java.io.Serializable;
import java.util.ArrayList;
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
	private transient Map<Character, Integer> adjacentNodes = new HashMap<Character, Integer>();
	private List<Character> nodeList;
	private Map<Character, Map<Character, Integer>> distTable;
	//distTable is a 2d map so can map char,char to a distance
	//first char is the via node, second char is the to node

	public Graph() {
		this.nodeList = new ArrayList<Character>();
		this.distTable = new HashMap<Character, Map<Character, Integer>>();
	}

	public void addNode(Character c) throws InputMismatchException {
		if (this.nodeList.contains(c) || this.distTable.containsKey(c)) {
			throw new InputMismatchException();
		}
		this.nodeList.add(c);
		this.distTable.put(c, new HashMap<Character, Integer>());
	}
	
	public void deleteNode(Character c) throws InputMismatchException {
		if (!this.nodeList.contains(c) || !this.distTable.containsKey(c)) {
			throw new InputMismatchException();
		}
		this.nodeList.remove(c);
		this.distTable.remove(c);
		for (Character character : this.nodeList) {
			//remove c entries in other rows
			this.distTable.get(character).remove(c);
		}
	}
	
	public void setAdjacentNodes(Map<Character, Integer> adjacentNodes) {
		this.adjacentNodes = adjacentNodes;
	}
	
	public Map<Character, Integer> getAdjacentNodes() {
		return this.adjacentNodes;
	}
	
	public void updateDistance(Character viaNode, Character toNode, int distance) {
		this.distTable.get(viaNode).put(toNode, distance);
	}

	public void printTable() {
		for (char c1 : this.nodeList) {
			for (char c2 : this.nodeList) {
				try{
				System.out.printf("%c %c %d\n", c1, c2, this.distTable.get(c1).get(c2));
				} catch (Exception e) {
					
				}
			}
		}
	}
	public void printDistTable() {
		System.out.printf("\t");
		for (char c : this.nodeList) {
			System.out.printf("%c\t", c);
		}
		System.out.println();
		
		for (char c1 : this.nodeList) {
			System.out.printf("%c\t", c1);
			for (char c2 : this.nodeList) {
				System.out.printf("%d\t", this.distTable.get(c2).get(c1));
			}
			System.out.println();
		}
	}
}
