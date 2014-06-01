
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
		Queue jobQueue = new Queue();
		Queue heartBeatQueue = new Queue();
		int pingInterval = 2500;	//in milliseconds
		int convergenceWait = 2500;	//in milliseconds

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

		System.out.println("\nRunning node with these settings:\n");
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
			//g.printDT();
			//g.printDV();
			udp = new UDP(port, nodePorts);				//open udp connection
		} catch (DataFormatException e) {
			System.err.println("Config file has invalid data");
			return;
		} catch (IOException e) {
			System.err.println("could not create udp object");
			return;
		}

		//starts processing heartbeats from other nodes
		new HeartBeatProcessor(jobQueue, heartBeatQueue, g);
		//starts sending heartbeats to other nodes
		new Timer().schedule(new Beat(nodeID, udp), 0, pingInterval);
		//starts listening for incoming messages
		new Listener(udp, jobQueue, heartBeatQueue);
		//starts listening to missed beats
		new Timer().schedule(new MissedBeatsListener(jobQueue, g), 0, pingInterval);

		//#####################################################################
		//Everything has been initialised
		//Processes all jobs in queue

		int waitLimit = 5;
		int waited = 0;
		while (true) {

			try {
				if (jobQueue.isEmpty() && waited < waitLimit) {
					//wait a period of time before declaring converged 
					//to see if there are any other DVs incoming
					Thread.sleep(convergenceWait/waitLimit);
					waited++;
				}
				else if (jobQueue.isEmpty() && waited >= waitLimit) {
					g.printDT();
					g.printDV();
					g.printDVWords();
					g.printDebug();
					synchronized(jobQueue) {
						jobQueue.wait();
					}
				}
				else if (!jobQueue.isEmpty()) {
					waited = 0;
					Object message = jobQueue.pop();
					if (message instanceof Message) {
						((Message) message).execute(g);
						if (message instanceof DistanceVector) {
							if (((DistanceVector) message).isUpdated()) {
								//if distanceTable is updated, send new DV out
								udp.sendToAll(g.getDV());
							}
						}
						else if (message instanceof ConnectionSignal) {
							//ensure the node that caused this signal will have connected
							//by the time the DV gets there
							if (((ConnectionSignal) message).connection() == true && ((ConnectionSignal) message).relayable() == true) {
								udp.sendToAll(new HeartBeat(nodeID));
								udp.sendToAll(g.getDV());
							}
							else if (((ConnectionSignal) message).connection() == false && ((ConnectionSignal) message).relayable() == true) {
								udp.sendToAll(message);
							}
						} 
					} else {
						throw new IllegalArgumentException();
					}
				}
			} catch (IllegalArgumentException e) {
				System.err.println("Received a message that is not a message");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static Graph initialise(char thisNodeID, File config, Map<Character, Integer> nodePorts) throws DataFormatException, IOException {
		Graph g = new Graph(thisNodeID);
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

				g.addAdjacentNode(nodeID, distance);
			}

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

	private static class HeartBeatProcessor implements Runnable {

		private final Queue heartBeatQueue;
		private final Queue jobQueue;
		private final Graph g;
		private final Thread t;

		private HeartBeatProcessor (Queue jobQueue, Queue heartBeatQueue, Graph g) {
			this.heartBeatQueue = heartBeatQueue;
			this.jobQueue = jobQueue;
			this.g = g;
			t = new Thread(this);
			t.start();
			
		}
		

		@Override
		public void run() {
			synchronized (this.heartBeatQueue) {
				try {
					while (true) {
						if (this.heartBeatQueue.isEmpty()) {
							this.heartBeatQueue.wait();
						}
						Object heartBeat = this.heartBeatQueue.pop();
						if (heartBeat instanceof HeartBeat) {
							Boolean action = ((HeartBeat) heartBeat).newConnection(g);
							if (action == true) {
								//means currently disconnected, connect
								this.jobQueue.push(new ConnectionSignal(true, ((HeartBeat) heartBeat).getNodeID()));
							}/* else if (action == false) {
								//action = false, means currently connected, disconnect
								this.jobQueue.push(new ConnectionSignal(false, ((HeartBeat) heartBeat).getNodeID()));
							}*/
						}
					}
				} catch (InterruptedException e) {

				}
			}
		}
	}
	
	private static class MissedBeatsListener extends TimerTask {
		
		private final Queue jobQueue;
		private final Graph g;
		
		private MissedBeatsListener (Queue jobQueue, Graph g) {
			this.jobQueue = jobQueue;
			this.g = g;
		}

		@Override
		public void run() {
			//periodically decrement missedBeats to all connections
			//each heartBeat should reset missedBeats counter
			//when decrementing, check for dead connections
			//for each deadConnection, push ConnectionSignal disconnect to jobQueue
			g.incrementMissedBeat();
			for (Character connection : g.getDeadConnections()) {
				this.jobQueue.push(new ConnectionSignal(false, connection));
			}
			
		}
	}

	private static class Listener implements Runnable {

		private final Queue jobQueue;
		private final Queue heartBeatQueue;
		private final UDP udp;
		private final Thread t;

		private Listener (UDP udp, Queue jobQueue, Queue heartBeatQueue) {
			this.jobQueue = jobQueue;
			this.heartBeatQueue = heartBeatQueue;
			this.udp = udp;
			t = new Thread(this);
			System.out.println("Listener running");
			t.start();
		}

		@Override
		public void run() {
			try {
				while (true) {
					Object readObject = udp.read();
					if (readObject instanceof HeartBeat) {
						//System.out.println("Heartbeat from: "+((HeartBeat) readObject).getNodeID());
						heartBeatQueue.push(readObject);
					}
					else if (readObject instanceof Message) {
						//System.out.println("Message: "+readObject);
						jobQueue.push(readObject);
					} else {
						throw new IOException();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class Beat extends TimerTask {

		private final UDP udp;
		private final char nodeID;

		private Beat(char nodeID, UDP udp) {
			this.nodeID = nodeID;
			this.udp = udp;
		}

		@Override
		public void run() {
			HeartBeat hb = new HeartBeat(this.nodeID);
			try {
				udp.sendToAll(hb);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
