import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class Main {

    public static void main(String[] args) {

//        if (args.length == 0) {
//            System.out.println("Usage: java Main (number of processses) [filename of commands]");
//            return;
//        }

        // TODO: add support for reading commands from a file

        String input;

        try {
//            int n = Integer.parseInt(args[0]);
            int n = 3;
            LamportClock[] clocks = new LamportClock[n];
            System.setProperty("java.net.preferIPv4Stack", "true");
            InetAddress group = InetAddress.getByName("224.255.255.255");
            for (int i = 0; i < 3; ++i) {
                int port = 8888;
                LamportClock lc = new LamportClock(group, port, i, (i + 1));
                lc.start();
                clocks[i] = lc;
            }
            List<List<Object>> chat12 = new ArrayList<List<Object>>();
            List<List<Object>> chat13 = new ArrayList<List<Object>>();
            List<List<Object>> chat23 = new ArrayList<List<Object>>();
            List<List<Object>> splits = new ArrayList<List<Object>>();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                input = in.readLine();
                if (input.equals("exit"))
                    return;

                /**
                 * A message format is of the following:
                 * EVENT_NAME ID_OF_SENDER (ID_OF_RECEIVER)
                 *
                 * EVENT_NAME is of the following:
                 *  - SEND
                 *  - LOCAL
                 *
                 * For example:
                 * SEND 1 2 (process 1 sends a message to process 2)
                 * LOCAL 3 (process 3 performs a local event)
                 */
                // perform a string split operation based on space
                String[] temp = input.split(" ");
                List<Object> event = new ArrayList<Object>();
                for (int a = 0; a < temp.length; a++) {
                    event.add(temp[a]);
                }
                splits.add(event);
//                String[] splits = input.split(" ");
                if (splits.size() == 0) {
                    continue;
                }
                if (splits.get(splits.size() - 1).get(0).toString().equalsIgnoreCase("RUN")) {
                    splits.remove(splits.size() - 1);
                    for (int i = 0; i < splits.size(); i++) {
                        switch (splits.get(i).get(0).toString().toUpperCase()) {

                            case "SEND":
                                int clockArrayId = Integer.parseInt(splits.get(i).get(1).toString());
                                long firstProcessId = clocks[clockArrayId].getId();
                                long secondProcessId = clocks[Integer.parseInt(splits.get(i).get(2).toString())].getId();
                                int delay = Integer.parseInt(splits.get(i).get(3).toString()) * 1000;
                                String messageContent = "";
                                if (splits.get(i).size() >= 4) {
                                    List<String> wordsList = new ArrayList<>();
                                    for (int j = 4; j < splits.get(i).size(); j++) {
                                        wordsList.add(splits.get(i).get(j).toString());
                                    }
                                    messageContent = String.join(" ", wordsList);
                                }

                                Event e = new Event(1, firstProcessId, secondProcessId, messageContent);
                                Thread.sleep(delay);
                                clocks[clockArrayId].updateTime(e);
                                List<Object> list = new ArrayList<Object>();
                                list.add(firstProcessId);
                                list.add(secondProcessId);
                                list.add(messageContent);
                                list.add(clocks[clockArrayId].getTime());
                                if ((firstProcessId == 1 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 1)) {
                                    chat12.add(list);
                                    Thread.sleep(2000);
                                    System.out.print("Chat 1 and 2: ");
                                    for(int b = 0; b < chat12.size();b++){
                                        System.out.print(chat12.get(b).get(2)+", ");
                                    }
                                    System.out.println();
                                }
                                if ((firstProcessId == 1 && secondProcessId == 3) || (firstProcessId == 3 && secondProcessId == 1)) {
                                    chat13.add(list);
                                    Thread.sleep(2000);
                                    System.out.print("Chat 1 and 3: ");
                                    for(int b = 0; b < chat13.size();b++){
                                        System.out.print(chat13.get(b).get(2)+", ");
                                    }
                                    System.out.println();
                                }
                                if ((firstProcessId == 3 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 3)) {
                                    chat23.add(list);
                                    Thread.sleep(2000);
                                    System.out.print("Chat 2 and 3: ");
                                    for(int b = 0; b < chat23.size();b++){
                                        System.out.print(chat23.get(b).get(2)+", ");
                                    }
                                    System.out.println();
                                }
                                break;

                            case "LOCAL":
                                clockArrayId = Integer.parseInt(splits.get(i).get(1).toString());
                                firstProcessId = clocks[clockArrayId].getId();
                                secondProcessId = 0;
                                messageContent = "";

                                e = new Event(0, firstProcessId, secondProcessId, messageContent);
                                clocks[clockArrayId].updateTime(e);
                                break;

                            case "FORWARD":
                                int chat1 = Integer.parseInt(splits.get(i).get(1).toString()) + 1;
                                int chat2 = Integer.parseInt(splits.get(i).get(2).toString()) + 1;
                                firstProcessId = chat1;
                                secondProcessId = Integer.parseInt(splits.get(i).get(3).toString()) + 1;
                                Object[] forwardChat;
                                if ((chat1 == 1 && chat2 == 2) || (chat1 == 2 && chat2 == 1)) {
                                    forwardChat = new Object[chat12.size()];
                                    for (int k = 0; k < chat12.size(); k++) {
                                        forwardChat[k] = chat12.get(k).get(3);
                                    }
                                    Arrays.sort(forwardChat);
                                    for (int k = 0; k < forwardChat.length; k++) {
                                        for (int j = 0; j < chat12.size(); j++) {
                                            if (forwardChat[k] == chat12.get(j).get(3)) {
                                                forwardChat[k] = chat12.get(j).get(2);
                                            }
                                        }
                                    }
                                    for (int j = 0; j < forwardChat.length; j++) {
                                        e = new Event(1, firstProcessId, secondProcessId, forwardChat[j].toString());
                                        clocks[chat1 - 1].updateTime(e);
                                        list = new ArrayList<Object>();
                                        list.add(firstProcessId);
                                        list.add(secondProcessId);
                                        list.add(forwardChat[j]);
                                        list.add(clocks[chat1 - 1].getTime());
                                        if ((firstProcessId == 1 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 1)) {
                                            chat12.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 1 and 2: ");
                                            for(int b = 0; b < chat12.size();b++){
                                                System.out.print(chat12.get(b).get(2)+", ");
                                            }
                                            System.out.println(" ");
                                        }
                                        if ((firstProcessId == 1 && secondProcessId == 3) || (firstProcessId == 3 && secondProcessId == 1)) {
                                            chat13.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 1 and 3: ");
                                            for(int b = 0; b < chat13.size();b++){
                                                System.out.print(chat13.get(b).get(2)+", ");
                                            }
                                            System.out.println(" ");
                                        }
                                        if ((firstProcessId == 3 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 3)) {
                                            chat23.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 2 and 3: ");
                                            for(int b = 0; b < chat23.size();b++){
                                                System.out.print(chat23.get(b).get(2)+", ");
                                            }
                                            System.out.println(" ");
                                        }
                                    }
                                }
                                if ((chat1 == 1 && chat2 == 3) || (chat1 == 3 && chat2 == 1)) {
                                    forwardChat = new Object[chat13.size()];
                                    for (int k = 0; k < chat13.size(); k++) {
                                        forwardChat[k] = chat13.get(k).get(3);
                                    }
                                    Arrays.sort(forwardChat);
                                    for (int k = 0; k < forwardChat.length; k++) {
                                        for (int j = 0; j < chat13.size(); j++) {
                                            if (forwardChat[k] == chat13.get(j).get(3)) {
                                                forwardChat[k] = chat13.get(j).get(2);
                                            }
                                        }
                                    }
                                    for (int j = 0; j < forwardChat.length; j++) {
                                        e = new Event(1, firstProcessId, secondProcessId, forwardChat[j].toString());
                                        clocks[chat1 - 1].updateTime(e);
                                        list = new ArrayList<Object>();
                                        list.add(firstProcessId);
                                        list.add(secondProcessId);
                                        list.add(forwardChat[j]);
                                        list.add(clocks[chat1 - 1].getTime());

                                        if ((firstProcessId == 1 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 1)) {
                                            chat12.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 1 and 2: ");
                                            for(int b = 0; b < chat12.size();b++){
                                                System.out.print(chat12.get(b).get(2)+", ");
                                            }
                                            System.out.println();
                                        }
                                        if ((firstProcessId == 1 && secondProcessId == 3) || (firstProcessId == 3 && secondProcessId == 1)) {
                                            chat13.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 1 and 3: ");
                                            for(int b = 0; b < chat13.size();b++){
                                                System.out.print(chat13.get(b).get(2)+", ");
                                            }
                                            System.out.println();
                                        }
                                        if ((firstProcessId == 3 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 3)) {
                                            chat23.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 2 and 3: ");
                                            for(int b = 0; b < chat23.size();b++){
                                                System.out.print(chat23.get(b).get(2)+", ");
                                            }
                                            System.out.println();
                                        }
                                    }
                                }
                                if ((chat1 == 3 && chat2 == 2) || (chat1 == 2 && chat2 == 3)) {
                                    forwardChat = new Object[chat23.size()];
                                    for (int k = 0; k < chat23.size(); k++) {
                                        forwardChat[k] = chat23.get(k).get(3);
                                    }
                                    Arrays.sort(forwardChat);
                                    for (int k = 0; k < forwardChat.length; k++) {
                                        for (int j = 0; j < chat23.size(); j++) {
                                            if (forwardChat[k] == chat23.get(j).get(3)) {
                                                forwardChat[k] = chat23.get(j).get(2);
                                            }
                                        }
                                    }
                                    for (int j = 0; j < forwardChat.length; j++) {
                                        e = new Event(1, firstProcessId, secondProcessId, forwardChat[j].toString());
                                        clocks[chat1 - 1].updateTime(e);
                                        list = new ArrayList<Object>();
                                        list.add(firstProcessId);
                                        list.add(secondProcessId);
                                        list.add(forwardChat[j]);
                                        list.add(clocks[chat1 - 1].getTime());
                                        if ((firstProcessId == 1 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 1)) {
                                            chat12.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 1 and 2: ");
                                            for(int b = 0; b < chat12.size();b++){
                                                System.out.print(chat12.get(b).get(2)+", ");
                                            }
                                            System.out.println();
                                        }
                                        if ((firstProcessId == 1 && secondProcessId == 3) || (firstProcessId == 3 && secondProcessId == 1)) {
                                            chat13.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 1 and 3: ");
                                            for(int b = 0; b < chat13.size();b++){
                                                System.out.print(chat13.get(b).get(2)+", ");
                                            }
                                            System.out.println();
                                        }
                                        if ((firstProcessId == 3 && secondProcessId == 2) || (firstProcessId == 2 && secondProcessId == 3)) {
                                            chat23.add(list);
                                            Thread.sleep(2000);
                                            System.out.print("Chat 2 and 3: ");
                                            for(int b = 0; b < chat23.size();b++){
                                                System.out.print(chat23.get(b).get(2)+", ");
                                            }
                                            System.out.println();
                                        }
                                    }
                                }

                                break;


                            default:
                                throw new RuntimeException("Invalid event name");

                        }
                    }
                    splits.clear();
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            return;
        }
    }

}
