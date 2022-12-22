import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.lang.Thread;
import java.lang.Runnable;

public class Endpoint extends Node {

    // Setting the endpoints' and forwarders' name and port number
    static final int[] HOST_PORTS = { 50000, 50004 };
    static final String[] ENDPOINT_NODES = { "endpoint1", "endpoint2" };
    static final int NEXT_FW_PORTS = 54321;
    static final String[] NEXT_FW_NODES = { "forwarder1", "forwarder3" };

    // Creating a variable for the connected forwarder and the other endpoint
    static InetSocketAddress nextFW;
    InetSocketAddress dstNode;

    Scanner scanner = new Scanner(System.in);
    String hostName;
    boolean sent = false;

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
                switch (numberEP) {
                    case 1:
                        validInput = true;
                        endpoint = new Endpoint(HOST_PORTS[0], ENDPOINT_NODES[0], NEXT_FW_NODES[0], NEXT_FW_PORTS,
                                ENDPOINT_NODES[1], HOST_PORTS[1]);
                        break;
                    case 2:
                        validInput = true;
                        endpoint = new Endpoint(HOST_PORTS[1], ENDPOINT_NODES[1], NEXT_FW_NODES[1], NEXT_FW_PORTS,
                                ENDPOINT_NODES[0],
                                HOST_PORTS[0]);
                        break;
                    default:
                        System.out.println("Invalid entry.");
                        break;
                }
                Thread listenThread = new Thread(new ListenThread(socket, nextFW));
                listenThread.start();
                while (true) {
                    endpoint.startEP();
                }
            } catch (Exception e) {
                System.out.println("Invalid entry.");
                e.printStackTrace();
            }
        }
        scanner.close();
    }

    public void startEP() throws Exception {
        System.out.println("Message to be sent:");
        String message = scanner.nextLine();
        sendMessage(message);
    }

    public synchronized void sendMessage(String message) {
        byte[] data = setMessage(message, dstNode, hostName, Node.MESSAGE);

        try {

            while (!sent) {

                DatagramPacket packet = new DatagramPacket(data, data.length);
                packet.setSocketAddress(nextFW);
                socket.send(packet);

                this.wait(3000);
            }

            System.out.println("Message sent.");

        } catch (Exception e) {
            System.out.println("Message failed to send");
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        String message = new String(packet.getData());
        System.out.println("Packet recieved");
        if (getType(message) == Node.ACK) {
            sent = true;
        }
        /*
         * System.out.println("Checkpoint 4");
         * byte[] data = packet.getData(); // On recieving the packet it extracts the
         * message from the byte
         * // array and displays it
         * String message = new String(data);
         * if (getType(message) == Node.MESSAGE) {
         * /*
         * if (getType(message) == Node.CONTROLLER_INFORMATION) {
         * String[] s = getMessage(message).split(" ");
         * String nName = s[0];
         * int nPort = Integer.parseInt(s[1]);
         * nextFW = new InetSocketAddress(nName, nPort);
         * } else {
         * 
         * System.out.println("Message recieved: \n" + getMessage(message));
         * }
         * System.out.println("Checkpoint 5");
         * this.notify();
         * // }
         */
    }

}

class ListenThread extends Node implements Runnable {

    private DatagramSocket socket;
    private InetSocketAddress next;

    ListenThread(DatagramSocket socket, InetSocketAddress next) {
        this.socket = socket;
        this.next = next;
    }

    public void run() {
        try {
            byte[] data = new byte[PACKETSIZE];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            onReceipt(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onReceipt(DatagramPacket packet) {
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

            synchronized (this) {
                for (int i = 0; i < 2; i++) {
                    try {
                        byte[] ack = setMessage("ACK", getHost(message), getDes(message), Node.ACK);
                        packet = new DatagramPacket(ack, ack.length);
                        packet.setSocketAddress(next);
                        socket.send(packet);
                        // System.out.println("ACK sent");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // }
    }
}
