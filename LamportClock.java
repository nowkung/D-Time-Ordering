import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.lang.Thread;
import java.util.*;

public class LamportClock extends Thread {

    private int id;
    private MulticastSocket sock;
    private InetAddress group;
    private int port;

    // local time of a process
    private int time;

    // order of the process (viewed from the master's perspective)
    private int order;

    public LamportClock(InetAddress group, int port) throws Exception {
        this.group = group;
        this.port = port;

        // if we don't assign an order to a process
        this.order = -1;

        // set local time to random
        Random rand = new Random();
        // this.time = rand.nextInt(10);
        this.time = 0;

        sock = new MulticastSocket(port);
        sock.setTimeToLive(2);
        sock.joinGroup(group);
    }

    public LamportClock(InetAddress group, int port, int order, int id) throws Exception {
        this(group, port);
        this.order = order;
        this.id = id;
    }

    public int getOrder() {
        return this.order;
    }

    public int getTime() {
        return this.time;
    }

    public long getId() {
        return this.id;
    }

    public int localEvent() {
        ++this.time;
        // System.out.println(this.getId() + " performing local event. local time is " + this.time);
        return this.time;
    }

    public int receivedEvent(long senderId, int receivedTime) {
        // System.out.println(this.getId() + " received message from "
        //     + senderId + ". local time is " + this.time);

        return this.time;
    }

    public int sendEvent(String msg) throws Exception {
        byte[] data = msg.getBytes();

        DatagramPacket d = new DatagramPacket(data, data.length, group, port);
        sock.send(d);

        return this.time;
    }

    public void updateTime(Event e) throws Exception {
        int type = e.type;

        switch (type) {
            // LOCAL EVENT
            case 0:
                this.localEvent();
                break;

            // SEND EVENT
            case 1: // extract information from the event
                long senderId = e.senderId;
                long receiverId = e.receiverId;
                // increase the time first before sending the message
                e.localTime = ++this.time;
                Object content = e.content;

                /** send a message of the following format
                 * SENDER_ID|RECEIVER_ID|LOCAL_TIME
                 */
                String msg = Long.toString(senderId) + "-" + Long.toString(receiverId)
                        + "-" + e.localTime + "-" + content;
                sendEvent(msg);
                break;

            // RECEIVE EVENT
            case 2:
                // update its logical clock
                this.time = Math.max(e.localTime, this.time) + 1;
                break;


            // ACK EVENT
            case 3:
                // update its local clock
                ++this.time;
                break;

            // ACK EVENT
            default:
                break;
        }

        printTime(e);
    }

    public void printTime(Event e) {
        String logging = "-------------------------\n";
        logging += "User " + this.id + "\n";
        logging += "Event " + this.getTime() + "\n";
        logging += "\tEvent type: ";

        switch (e.type) {
            case 0:
                logging += "LOCAL EVENT\n";
                break;
            case 1:
                logging += "SEND EVENT\n";
                break;
            case 2:
                logging += "RECEIVE EVENT\n";
                break;
            case 3:
                logging += "ACK EVENT\n";
                break;
            default:
                break;
        }

        logging += "\tEvent sender's ID: " + "User " + e.senderId + "|";
        logging += "\tEvent receiver's ID: " + "User " + e.receiverId + "|";
        logging += "\tEvent content: " + e.content + "|";
        logging += "\tTimestamp: " + e.localTime + "\n";
        logging += "-------------------------\n";

        System.out.print(logging);
    }

    public void run() {
        String greeting = "";
        greeting = "User " + this.id + " Ready!";
        System.out.println(greeting);

        try {
            while (true) {
                DatagramPacket d = new DatagramPacket(new byte[256], 256);
                sock.receive(d);
                String s = new String(d.getData());
                // System.out.println(this.getId() + " received " + s);

                String[] meta = s.trim().split("-");


                int senderId = Integer.parseInt(meta[0]);
                int receiverId = Integer.parseInt(meta[1]);
                int localTime = Integer.parseInt(meta[2]);
                String content = "";
                // if there is a message
                if (meta.length >= 4)
                    content = meta[3];

                if (this.id == receiverId) {
                    Event e = new Event(2, senderId, receiverId, localTime, content);
                    updateTime(e);
                }
            }


        } catch (
                Exception e) {
            System.err.println("LC Failed: " + e);
            return;
        }
    }

}