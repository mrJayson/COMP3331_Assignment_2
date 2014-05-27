import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class dv_routing_base {

	public static void main(String[] args) {
		if (args.length != 3 && args.length != 4) {
			System.err.println("Error: invalid arguments");
			System.err.println("Usage: [NODE_ID] [NODE_PORT] [CONFIG.TXT] [POISONED REVERSE FLAG|-p]");
			return;
		}
		
		char nodeID;
		int port;
		File config;
		boolean poisonReverse = false;
		
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
					poisonReverse = true;
				}
			}
			
		} catch (NumberFormatException nfe) {
			System.err.println("port number not valid");
			System.err.println("port number must be a single uppercase alphabet letter");
			return;
		} catch (IllegalArgumentException lae) {
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
		System.out.println("Poison Reverse flag: "+(poisonReverse? "on":"off")+"\n");
		
		
	}

}
