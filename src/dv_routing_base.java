import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;


public class dv_routing_base {

	public static void main(String[] args) {

		char nodeID;
		int port;
		File config;
		boolean poisonReversed = false;
		UDP udp;

		if (args.length != 3 && args.length != 4) {
			System.err.println("Usage: [NODE_ID] [NODE_PORT] [CONFIG.TXT] [POISONED REVERSE FLAG|-p]");
			return;
		}

		try {
			if (args[0].length() != 1 || !args[0].matches("[A-Z]")) {
				throw new IllegalArgumentException();
			}
			nodeID = args[0].charAt(0);

			port = Integer.parseInt(args[1]);

			config = new File(args[2]);						//check if file is in immediate directory
			if (!config.exists()) {
				config = new File("../"+args[2]);			//check if file is in parent directory
			}
			if (!config.exists()) {
				config = new File("../config/"+args[2]);	//check if file is in the config folder of parent directory
			}
			if (!config.isFile() || !config.exists()) {
				throw new FileNotFoundException();			//if not found, do not proceed
			}
			if (args.length == 4) {
				if (args[3].toLowerCase().equals("-p")) {	//if flag is entered in wrong, treat as not entered
					poisonReversed = true;
				}
			}

		} catch (NumberFormatException e) {
			System.err.println("port number not valid");
			System.err.println("port number must be a single uppercase alphabet letter");
			return;
		} catch (IllegalArgumentException e) {
			System.err.println("node id not valid");
			System.err.println("node id must be a possible port number");
			return;
		} catch (FileNotFoundException e) {
			System.err.println("file invalid or not found");
			System.err.println("file must be in current directory, parent directory or config folder of parent directory");
			return;
		}

		System.out.println("\nstarting up node with these settings:\n");
		System.out.println("NODE_ID: "+nodeID);
		System.out.println("Port Number: "+port);
		System.out.println("Config File Path: "+config.getAbsolutePath());
		System.out.println("Poison Reverse flag: "+(poisonReversed?"on":"off")+"\n");

		//#####################################################################
		//command line input validation finished
		//now initialising variables
		try {
			Graph g = initialise(nodeID, config);
			g.printDistTable();
		} catch (DataFormatException e) {
			System.err.println("Config file has invalid data");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			udp = new UDP(port);				//open udp connections
		} catch (IOException e) {
			System.err.println("could not create udp object");
			return;
		}
	}

	private static Graph initialise(char thisNodeID, File config) throws DataFormatException, IOException {
		Graph g = new Graph();
		Map<Character, Integer> adjacentNodes = new HashMap<Character, Integer>();
		BufferedReader br = null;
		int num;
		try {
			br = new BufferedReader(new FileReader(config));

			if (!br.ready()) {
				br.close();
				throw new DataFormatException();
			}
			num = Integer.parseInt(br.readLine());
			for (int i = 0; i < num && br.ready(); i++) {
				String[] inputSplit = br.readLine().split(" ");
				if (inputSplit[0].length() != 1 || !inputSplit[0].matches("[A-Z]")) {
					br.close();
					throw new IllegalArgumentException();
				}
				char nodeID = inputSplit[0].charAt(0);
				int distance = Integer.parseInt(inputSplit[1]);
				int port = Integer.parseInt(inputSplit[2]);

				adjacentNodes.put(nodeID, port);	//link node to port
				g.addNode(nodeID);
				g.updateDistance(nodeID, nodeID, distance);
				//since all nodes at this point are adjacent nodes, the via node and to node are the same
			}
			//set adj nodes after collect them from file
			g.setAdjacentNodes(adjacentNodes);
			br.close();
		} catch (FileNotFoundException e) {
			//should not reach here, it has already been checked
		} catch (NumberFormatException e) {
			throw new DataFormatException();
		} catch (IndexOutOfBoundsException e) {
			System.out.println("test");
			throw new DataFormatException();
		} catch (Exception e) {
		}


		return g;
	}

	private static class UDP {

		private DatagramSocket serverSocket;
		private DatagramSocket clientSocket;
		private ByteArrayOutputStream baos;
		private ObjectOutputStream oos;

		private UDP (int port) throws IOException {
			this.serverSocket = new DatagramSocket(port);
			this.clientSocket = new DatagramSocket();
			this.baos = new ByteArrayOutputStream();
			this.oos = new ObjectOutputStream(baos);
		}

		private void write(Object obj, char nodeID) throws IOException {
			this.oos.writeObject(obj);
			this.oos.flush();
			// change object into byte array
			byte[] buf= baos.toByteArray();
		}

	}

}
