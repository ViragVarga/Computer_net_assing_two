import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Scanner;

public class Endpoint extends Node {
    static final int[] HOST_PORTS = { 50000, 50004 };
    static final String[] ENDPOINT_NODES = { "endpoint1", "endpoint2" };
    static final int[] NEXT_FW_PORTS = { 50001, 50003 };
    static final String[] NEXT_FW_NODES = { "forwarder1", "forwarder3" };
    InetSocketAddress nextFW;
    InetSocketAddress dstNode;

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
            System.out.println("Initialize the endpoint (1 or 2):");
            int numberEP = 0;
            try {
                numberEP = scanner.nextInt();
                if (numberEP == 1) {
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
            byte[] tmp = message.getBytes();
            byte[] data = new byte[126 + tmp.length];
            System.arraycopy(tmp, 0, data, 126, tmp.length);

            tmp = ByteBuffer.allocate(4).putInt(dstNode.getPort()).array();
            System.arraycopy(tmp, 0, data, 84, tmp.length);

            tmp = ByteBuffer.allocate(4).putInt(socket.getPort()).array();
            System.arraycopy(tmp, 0, data, 42, tmp.length);

            DatagramPacket packet = new DatagramPacket(data, data.length);
            packet.setSocketAddress(nextFW);
            socket.send(packet);
        } catch (Exception e) {
            System.out.println("Failed to send the packet");
            e.printStackTrace();
        }
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte[] tmp = new byte[data.length - 126];
        System.arraycopy(data, 126, tmp, 0, data.length - 126);
        String message = new String(tmp);
        System.out.println("Message from *ide majd visszakódolom a Port számot* : \n" + message);
        this.notify();
    }

}