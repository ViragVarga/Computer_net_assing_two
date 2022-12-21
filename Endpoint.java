import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Endpoint extends Node {

    // Setting the endpoints' and forwarders' name and port number
    static final int[] HOST_PORTS = { 50000, 50004 };
    static final String[] ENDPOINT_NODES = { "endpoint1", "endpoint2" };
    static final int NEXT_FW_PORTS = 54321;
    static final String[] NEXT_FW_NODES = { "forwarder1", "forwarder3" };

    // Creating a variable for the connected forwarder and the other endpoint
    InetSocketAddress nextFW;
    InetSocketAddress dstNode;

    Scanner scanner = new Scanner(System.in);
    String hostName;

    // Contractor for the class
    Endpoint(int hostPort, String hostName, String connectedNode, int connectedPort, String dstNode, int dstPort) {
        try {
            nextFW = new InetSocketAddress(connectedNode, connectedPort);
            this.dstNode = new InetSocketAddress(dstNode, dstPort);
            socket = new DatagramSocket(hostPort);
            this.hostName = hostName;
            listener.go();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean validInput = false;
        Endpoint endpoint = null;
        while (!validInput) {
            System.out.println("Initialize the endpoint (1 or 2):"); // Differentiating the two endpoints depending on
                                                                     // the number given
            int numberEP = 0;
            try {
                numberEP = scanner.nextInt();
                if (numberEP == 1) { // Initializing the host endpoint with connected forwarder and the other
                                     // endpoint
                    validInput = true;
                    endpoint = new Endpoint(HOST_PORTS[0], ENDPOINT_NODES[0], NEXT_FW_NODES[0], NEXT_FW_PORTS,
                            ENDPOINT_NODES[1],
                            HOST_PORTS[1]);
                    while (true) {
                        endpoint.startEP1();
                    }
                } else if (numberEP == 2) {
                    validInput = true;
                    endpoint = new Endpoint(HOST_PORTS[1], ENDPOINT_NODES[1], NEXT_FW_NODES[1], NEXT_FW_PORTS,
                            ENDPOINT_NODES[0],
                            HOST_PORTS[0]);
                    while (true) {
                        endpoint.startEP2();
                    }
                }
            } catch (Exception e) {
                System.out.println("Invalid entry.");
                e.printStackTrace();
            }
        }
        scanner.close();
    }

    public synchronized void startEP1() throws Exception {
        System.out.println("Message to be sent:");
        String message = scanner.nextLine();
        sendMessage(message);
    }

    public synchronized void startEP2() throws Exception {
        System.out.println("Waiting for massege from other EndPoint.");
        this.wait();
    }

    public void sendMessage(String message) {
        byte[] data = setMessage(message, dstNode, hostName, Node.MESSAGE);
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length);
            packet.setSocketAddress(nextFW);
            socket.send(packet);
            System.out.println("Message sent.");
        } catch (Exception e) {
            System.out.println("Message failed to send");
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data = packet.getData(); // On recieving the packet it extracts the message from the byte
                                        // array and displays it
        String message = new String(data);
        if (getType(message) == Node.MESSAGE) {
            /*
             * if (getType(message) == Node.CONTROLLER_INFORMATION) {
             * String[] s = getMessage(message).split(" ");
             * String nName = s[0];
             * int nPort = Integer.parseInt(s[1]);
             * nextFW = new InetSocketAddress(nName, nPort);
             * } else {
             */
            System.out.println("Message recieved: \n" + getMessage(message));
        }
        this.notify();
        // }
    }

}