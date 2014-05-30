
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.DataFormatException;


public class dv_routing_base {

	public static void main(String[] args) {

		char nodeID;
		int port;
		File config;
		boolean poisonReversed = false;
		UDP udp;
		Graph g;
		SyncQueue queue = new SyncQueue();
		int pingIntervalMilli = 5000;

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
			Map<Character, Integer> nodePorts = new HashMap<Character, Integer>();
			g = initialise(nodeID, config, nodePorts);

			g.printDT();
			g.printDV();
			udp = new UDP(port, nodePorts);				//open udp connections
		} catch (DataFormatException e) {
			System.err.println("Config file has invalid data");
			return;
		} catch (IOException e) {
			System.err.println("could not create udp object");
			return;
		}

		Listener listener = new Listener(udp, queue);	//starts the listener thread
		Timer t = new Timer();
		t.schedule(new Ping(nodeID, udp), 0, pingIntervalMilli);
		//#####################################################################
		//Everything has been initialised
		//Processes all jobs in queue 
		synchronized (queue) {
			try {
				while (true) {
					if (queue.isEmpty()) {
						queue.wait();
					}
					Object message = queue.pop();
					if (message instanceof Message) {
						((Message) message).execute();
					} else {
						throw new IllegalArgumentException();
					}
				}
			} catch (InterruptedException e) {

			} catch (IllegalArgumentException e) {
				System.err.println("Received a message that is not a message");
			}
		}
	}

	private static Graph initialise(char thisNodeID, File config, Map<Character, Integer> nodePorts) throws DataFormatException, IOException {
		Graph g = new Graph();
		BufferedReader br = null;
		FileReader fr = null;
		int num;
		try {
			fr = new FileReader(config);
			br = new BufferedReader(fr);

			if (!br.ready()) {
				throw new DataFormatException();
			}
			num = Integer.parseInt(br.readLine());
			for (int i = 0; i < num; i++) {
				String[] inputSplit = br.readLine().split(" ");
				if (inputSplit[0].length() != 1 || !inputSplit[0].matches("[A-Z]")) {
					throw new IllegalArgumentException();
				}
				char nodeID = inputSplit[0].charAt(0);
				int distance = Integer.parseInt(inputSplit[1]);
				int port = Integer.parseInt(inputSplit[2]);
				nodePorts.put(nodeID, port);	//link node to port

				g.addAdjacentNode(nodeID);
				g.addKnownNode(nodeID);
				g.updateDistance(nodeID, nodeID, distance);
				//since all nodes at this point are adjacent nodes, the via node and to node are the same
			}
			//set adjacent nodes after collect them from file

		} catch (FileNotFoundException e) {
			//should not reach here, it has already been checked
		} catch (NumberFormatException e) {
			throw new DataFormatException();
		} catch (IndexOutOfBoundsException e) {
			throw new DataFormatException();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			br.close();
			fr.close();
		}
		return g;
	}

	private static class Ping extends TimerTask {

		private final UDP udp;
		private final char nodeID;

		private Ping(char nodeID, UDP udp) {
			this.nodeID = nodeID;
			this.udp = udp;
		}

		@Override
		public void run() {
			HeartBeat hb = new HeartBeat(this.nodeID);
			try {
				udp.sendToAll(udp.getPorts(), hb);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
