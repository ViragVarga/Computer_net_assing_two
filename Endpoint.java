import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.lang.Thread;
import java.lang.Runnable;

public class Endpoint extends Node implements Runnable {

    // Setting the endpoints' and forwarders' name and port number
    static final int[] HOST_PORTS = { 50000, 50002, 50003, 50004 };
    static final String[] ENDPOINT_NODES = { "endpoint1", "endpoint2", "endpoint3", "endpoint4" };
    static final int NEXT_FW_PORTS = 54321;
    static final String[] NEXT_FW_NODES = { "forwarder1", "forwarder3", "forwarder4", "forwarder5" };

    // Creating a variable for the connected forwarder and the other endpoint
    static InetSocketAddress nextFW;
    String dstNode;

    Scanner scanner = new Scanner(System.in);
    String hostName;
    boolean sent = false;

    // Contractor for the class
    Endpoint(int hostPort, String hostName, String connectedNode, int connectedPort) {
        try {
            nextFW = new InetSocketAddress(connectedNode, connectedPort);
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
        Thread listenEP = null;
        while (!validInput) {
            System.out.println("Initialize the endpoint (1, 2, 3 or 4):"); // Differentiating the two endpoints
                                                                           // depending on
            // the number given
            int numberEP = 0;
            try {
                numberEP = scanner.nextInt();
                switch (numberEP) {
                    case 1:
                        validInput = true;

                        endpoint = new Endpoint(HOST_PORTS[0], ENDPOINT_NODES[0], NEXT_FW_NODES[0],
                                NEXT_FW_PORTS);
                        listenEP = new Thread(endpoint);

                        break;
                    case 2:
                        validInput = true;

                        endpoint = new Endpoint(HOST_PORTS[1], ENDPOINT_NODES[1], NEXT_FW_NODES[1],
                                NEXT_FW_PORTS);
                        listenEP = new Thread(endpoint);

                        break;
                    case 3:
                        validInput = true;

                        endpoint = new Endpoint(HOST_PORTS[2], ENDPOINT_NODES[2], NEXT_FW_NODES[2],
                                NEXT_FW_PORTS);
                        listenEP = new Thread(endpoint);

                        break;
                    case 4:
                        validInput = true;

                        endpoint = new Endpoint(HOST_PORTS[3], ENDPOINT_NODES[3], NEXT_FW_NODES[3],
                                NEXT_FW_PORTS);
                        listenEP = new Thread(endpoint);

                        break;
                    default:
                        System.out.println("Invalid entry.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Invalid entry.");
                e.printStackTrace();
            }
        }
        try {
            // listenEP.start();
            endpoint.sendEP();
        } catch (Exception e) {
            e.printStackTrace();
        }
        scanner.close();
    }

    public synchronized void run() {
        try {
            while (true) {
                byte[] data = new byte[PACKETSIZE];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                onReceipt(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendEP() throws Exception {
        while (true) {
            System.out.println("To send a message, type in the message:");
            String message = scanner.nextLine();
            boolean validInput = false;
            while (!validInput) {
                System.out.println("Which endpoint do you want to send to?");
                int dstNum = 0;
                try {
                    dstNum = scanner.nextInt();
                    if (dstNum > 0 && dstNum < 5) {
                        validInput = true;
                        dstNum--;
                        dstNode = ENDPOINT_NODES[dstNum];
                    } else {
                        System.out.println("Invalid entry");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sendMessage(message);
        }
    }

    /*
     * public synchronized void listenEP() throws Exception {
     * this.wait();
     * 
     * byte[] data = new byte[PACKETSIZE];
     * DatagramPacket packet = new DatagramPacket(data, data.length);
     * socket.receive(packet);
     * onReceipt(packet);
     *
     * }
     */

    public void sendMessage(String message) {
        byte[] data = setMessage(message, dstNode, hostName, Node.MESSAGE);

        try {

            while (!sent) {

                DatagramPacket packet = new DatagramPacket(data, data.length);
                packet.setSocketAddress(nextFW);
                socket.send(packet);
                synchronized (this) {
                    this.wait(5000);
                }
            }

            System.out.println("Message sent.");
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

        } catch (Exception e) {
            System.out.println("Message failed to send");
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        String message = new String(packet.getData());

        if (getType(message) == Node.MESSAGE) {
            System.out.println("Message recieved: \n" + getMessage(message));

            try {
                byte[] ack = setMessage("ACK", getHost(message), getDes(message), Node.ACK);
                packet = new DatagramPacket(ack, ack.length);
                packet.setSocketAddress(nextFW);
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (getType(message) == Node.ACK) {
            sent = true;
            notifyAll();
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
        }
        this.notify();
    }

}
