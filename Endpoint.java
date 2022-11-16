import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Endpoint extends Node {

    // Setting the endpoints' and forwarders' name and port number
    static final int[] HOST_PORTS = { 50000, 50004 };
    static final String[] ENDPOINT_NODES = { "endpoint1", "endpoint2" };
    static final int[] NEXT_FW_PORTS = { 50001, 50003 };
    static final String[] NEXT_FW_NODES = { "forwarder1", "forwarder3" };

    // Creating a variable for the connected forwarder and the other endpoint
    InetSocketAddress nextFW;
    InetSocketAddress dstNode;

    // Contractor for the class
    Endpoint(int hostPort, String connectedNode, int connectedPort, String dstNode, int dstPort) {
        try {
            nextFW = new InetSocketAddress(connectedNode, connectedPort);
            this.dstNode = new InetSocketAddress(dstNode, dstPort);
            socket = new DatagramSocket(hostPort);
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
                    endpoint = new Endpoint(HOST_PORTS[0], NEXT_FW_NODES[0], NEXT_FW_PORTS[0], ENDPOINT_NODES[1],
                            HOST_PORTS[1]);
                    endpoint.startEP1();
                } else if (numberEP == 2) {
                    validInput = true;
                    endpoint = new Endpoint(HOST_PORTS[1], NEXT_FW_NODES[1], NEXT_FW_PORTS[1], ENDPOINT_NODES[0],
                            HOST_PORTS[0]);
                    endpoint.startEP2();
                }
            } catch (Exception e) {
                System.out.println("Invalid entry.");
            }
        }
        scanner.close();
    }

    public synchronized void startEP1() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Message to be sent:");
        String message = scanner.nextLine();
        sendMessage(message);
        System.out.println("Message sent.");
        scanner.close();
    }

    public synchronized void startEP2() throws Exception {
        System.out.println("Waiting for massege from other EndPoint.");
        this.wait();
    }

    public void sendMessage(String message) {
        try {
            byte[] tmp = message.getBytes(); // Turning message into a byte array
            byte[] data = new byte[126 + tmp.length]; // Creating byte array that's going to be sent containing all the
                                                      // data
            System.arraycopy(tmp, 0, data, 126, tmp.length); // Placing the message(bytes) into the sending byte array
                                                             // from byte 126

            tmp = ByteBuffer.allocate(4).putInt(dstNode.getPort()).array(); // Placing the destination port into the
                                                                            // sending byte array from byte 84
            System.arraycopy(tmp, 0, data, 84, tmp.length);

            tmp = ByteBuffer.allocate(4).putInt(socket.getPort()).array(); // Placing the source destination (local)
                                                                           // port into the sending byte array from byte
                                                                           // 42
            System.arraycopy(tmp, 0, data, 42, tmp.length);

            DatagramPacket packet = new DatagramPacket(data, data.length); // Sending packet to the connected forwarder
            packet.setSocketAddress(nextFW);
            socket.send(packet);
        } catch (Exception e) {
            System.out.println("Failed to send the packet");
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data = packet.getData(); // On recieving the packet it extracts the message from the byte
                                        // array and displays it
        byte[] tmp = new byte[data.length - 126];
        System.arraycopy(data, 126, tmp, 0, data.length - 126);
        String message = new String(tmp);
        System.out.println("Message recieved: \n" + message);
        this.notify();
    }

}