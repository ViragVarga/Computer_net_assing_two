import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Controller extends Node {

    static final int hostPort = 60000;
    static final String hostNode = "controller";

    static private String[][] conNames = {
            { "endpoint1", "forwarder1", null, null, null },
            { "endpoint2", "forwarder3", null, null, null },
            { "endpoint3", "forwarder4", null, null, null },
            { "endpoint4", "forwarder5", null, null, null },
            { "forwarder1", "endpoint1", "forwarder2", "forwarder4", null },
            { "forwarder2", "forwarder1", "forwarder3", "forwarder4", "forwarder5" },
            { "forwarder3", "forwarder2", "endpoint2", null, null },
            { "forwarder4", "forwarder1", "forwarder2", "endpoint3", null },
            { "forwarder5", "forwarder2", "endpoint4", null, null } };

    static ArrayList<InetSocketAddress> connections = new ArrayList<InetSocketAddress>();

    static private int[][] conPorts = {
            { 50000, 54321, 0, 0, 0 },
            { 50002, 54321, 0, 0, 0 },
            { 50003, 54321, 0, 0, 0 },
            { 50004, 54321, 0, 0, 0 },
            { 54321, 50000, 54321, 54321, 0 },
            { 54321, 54321, 54321, 54321, 54321 },
            { 54321, 54321, 50002, 0, 0 },
            { 54321, 54321, 54321, 50003, 0 },
            { 54321, 54321, 50004, 0, 0 } };

    static int numNodes = 9;

    Controller(int host) {
        try {
            socket = new DatagramSocket(host);
            listener.go();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Controller controller = new Controller(hostPort);
            for (int i = 0; i < numNodes; i++) {
                connections.add(new InetSocketAddress(conNames[i][0], conPorts[i][0]));
            }
            System.out.println("Connection table set up, ready to go!");
            while (true) {
                controller.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void start() throws Exception {
        this.wait();
    }

    public void onReceipt(DatagramPacket packet) {
        String data = new String(packet.getData());
        String des = getDes(data);
        try {
            if (path(des, getHost(data))) {
                String table;
                String ports = "";
                String nodes = "";
                for (int i = 0; i < numNodes; i++) {
                    for (int j = 0; j < conNames[i].length; j++) {
                        nodes += conNames[i][j] + " ";
                        ports += conPorts[i][j] + " ";
                    }
                    nodes += ";";
                    ports += ";";
                }
                table = nodes + ":" + ports;
                byte[] conTable = setMessage(table, getHost(data), "controller",
                        Node.CONTROLLER_INFORMATION);
                packet = new DatagramPacket(conTable, conTable.length);
                for (int i = 0; i < numNodes; i++) {
                    if (conNames[i][0].contains(getHost(data))) {
                        packet.setSocketAddress(connections.get(i));
                        socket.send(packet);
                        System.out.println("Table sent to " + conNames[i][0]);
                        i = numNodes;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean path(String des, String host) {
        boolean[] checked = new boolean[numNodes];
        ArrayList<String> donePath = new ArrayList<String>();
        donePath.add(des);
        ArrayList<String> hostConn = new ArrayList<String>();
        for (int i = 0; i < numNodes; i++) {
            if (conNames[i][0].contains(host)) {
                for (String s : conNames[i]) {
                    hostConn.add(s);
                }
            }
        }
        for (int i = 0; i < numNodes; i++) {
            for (int j = 1; j < conPorts[i].length; j++) {
                if (!checked[i]) {
                    if (conNames[i][j] != null && conNames[i][j].contains(des)) {
                        for (int tmp = 1; tmp < conNames[i].length; tmp++) {
                            if (hostConn.contains(conNames[i][tmp]))
                                return true;
                        }
                        if (conNames[i][0].contains(host) || hostConn.contains(conNames[i][0])) {
                            return true;
                        } else if (!donePath.contains(conNames[i][0])) {
                            checked[i] = true;
                            des = conNames[i][0];
                            donePath.add(des);
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
        System.out.println("No connection between " + host + " and " + des + "!");
        return false;
    }

}
