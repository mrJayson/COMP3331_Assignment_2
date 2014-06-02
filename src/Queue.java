import java.util.LinkedList;
import java.util.List;


public class Queue {

	private final List<Object> queue;

	public Queue() {
		this.queue = new LinkedList<Object>();
	}

	public boolean isEmpty() {
		return this.queue.isEmpty();
	}

	public synchronized Object pop() {
		return this.queue.remove(0);
	}

	public synchronized void push(Object object) {
		synchronized (this.queue){ 
			this.queue.add(object);
			this.notify();
		}
	}

}
