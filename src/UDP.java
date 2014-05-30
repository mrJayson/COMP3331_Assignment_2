

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;

class UDP {
	private final DatagramSocket serverSocket;
	private final DatagramSocket clientSocket;
	private final InetAddress IPAddress;

	private final ByteArrayOutputStream baos;
	private final ObjectOutputStream oos;
	private final int byteSize = 1024;

	private final Map<Character, Integer> nodePorts;

	UDP (int port, Map<Character, Integer> adjacentNodes) throws IOException {
		this.serverSocket = new DatagramSocket(port);
		this.clientSocket = new DatagramSocket();
		IPAddress = InetAddress.getByName("localhost");

		this.baos = new ByteArrayOutputStream();
		this.oos = new ObjectOutputStream(baos);
		this.nodePorts = adjacentNodes;
	}

	void write(Object sendObject, int port) throws IOException {

		if (sendObject instanceof Message == false) {

			throw new IllegalArgumentException();
		}
		System.out.println("sending "+sendObject+" to "+port);
		this.oos.writeObject(sendObject);
		this.oos.flush();
		// change object into byte array
		byte[] sendBytes= baos.toByteArray();

		if (sendBytes.length > this.byteSize) {
			throw new IOException();
		}
		DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, IPAddress, port);
		clientSocket.send(sendPacket);
	}

	Object read() throws IOException, ClassNotFoundException {
		byte[] receiveBytes = new byte[byteSize];
		DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);
		this.serverSocket.receive(receivePacket);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
		return ois.readObject();
	}

	void sendToAll(List<Integer> ports, Object sendObject) throws IOException {

		for (int portNumber : ports) {
			write(sendObject, portNumber);
		}
	}
	
	Map<Character, Integer> getAdjacentNodes() {
		return this.nodePorts;
	}
	
	List<Integer> getPorts() {
		List<Integer> ports = new ArrayList<Integer>();
		for (Character key : this.nodePorts.keySet()) {
			ports.add(this.nodePorts.get(key));
		}
		return ports;
	}
	
	int getPort(Character nodeID) throws InputMismatchException {
		if (!this.nodePorts.containsKey(nodeID)) {
			throw new InputMismatchException();
		}
		return this.nodePorts.get(nodeID);
	}
}
