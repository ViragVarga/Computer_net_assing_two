import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Scanner;

public class Controller extends Node {

    static final int hostPort = 60000;
    static final String hostNode = "controller";

    /*
     * private String[][] conNodes; = { { "ednpoint1", "forwarder1", null },
     * { "forwarder1", "endpoint1", "forwarder2" },
     * { "forwarder2", "forwarder1", "forwarder3" },
     * { "forwarder3", "forwarder2", "endpoint2" },
     * { "endpoint2", "forwarder3", null } };
     */
    static ArrayList<ArrayList<String>> conNode = new ArrayList<ArrayList<String>>();

    static ArrayList<ArrayList<Integer>> conPorts = new ArrayList<ArrayList<Integer>>();

    static ArrayList<InetSocketAddress> connections = new ArrayList<InetSocketAddress>();
    /*
     * private int[][] conPorts; = { { 50000, 50001, 0 },
     * { 50001, 20000, 50002 },
     * { 50002, 50001, 50003 },
     * { 50003, 50002, 50004 },
     * { 50004, 50003, 0 } };
     */
    static int numNodes = 0;

    Controller(int host) {
        try {
            socket = new DatagramSocket(host);
            listener.go();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Please enter the number of containers:");
        int number = sc.nextInt();
        /*
         * for (int i = 0; i < number; i++) {
         * conNode.add(new ArrayList<String>());
         * conPorts.add(new ArrayList<Integer>());
         * }
         */
        // boolean done = false;
        for (numNodes = 0; numNodes < number; numNodes++) {
            System.out.println(
                    "Enter the container name and port number separated by space.");
            conNode.add(new ArrayList<String>());
            conNode.get(numNodes).add(sc.next());
            conPorts.add(new ArrayList<Integer>());
            conPorts.get(numNodes).add(sc.nextInt());
        }
        /*
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
         */

        try {
            for (int i = 0; i < numNodes; i++) {
                connections.add(new InetSocketAddress(conNode.get(i).get(0), conPorts.get(i).get(0)));
                System.out.println("Please enter the name(s) of containers (separated by space) connected to "
                        + conNode.get(i).get(0));
                String connect = sc.nextLine();
                String[] tmp = connect.split(" ");
                String s = "";
                for (int j = 0; i < tmp.length; i++) {
                    s = tmp[j];
                    conNode.get(i).add(s);
                    int counter = 0;
                    while (counter < numNodes && s != conNode.get(counter).get(0)) {
                        counter++;
                    }

                    if (counter < numNodes) {
                        conPorts.get(i).add(conPorts.get(counter).get(0));
                    } else {
                        conPorts.get(i).add(0);
                    }

                    s += " " + conPorts.get(i).get(j);
                }
                DatagramPacket packet = new DatagramPacket(s.getBytes(), s.getBytes().length);
                packet.setSocketAddress(connections.get(i));
                socket.send(packet);
            }
            Controller controller = new Controller(hostPort);
            System.out.println("Connection table set up, ready to go!");
            while (true) {
                controller.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sc.close();
    }

    public synchronized void start() throws Exception {
        this.wait();
    }

    public void onReceipt(DatagramPacket packet) {
        String data = new String(packet.getData());
        int des = Integer.parseInt(getMessage(data));
        if (path(des, getHost(data))) {
            String table;
            String ports = "";
            String nodes = "";
            for (int i = 0; i < conNode.size(); i++) {
                for (int j = 0; j < conNode.get(i).size(); j++) {
                    nodes += conNode.get(i).get(j) + " ";
                    ports += conPorts.get(i).get(j) + " ";
                }
                nodes += ";";
                ports += ";";
            }
            table = nodes + ":" + ports;
            try {
                byte[] conTable = setMessage(table, new InetSocketAddress(getHost(data)), socket,
                        Node.CONTROLLER_INFORMATION);
                packet = new DatagramPacket(conTable, conTable.length);
                for (int i = 0; i < conPorts.size(); i++) {
                    if (conPorts.get(i).get(0) == getHost(data)) {
                        packet.setSocketAddress(connections.get(i));
                        i = conPorts.size();
                    }
                }
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private boolean path(int des, int host) {
        boolean[] checked = new boolean[conPorts.size()];
        for (int i = 0; i < conPorts.size(); i++) {
            for (int j = 1; j < conPorts.get(i).size(); j++) {
                if (!checked[i]) {
                    if (conPorts.get(i).get(j) == des) {
                        if (conPorts.get(i).get(0) == host) {
                            return true;
                        } else {
                            checked[i] = true;
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
