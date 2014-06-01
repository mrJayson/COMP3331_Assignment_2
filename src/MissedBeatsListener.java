import java.util.TimerTask;


public class MissedBeatsListener extends TimerTask {
	
	private final Queue jobQueue;
	private final Graph g;
	
	MissedBeatsListener (Queue jobQueue, Graph g) {
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