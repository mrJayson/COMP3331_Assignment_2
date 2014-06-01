import java.io.IOException;


public class Listener implements Runnable {

	private final Queue jobQueue;
	private final Queue heartBeatQueue;
	private final UDP udp;
	private final Thread t;

	Listener (UDP udp, Queue jobQueue, Queue heartBeatQueue) {
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
					System.out.println("Message: "+readObject);
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