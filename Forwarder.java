import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Scanner;

public class Forwarder extends Node {

    // Setting the endpoints' and forwarders' name and port number
    static final int[] FW_PORTS = { 50001, 50002, 50003 };
    static final String[] FW_NODES = { "forwarder1", "forwarder2", "forwarder3" };
    static final int CONTROLLER_PORT = 60000;
    static final String CONTROLLER_NAME = "controller";
    static ArrayList<ArrayList<String>> conNames;
    static ArrayList<ArrayList<Integer>> conPorts;

    // Creating an array for the two connections each forwarder has
    InetSocketAddress nextAddress;
    InetSocketAddress controller = new InetSocketAddress(CONTROLLER_NAME, CONTROLLER_PORT);

    // Contractor for the class
    Forwarder(int localPort) {
        try {
            socket = new DatagramSocket(localPort);
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
                    forwarder = new Forwarder(FW_PORTS[0]);
                } else if (numberFW == 2) {
                    validInput = true;
                    forwarder = new Forwarder(FW_PORTS[1]);
                } else if (numberFW == 3) {
                    validInput = true;
                    forwarder = new Forwarder(FW_PORTS[2]);
                }
            } catch (Exception e) {
                System.out.println("Invalid entry");
            }
        }
        scanner.close();
        while (true) {
            forwarder.start();
        }
    }

    public synchronized void start() throws Exception {
        System.out.println("Forwarder's ready to recieve and forward packet(s)");
        this.wait();
    }

    public synchronized void onReceipt(DatagramPacket packet) {
        String data = new String(packet.getData());
        if (getType(data) == Node.MESSAGE) {
            System.out.println("Packet recieved.");
            try {
                nextAddress = getNextPort(socket, getDes(data));
                if (nextAddress != null) {
                    packet.setSocketAddress(nextAddress);
                    socket.send(packet);
                    System.out.println("Packet sent.");
                } else {
                    byte[] query = setMessage(Integer.toString(getDes(data)), controller, socket,
                            CONTROLLER_INFORMATION);
                    DatagramPacket info = new DatagramPacket(query, query.length);
                    info.setSocketAddress(controller);
                    socket.send(info);
                    socket.receive(packet);
                }

            } catch (Exception e) {
                System.out.println("Failed to forward the packet");
            }
        }

        if (getType(data) == Node.CONTROLLER_INFORMATION) {
            String s = getMessage(data);
            String[] separateNamesPorts = s.split(":");
            String[] rowNames = separateNamesPorts[0].split(";");
            for (int i = 0; i < rowNames.length; i++) {
                for (String string : rowNames[i].split(" ")) {
                    conNames.get(i).add(string);
                }
            }
            String[] rowPorts = separateNamesPorts[1].split(";");
            for (int i = 0; i < rowPorts.length; i++) {
                for (String string : rowPorts[i].split(" ")) {
                    conPorts.get(i).add(Integer.parseInt(string));
                }
            }
            System.out.println("Table updated!");
        }
        this.notify();
    }

    private InetSocketAddress getNextPort(DatagramSocket socket, int des) {
        for (int i = 0; i < conPorts.size(); i++) {
            for (int j = 1; j < conPorts.get(i).size(); j++) {
                if (conPorts.get(i).get(j) == des) {
                    if (conPorts.get(i).get(0) == socket.getPort()) {
                        return new InetSocketAddress(conPorts.get(i).get(j));
                    } else {
                        des = conPorts.get(i).get(0);
                        i = 0;
                        j = 0;
                    }
                }
            }
        }
        return null;
    }
}
