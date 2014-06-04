
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Timer;
import java.util.zip.DataFormatException;

public class dv_routing_pr {

	public static void main(String[] args) {

		char nodeID;
		int port;
		File config;
		boolean poisonReversed = false;
		boolean debug = false;
		UDP udp;
		Model g;
		Queue jobQueue = new Queue();
		Queue heartBeatQueue = new Queue();
		int pingInterval = 2500;	//in milliseconds
		int convergenceWait = 2000;	//in milliseconds

		if (args.length != 3 && args.length != 4 && args.length != 5) {
			System.err.println("Usage: [NODE_ID] [NODE_PORT] [CONFIG.TXT] [POISONED REVERSE FLAG|-p] [DEBUG|-d]");
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
			if (args.length >= 4) {
				if (args[3].toLowerCase().equals("-p")) {	//if flag is entered in wrong, treat as not entered
					poisonReversed = true;
				}
				if (args[3].toLowerCase().equals("-d")) {	//if flag is entered in wrong, treat as not entered
					debug = true;
				}
			}
			if (args.length >= 5) {
				if (args[4].toLowerCase().equals("-p")) {	//if flag is entered in wrong, treat as not entered
					poisonReversed = true;
				}
				if (args[4].toLowerCase().equals("-d")) {	//if flag is entered in wrong, treat as not entered
					debug = true;
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
			System.err.println("file must be in ~/[CONFIG.TXT] OR ~/../[CONFIG.TXT] OR ~/../config/[CONFIG.TXT]");
			return;
		}
		System.out.print("\033[H\033[2J");
		System.out.println("\nRunning node with these settings:\n");
		System.out.println("NODE_ID:\t\t"+nodeID);
		System.out.println("Port Number:\t\t"+port);
		System.out.println("Config File Path:\t"+config.getAbsolutePath());
		System.out.println("Poison Reverse flag:\t"+(poisonReversed?"on":"off"));
		System.out.println("debug flag:\t\t"+(debug?"on":"off")+"\n");

		//#####################################################################
		//command line input validation finished
		//now initialising variables
		try {
			Map<Character, Integer> nodePorts = new HashMap<Character, Integer>();
			g = initialise(nodeID, poisonReversed, config, nodePorts);
			udp = new UDP(port, nodePorts);				//open udp connection
		} catch (InputMismatchException e) {
			System.err.println("Poisoned Reverse is enabled for a file that is not poisoned reversable\n");
			return;
		} catch (DataFormatException e) {
			System.err.println("Config file has invalid data\n");
			return;
		} catch (IOException e) {
			System.err.println("Could not set up UDP listening on port "+port+"\n");
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
					System.out.print("\033[H\033[2J");
					System.out.println("\nRunning node with these settings:\n");
					System.out.println("NODE_ID:\t\t"+nodeID);
					System.out.println("Port Number:\t\t"+port);
					System.out.println("Config File Path:\t"+config.getAbsolutePath());
					System.out.println("Poison Reverse flag:\t"+(poisonReversed?"on":"off"));
					System.out.println("debug flag:\t\t"+(debug?"on":"off")+"\n");
					if (debug) {
						g.printDebug();
						g.printDT();
						g.printDV();
					}
					g.printDVWords();

					if (!g.updated() && poisonReversed) {
						if (g.getDisconnectedNodes().size() == 0) {
							System.out.println("Applying new costs");
							g.update();
							udp.sendToAll(g.getDV());
						} else {
							System.out.println("Waiting for all nodes to be online before applying update");
						}
					}
					synchronized(jobQueue) {
						jobQueue.wait();
					}
				}
				else if (!jobQueue.isEmpty()) {
					waited = 0;
					Object job = jobQueue.pop();
					if (job instanceof Message) {
						((Message) job).execute(g);
						if (job instanceof DistanceVector) {
							if (((DistanceVector) job).isUpdated()) {
								//if distanceTable is updated, send new DV out
								udp.sendToAll(g.getDV());
							}
						}
						else if (job instanceof ConnectionSignal) {
							//ensure the node that caused this signal will have connected
							//by the time the DV gets there
							if (((ConnectionSignal) job).connection() == true && ((ConnectionSignal) job).relayable() == true) {
								System.out.println("Node "+((ConnectionSignal) job).node()+" connected");
								udp.sendToAll(new HeartBeat(nodeID));
								udp.sendToAll(g.getDV());
							}
							else if (((ConnectionSignal) job).connection() == false && ((ConnectionSignal) job).relayable() == true) {
								System.out.println("Node "+((ConnectionSignal) job).node()+" disconnected");
								udp.sendToAll(job);
							}
						} 
					} else {
						throw new IllegalArgumentException();
					}
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
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

	private static Model initialise(char thisNodeID, boolean poisonReversed, File config, Map<Character, Integer> nodePorts) throws InputMismatchException, DataFormatException, IOException {
		Model g = new Model(thisNodeID, poisonReversed);
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
				if ((poisonReversed && inputSplit.length != 4) 
						&& (poisonReversed && inputSplit.length != 3)) {
					throw new InputMismatchException();
				}

				if (inputSplit[0].length() != 1 || !inputSplit[0].matches("[A-Z]")) {
					throw new IllegalArgumentException();
				}
				char nodeID = inputSplit[0].charAt(0);
				BigDecimal distance = new BigDecimal(inputSplit[1]);
				BigDecimal updateDistance = null;
				Integer port = null;
				if (inputSplit.length == 3) {
					port = Integer.parseInt(inputSplit[2]);
				}
				else if (inputSplit.length == 4) {
					if (poisonReversed) {
						updateDistance = new BigDecimal(inputSplit[2]);
					}
					port = Integer.parseInt(inputSplit[3]);
				}
				g.addAdjacentNode(nodeID, distance, updateDistance);
				if (port == null) {
					throw new DataFormatException();
				}
				nodePorts.put(nodeID, port);	//link node to port

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
}
