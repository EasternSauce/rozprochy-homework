package chatTcpUdp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;


public class ChatClient {
    private JTextArea chatArea;
    private final String hostName = "localhost";
    private final int portNumber = 12345;
    private Socket tcpSocket;
    private String username = null;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton button;
    private ArrayList<String> asciiArt;
    private final String multicastAddr = "224.0.0.3";
    private final int multicastPort = 8888;
    private MulticastSocket multicastSocket;
    private final boolean scrollAutomatically = true;


    DatagramSocket udpSocket = null;


    public ChatClient() {

        //create tcp socket
        try {
            tcpSocket = new Socket(hostName, portNumber);
        } catch (ConnectException e) {
            System.out.print("Connection refused, exiting");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //create udp socket
        try {
            udpSocket = new DatagramSocket();
        } catch (SocketException e) {

            e.printStackTrace();
        }

        //create udp multicast socket
        try {
            multicastSocket = new MulticastSocket(multicastPort);
            InetAddress address = InetAddress.getByName(multicastAddr);
            multicastSocket.joinGroup(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Chat Client GUI");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(1200, 800));

        chatArea = new JTextArea("Welcome to the chat", 19, 1);
        chatArea.setFont(new Font(chatArea.getFont().getName(), Font.PLAIN, 24));

        chatArea.append("\nPlease input name");

        chatArea.setEditable(false);




        //make it so chat is scrollable

        scrollPane = new JScrollPane(chatArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel pane = new JPanel(new BorderLayout());

        pane.add(scrollPane, BorderLayout.NORTH);


        //field to input messsages into

        inputField = new JTextField();


        //handle input on pressing enter

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    handleInput(inputField.getText());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                inputField.setText("");
            }
        });

        inputField.setFont(new Font(chatArea.getFont().getName(), Font.PLAIN, 24));

        pane.add(inputField, BorderLayout.CENTER);



        //send button as alternative to pressing enter

        button = new JButton("Send");
        button.setFont(new Font(chatArea.getFont().getName(), Font.PLAIN, 24));

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    handleInput(inputField.getText());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                inputField.setText("");
            }
        });


        pane.add(button, BorderLayout.SOUTH);

        frame.getContentPane().add(pane);

        frame.pack();
        frame.setVisible(true);
    }

    private void handleInput(String text) throws IOException {

        //at the start the username is null, so get the username from the user

        if(username == null){
            username = text;
            chatArea.append("\nYour name is " + username + "\nConnected");
        }


        else{

            //M command: sending the asciiart as a udp datagram to the server, messages start with M, so the server knows its not multicast

            if(text.equals("M")) {
                for(String line : asciiArt) {
                    chatArea.append("\n" + username + ": " + line);
                    try {
                        byte[] sendBuffer = ("M" + username + ": " + line).getBytes();
                        InetAddress address = InetAddress.getByName("localhost");
                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
                        udpSocket.send(sendPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                if(scrollAutomatically) {

                    scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
                }



            }


            //N command: sending the asciiart as a udp datagram to the server, messages start with N, so the server knows its multicast

            else if (text.equals("N")) {

                for(String line : asciiArt) {
                    try {
                        byte[] sendBuffer = ("N" + username + ": " + line).getBytes();
                        InetAddress address = InetAddress.getByName("localhost");

                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
                        udpSocket.send(sendPacket);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            else {

                //normal tcp messages

                String message = username + ": " + text;
                PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);
                out.println(message);

                printToChat(message);
            }
        }
    }

    private void printToChat(String message) {
        // print to chat
        chatArea.append("\n" + message);

        if(scrollAutomatically) {

            //set scroll to all the way down
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        }
    }


    private void cleanUp() {


        try {
            tcpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //wait for socket to close

        while(!tcpSocket.isClosed()) {}

        System.out.println("Closing now");
    }

    private void listenForTcpMessages() throws IOException{
        while(true) {
            BufferedReader in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            String message = in.readLine();

            if(username != null) {
                printToChat(message);

            }
        }
    }

    private void listenForUdpMessages() {
        try {

            InetAddress address = InetAddress.getByName("localhost");

            //sending hello packet so clients port is added to the server
            byte[] sendBuffer = "hello".getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
            udpSocket.send(sendPacket);

            while(true) {

                //receive and print messages

                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket);

                String msg = new String(receivePacket.getData());

                if(username != null) {
                    printToChat(msg);
                }


            }

        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if (udpSocket != null) {
                udpSocket.close();
            }
        }
    }

    private void listenForUdpMessagesMulticast() {
        try {

            while(true) {

                //receive and print messages

                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                multicastSocket.receive(receivePacket);

                String msg = new String(receivePacket.getData());

                if(username != null) {
                    printToChat(msg);
                }

            }

        }
        catch(IOException e){
            e.printStackTrace();
        }
        finally {
            if (multicastSocket != null) {
                multicastSocket.close();
            }
        }
    }


    public void setAsciiArt(ArrayList<String> asciiArt) {
        this.asciiArt = asciiArt;
    }



    public static void main(String[] args) throws IOException {
        //load asciiart from file
        BufferedReader br = new BufferedReader(new FileReader("asciiart.txt"));

        ArrayList<String> asciiArt = new ArrayList<>();

        try {
            String line = br.readLine();


            while (line != null) {
                asciiArt.add(line);

                line = br.readLine();
            }
        } finally {
            br.close();
        }


        ChatClient chatClient = new ChatClient();
        chatClient.setAsciiArt(asciiArt);


        //gui thread
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chatClient.createAndShowGUI();
            }

        });


        //tcp thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chatClient.listenForTcpMessages();
                }
                catch (SocketException e) {
                    System.out.println("Server connection died, exiting");

                    System.exit(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    chatClient.cleanUp();
                }

            }
        }).start();

        //udp thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                chatClient.listenForUdpMessages();
            }
        }).start();

        //udp multicast thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                chatClient.listenForUdpMessagesMulticast();
            }
        }).start();


    }

}
