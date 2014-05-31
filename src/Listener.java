
public class Listener implements Runnable{

	private final JobQueue queue;
	private final UDP udp;
	private final Thread t;

	public Listener (UDP udp, JobQueue queue) {
		this.queue = queue;
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
					System.out.println("Heartbeat from: "+((HeartBeat) readObject).getNodeID());
				}
				queue.push(readObject);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}