import java.util.LinkedList;
import java.util.List;


public class JobQueue {
	
	private final List<Object> queue;

	public JobQueue() {
		this.queue = new LinkedList<Object>();
	}

	public boolean isEmpty() {
		return this.queue.isEmpty();
	}


	public synchronized void push(Object object) {
		synchronized (this.queue){ 
			this.queue.add(object);
			this.notify();
		}
	}

	public synchronized Object pop() {
		return this.queue.remove(0);
	}

}
