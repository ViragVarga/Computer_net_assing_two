import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Forwarder extends Node {

    // Setting the endpoints' and forwarders' name and port number
    static final int[] FW_PORTS = { 50001, 50002, 50003 };
    static final String[] FW_NODES = { "forwarder1", "forwarder2", "forwarder3" };
    static final int[] ENDPOINT_PORTS = { 50000, 50004 };
    static final String[] ENDPOINT_NODE = { "endpoint1", "endpoint2" };

    // Creating an array for the two connections each forwarder has
    InetSocketAddress[] connectedNodes = new InetSocketAddress[2];

    // Contractor for the class
    Forwarder(int localPort, String firstConnectedNode, int firstConnectedPort, String secondConnectedNode,
            int secondConnectedPort) {
        try {
            socket = new DatagramSocket(localPort);
            connectedNodes[0] = new InetSocketAddress(firstConnectedNode, firstConnectedPort);
            connectedNodes[1] = new InetSocketAddress(secondConnectedNode, secondConnectedPort);
            listener.go();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        boolean validInput = false;
        Forwarder forwarder = null;
        while (!validInput) {
            System.out.println("Initialize the forwarder (1, 2 or 3):"); // Differentiating the forwarder (with its
                                                                         // connections) by user input number
            int numberFW = 0;
            try {
                numberFW = scanner.nextInt();
                if (numberFW == 1) {
                    validInput = true;
                    forwarder = new Forwarder(FW_PORTS[0], ENDPOINT_NODE[0], ENDPOINT_PORTS[0], FW_NODES[1],
                            FW_PORTS[1]);
                } else if (numberFW == 2) {
                    validInput = true;
                    forwarder = new Forwarder(FW_PORTS[1], FW_NODES[0], FW_PORTS[0], FW_NODES[2], FW_PORTS[2]);
                } else if (numberFW == 3) {
                    validInput = true;
                    forwarder = new Forwarder(FW_PORTS[2], FW_NODES[1], FW_PORTS[1], ENDPOINT_NODE[1],
                            ENDPOINT_PORTS[1]);
                }
            } catch (Exception e) {
                System.out.println("Invalid entry");
            }
        }
        scanner.close();
        forwarder.start();
    }

    public synchronized void start() throws Exception {
        System.out.println("Forwarder's ready to recieve and forward packet(s)");
        this.wait();
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        System.out.println("Packet recieved.");
        int port = packet.getPort(); // taking the port number it was sent from and forwarding it to the other
                                     // connection the forwarder has
        try {
            if (port == connectedNodes[0].getPort()) {
                packet.setSocketAddress(connectedNodes[1]);
                socket.send(packet);
                System.out.println("Packet sent.");
            } else if (port == connectedNodes[1].getPort()) {
                packet.setSocketAddress(connectedNodes[0]);
                socket.send(packet);
                System.out.println("Packet sent.");
            } else {
                System.out.println("Packet is sent from an unidentified port!");
            }

        } catch (Exception e) {
            System.out.println("Failed to forward the packet");
        }
        this.notify();
    }
}
