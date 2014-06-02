

public class HeartBeatProcessor implements Runnable {

	private final Queue heartBeatQueue;
	private final Queue jobQueue;
	private final Graph g;
	private final Thread t;

	HeartBeatProcessor (Queue jobQueue, Queue heartBeatQueue, Graph g) {
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