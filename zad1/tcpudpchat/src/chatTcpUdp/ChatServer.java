package chatTcpUdp;

import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChatServer {

    private ExecutorService executor = Executors.newFixedThreadPool(20);
    private Vector<Socket> clientSockets = new Vector<Socket>();
    private Vector<Integer> clientUdpPorts = new Vector<Integer>();
    private final int portNumber = 12345;
    private ServerSocket tcpServerSocket = null;
    private final String multicastAddr = "224.0.0.3";
    private final int multicastPort = 8888;


    private DatagramSocket udpServerSocket = null;

    public ChatServer() {

        //tcp server socket creation
        try {
            tcpServerSocket = new ServerSocket(portNumber);
        }catch (BindException e) {
            System.out.println("Port in use, cannot bind");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //udp server socket creation
        try {
            udpServerSocket = new DatagramSocket(portNumber);
        } catch (SocketException e) {
            System.out.println("Port in use, cannot bind");
            System.exit(1);
        }
    }





    public void listenForTcpConnections() throws IOException {
        System.out.println("Server is running...");
        while(true){

            //on every new client socket, execute a new runnable in the thread pool
            Socket clientSocket = tcpServerSocket.accept();


            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        manageTcpConnection(clientSocket);
                    } catch (IOException e) {
                        System.out.println("Client disconnected");
                    } finally {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });

        }
    }


    private void manageTcpConnection(Socket clientSocket) throws IOException {
        //add socket to collection
        clientSockets.add(clientSocket);
        System.out.println("Client connected");
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        while (!clientSocket.isClosed() && clientSocket.isConnected()) {
            //read message
            String msg = in.readLine();

            //send message to everyone but source
            for(Socket client : new Vector<Socket>(clientSockets)) {
                if(client == clientSocket) continue;
                if(client.isClosed() || !client.isConnected()) continue;
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                if(msg != null) out.println(msg);
            }
        }
    }



    public void manageUdpDatagrams() {
        try{
            while(true) {

                //receive message
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpServerSocket.receive(receivePacket);

                String msg = new String(receivePacket.getData());


                //on hello message add port to collection
                if(msg.substring(0,5).equals("hello")) {
                    clientUdpPorts.add(receivePacket.getPort());

                }
                else {
                    //on M command send to all udp ports but source
                    if(msg.charAt(0) == 'M') {
                        for (Integer port : clientUdpPorts) {
                            if(port == receivePacket.getPort()) continue;
                            byte[] sendBuffer = msg.substring(1, msg.length()).getBytes();
                            InetAddress address = InetAddress.getByName("localhost");
                            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
                            udpServerSocket.send(sendPacket);
                        }
                    }
                    //on N command send to multicast
                    else if(msg.charAt(0) == 'N') {
                        byte[] sendBuffer = msg.substring(1, msg.length()).getBytes();


                        InetAddress address = InetAddress.getByName(multicastAddr);
                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, multicastPort);

                        udpServerSocket.send(sendPacket);


                    }

                }


            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if (udpServerSocket != null) {
                udpServerSocket.close();
            }
        }
    }

        private void cleanUp() {
        try {
            tcpServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!tcpServerSocket.isClosed()) {}
        executor.shutdown();
        while (!executor.isTerminated()) {}

    }
        public static void main(String[] args) {

        ChatServer chatServer = new ChatServer();

        //clean up on exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                chatServer.cleanUp();
            }
        });

        //listen for new connection and start threads for them
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chatServer.listenForTcpConnections();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

        //listen for udp datagrams and act accordingly
        new Thread(new Runnable() {
            @Override
            public void run() {
                chatServer.manageUdpDatagrams();
            }
        }).start();


    }
}


