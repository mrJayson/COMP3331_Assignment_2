import java.util.LinkedList;
import java.util.List;


public class Queue {

	private final List<Object> queue;
	private final Thread main;

	public Queue(Thread main) {
		this.queue = new LinkedList<Object>();
		this.main = main;
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
