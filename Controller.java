import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Scanner;

public class Controller extends Node {

    static final int hostPort = 60000;
    static final String hostNode = "controller";

    static private String[][] conNode = { { "ednpoint1", "forwarder1", null },
            { "forwarder1", "endpoint1", "forwarder2" },
            { "forwarder2", "forwarder1", "forwarder3" },
            { "forwarder3", "forwarder2", "endpoint2" },
            { "endpoint2", "forwarder3", null } };

    // static ArrayList<ArrayList<String>> conNode = new
    // ArrayList<ArrayList<String>>();

    // static ArrayList<ArrayList<Integer>> conPorts = new
    // ArrayList<ArrayList<Integer>>();

    static ArrayList<InetSocketAddress> connections = new ArrayList<InetSocketAddress>();

    static private int[][] conPorts = { { 50000, 50001, 0 },
            { 50001, 50000, 50002 },
            { 50002, 50001, 50003 },
            { 50003, 50002, 50004 },
            { 50004, 50003, 0 } };

    static int numNodes = 5;

    Controller(int host) {
        try {
            socket = new DatagramSocket(host);
            listener.go();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        /*
         * Scanner sc = new Scanner(System.in);
         * System.out.println("Please enter the number of containers:");
         * int number = sc.nextInt();
         * 
         * for (int i = 0; i < number; i++) {
         * conNode.add(new ArrayList<String>());
         * conPorts.add(new ArrayList<Integer>());
         * }
         * 
         * boolean done = false;
         * for (numNodes = 0; numNodes < number; numNodes++) {
         * System.out.println(
         * "Enter the container name and port number separated by space.");
         * conNode.add(new ArrayList<String>());
         * conNode.get(numNodes).add(sc.next());
         * conPorts.add(new ArrayList<Integer>());
         * conPorts.get(numNodes).add(sc.nextInt());
         * }
         * 
         * while (!done) {
         * System.out.println(
         * "Enter the container name and port number separated by space.\nOr the word exit to finish."
         * );
         * String tmp = sc.next();
         * if (tmp.toLowerCase().equals("exit")) {
         * done = true;
         * } else {
         * conNode.get(numNodes).add(tmp);
         * conPorts.get(numNodes).add(sc.nextInt());
         * numNodes++;
         * }
         * }
         * 
         * 
         */
        try {
            /*
             * 0
             * for (int i = 0; i < numNodes; i++) {
             * System.out.
             * println("Please enter the name(s) of containers (separated by space) connected to "
             * + conNode.get(i).get(0));
             * String connect = sc.nextLine();
             * String[] tmp = connect.split(" ");
             * String s = "";
             * for (int j = 0; i < tmp.length; i++) {
             * s = tmp[j];
             * conNode.get(i).add(s);
             * int counter = 0;
             * while (counter < numNodes && s != conNode.get(counter).get(0)) {
             * counter++;
             * }
             * 
             * if (counter < numNodes) {
             * conPorts.get(i).add(conPorts.get(counter).get(0));
             * } else {
             * conPorts.get(i).add(0);
             * }
             * 
             * s += " " + conPorts.get(i).get(j);
             * }
             * DatagramPacket packet = new DatagramPacket(s.getBytes(),
             * s.getBytes().length);
             * packet.setSocketAddress(connections.get(i));
             * socket.send(packet);
             * }
             */
            for (int i = 0; i < numNodes; i++) {
                connections.add(new InetSocketAddress(conNode[i][0], conPorts[i][0]));
            }
            Controller controller = new Controller(hostPort);
            System.out.println("Connection table set up, ready to go!");
            while (true) {
                controller.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // sc.close();
    }

    public synchronized void start() throws Exception {
        this.wait();
    }

    public void onReceipt(DatagramPacket packet) {
        String data = new String(packet.getData());
        int des = getDes(data);
        try {
            if (path(des, getHost(data))) {
                String table;
                String ports = "";
                String nodes = "";
                for (int i = 0; i < numNodes; i++) {
                    for (int j = 0; j < conNode[i].length; j++) {
                        nodes += conNode[i][j] + " ";
                        ports += conPorts[i][j] + " ";
                    }
                    nodes += ";";
                    ports += ";";
                }
                table = nodes + ":" + ports;
                byte[] conTable = setMessage(table, new InetSocketAddress(getHost(data)), socket,
                        Node.CONTROLLER_INFORMATION);
                packet = new DatagramPacket(conTable, conTable.length);
                for (int i = 0; i < numNodes; i++) {
                    if (conPorts[i][0] == getHost(data)) {
                        packet.setSocketAddress(connections.get(i));
                        socket.send(packet);
                        System.out.println("Table sent to " + conNode[i][0]);
                        i = numNodes;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean path(int des, int host) {
        boolean[] checked = new boolean[numNodes];
        for (int i = 0; i < numNodes; i++) {
            for (int j = 1; j < conPorts[i].length; j++) {
                if (!checked[i]) {
                    if (conPorts[i][j] == des) {
                        if (conPorts[i][0] == host) {
                            return true;
                        } else {
                            checked[i] = true;
                            des = conPorts[i][0];
                            i = 0;
                            j = 0;
                        }
                    }
                } else {
                    i++;
                    j = 0;
                }
            }
        }
        System.out.println("No connection!");
        return false;
    }

}
