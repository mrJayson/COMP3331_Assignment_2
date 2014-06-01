import java.io.IOException;
import java.util.TimerTask;


public class Beat extends TimerTask {

	private final UDP udp;
	private final char nodeID;

	Beat(char nodeID, UDP udp) {
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