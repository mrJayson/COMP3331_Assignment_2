import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;


public class UDP {
	private ByteArrayInputStream bais;
	private ByteArrayOutputStream baos;
	private final int byteSize = 1024;

	private DatagramSocket clientSocket;
	private final InetAddress IPAddress;
	private final Map<Character, Integer> nodePorts;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;

	private final DatagramSocket serverSocket;

	public UDP (int port, Map<Character, Integer> adjacentNodes) throws IOException {
		this.serverSocket = new DatagramSocket(port);

		IPAddress = InetAddress.getByName("localhost");
		
		this.nodePorts = adjacentNodes;
	}

	public Map<Character, Integer> getAdjacentNodes() {
		return this.nodePorts;
	}

	public int getPort(Character nodeID) throws InputMismatchException {
		if (!this.nodePorts.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		return this.nodePorts.get(nodeID);
	}

	public List<Integer> getPorts() {
		List<Integer> ports = new ArrayList<Integer>();
		for (Character key : this.nodePorts.keySet()) {
			ports.add(getPort(key));
		}
		return ports;
	}

	public Object read() throws IOException, ClassNotFoundException {
		byte[] receiveBytes = new byte[byteSize];
		Object readObject;
		DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);
		this.serverSocket.receive(receivePacket);
		
		try {
			bais = new ByteArrayInputStream(receivePacket.getData()); 
			ois = new ObjectInputStream(bais);
			readObject = ois.readObject();
		} finally {
			ois.close();
			bais.close();
		}
		return readObject;
	}

	public synchronized void sendToAll(Object sendObject) throws IOException {
		for (int portNumber : getPorts()) {
			write(sendObject, portNumber);
		}
	}

	public void write(Object sendObject, int port) throws IOException {
		byte[] sendBytes;

		if (sendObject instanceof Sendable == false) {
			throw new IllegalArgumentException();
		}
		//System.out.println("sending "+sendObject+" to "+port);

		try {
			this.baos = new ByteArrayOutputStream();
			this.oos = new ObjectOutputStream(baos);

			this.oos.writeObject(sendObject);
			this.oos.flush();

			// change object into byte array
			sendBytes = baos.toByteArray();

			if (sendBytes.length > this.byteSize) {
				throw new IOException();
			}
		} finally {
			this.oos.close();
			this.baos.close();
		}

		try {
			DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, IPAddress, port);
			this.clientSocket = new DatagramSocket();
			clientSocket.send(sendPacket);
		} finally {
			this.clientSocket.close();
		}
	}
}
