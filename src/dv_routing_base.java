
public class dv_routing_base {

	public static void main(String[] args) {
		if (args.length != 3 && args.length != 4) {
			System.err.println("Error, invalid arguments");
			System.err.println("Usage: [NODE_ID] [NODE_PORT] [CONFIG.TXT] [POISONED REVERSE FLAG|-p]");
			return;
		}
		try {
			if (args[0].length() != 1) {
				throw new IllegalArgumentException("arg [NODE_ID] is invalid, it must be a single uppercase alphabet letter");
			}
			Integer.parseInt(args[1]);
			//check for file exists
			if (args.length == 4) {
				//check flag
			}
			
		} catch (NumberFormatException nfe) {
			
		} catch (IllegalArgumentException lae) {
			
		}


	}

}
