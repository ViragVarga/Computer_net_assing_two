import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Scanner;

public class Forwarder extends Node {

    // Setting the endpoints' and forwarders' name and port number
    static final int FW_PORTS = 54321;
    static final String[] FW_NODES = { "forwarder1", "forwarder2", "forwarder3", "forwarder4", "forwarder5" };
    static final int CONTROLLER_PORT = 60000;
    static final String CONTROLLER_NAME = "controller";
    static ArrayList<ArrayList<String>> conNames = new ArrayList<ArrayList<String>>();
    static ArrayList<ArrayList<Integer>> conPorts = new ArrayList<ArrayList<Integer>>();

    // Creating an array for the two connections each forwarder has
    InetSocketAddress nextAddress;
    InetSocketAddress controller = new InetSocketAddress(CONTROLLER_NAME, CONTROLLER_PORT);

    DatagramPacket packetOnHold = null;
    String localName;
    ArrayList<String> localCon = new ArrayList<String>();

    // Contractor for the class
    Forwarder(int localPort, String name) {
        try {
            socket = new DatagramSocket(localPort);
            localName = name;
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
            System.out.println("Initialize the forwarder (1, 2, 3, 4 or 5):"); // Differentiating the forwarder (with
                                                                               // its
            // connections) by user input number
            int numberFW = 0;
            try {
                numberFW = scanner.nextInt();
                if (numberFW > 0 && numberFW < 6) {
                    validInput = true;
                    numberFW--;
                    forwarder = new Forwarder(FW_PORTS, FW_NODES[numberFW]);
                } else {
                    System.out.println("Invalid entry");
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

        if (getType(data) == Node.ACK || getType(data) == Node.MESSAGE) {
            try {
                System.out.println("Packet recieved.");
                nextAddress = getNextPort(getDes(data));
                if (nextAddress != null) {
                    System.out.println(nextAddress.getHostName());
                    packet.setSocketAddress(nextAddress);
                    socket.send(packet);
                    System.out.println("Packet sent.");
                } else {
                    packetOnHold = packet;
                    byte[] query = setMessage("Query", getDes(data), localName, Node.CONTROLLER_INFORMATION);
                    DatagramPacket info = new DatagramPacket(query, query.length);
                    info.setSocketAddress(controller);
                    socket.send(info);
                }

            } catch (Exception e) {
                System.out.println("Failed to forward the packet");
                e.printStackTrace();
            }
        }

        if (getType(data) == Node.CONTROLLER_INFORMATION) {
            String s = getMessage(data);
            String[] separateNamesPorts = s.split(":");
            String[] rowNames = separateNamesPorts[0].split(";");
            for (int i = 0; i < rowNames.length; i++) {
                conNames.add(new ArrayList<String>());
                for (String string : rowNames[i].split(" ")) {
                    conNames.get(i).add(string);
                    if (conNames.get(i).get(0).contains(localName) && string != null) {
                        localCon.add(string);
                    }
                }
            }
            String[] rowPorts = separateNamesPorts[1].split(";");
            for (int i = 0; i < rowPorts.length; i++) {
                conPorts.add(new ArrayList<Integer>());
                for (String string : rowPorts[i].split(" ")) {
                    try {
                        conPorts.get(i).add(Integer.parseInt(string));
                    } catch (Exception e) {
                    }
                }
            }
            System.out.println("Table updated!");
        }

        if (packetOnHold != null) {
            data = new String(packetOnHold.getData());
            nextAddress = getNextPort(getDes(data));
            if (nextAddress != null) {
                try {
                    packetOnHold.setSocketAddress(nextAddress);
                    socket.send(packetOnHold);
                    packetOnHold = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        this.notify();
    }

    private InetSocketAddress getNextPort(String des) {

        if (conNames != null) {
            for (int i = 0; i < conNames.size(); i++) {
                if (conNames.get(i).get(0).equals(localName)) {
                    for (int j = 1; j < conNames.get(i).size(); j++) {
                        if (conNames.get(i).get(j).equals(des)) {
                            return new InetSocketAddress(conNames.get(i).get(j), conPorts.get(i).get(j));
                        }
                    }
                }
            }
            for (int i = 0; i < conNames.size(); i++) {
                if (conNames.get(i).contains(localName)) {
                    for (int j = 1; j < conNames.get(i).size(); j++) {
                        if (conNames.get(i).get(j).equals(des)) {
                            return new InetSocketAddress(conNames.get(i).get(0), conPorts.get(i).get(0));
                        }
                    }
                }
            }
            for (int i = 0; i < conNames.size(); i++) {
                for (int j = 1; j < conNames.get(i).size(); j++) {
                    if (conNames.get(i).get(j).equals(des)) {
                        for (int tmp = 0; tmp < conNames.get(i).size(); tmp++) {
                            if (localCon.contains(conNames.get(i).get(tmp))) {
                                return new InetSocketAddress(conNames.get(i).get(tmp), conPorts.get(i).get(tmp));
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
