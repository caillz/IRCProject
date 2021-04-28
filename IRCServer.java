import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IRCServer {
    static HashMap<Socket, String> currentChannel = new HashMap<>();
    static ArrayList<Socket> connections = new ArrayList<>();
    static ArrayList<String> userList = new ArrayList<>();
    static ArrayList<String> channels = new ArrayList<>();
    static HashMap<Socket, String> userNames = new HashMap<>();
    static final int port = 5000;
    static ServerSocket server;

    public static void main(String[] args) throws IOException {

        try {
            server = new ServerSocket(port);
            server.setReuseAddress(true);
            System.out.println("Server started...");
            while (true) {
                Socket socket = server.accept();
                // Displaying that new client is connected
                // to server
                System.out.println("New client connected "
                        + socket.getInetAddress()
                        .getHostAddress());

                ClientHandler ch = new ClientHandler(socket);
                new Thread(ch).start();
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
        finally {
            if (server != null) {
                try {
                    server.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * Thread for Client
     */
    private static class ClientHandler implements Runnable {

        private final Socket clientSocket;
        PrintWriter out = null;
        BufferedReader in = null;
        BufferedWriter cout = null;
        //public HashMap<Socket, String> userNames = IRCServer.userNames;
        //private BufferedReader cin;


        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
        }

        //@Override
        public void run() {
            /*PrintWriter out = null;
            BufferedReader in = null;*/
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                int user = 0;
                //String user;
                //user = in.readLine();
                //System.out.println("Screen name is:" + line);
                /*System.out.println("sock is:  "
                        + clientSocket.getInetAddress()
                        .getHostAddress());*/
                //userNames.put(clientSocket, user);
                //System.out.println(userNames);

               String line;
                while ((line = in.readLine()) != null) {
                    /*if (user != 1) {
                        // message to server
                        System.out.printf(" Sent from the client: %s\n",
                                line);
                        // message to client
                        out.println("User name saved.");
                    }*/
                    if (user == 0) {
                        // message to server
                        System.out.printf(" Sent from the client: %s\n",
                                line);
                        // message to client
                        out.println("User name saved.");
                        userNames.put(clientSocket, line);
                        System.out.println(userNames);
                        connections.add(clientSocket);
                        user++;
                        continue;
                    }
                    if (user == 1) {
                        // message to server
                        System.out.printf(" Sent from %s: %s\n", userNames.get(clientSocket),
                                line);
                        // message to client
                        //out.println(line);
                        parseData(clientSocket, line);

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                        clientSocket.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }


        /*try {
            while (true) {
                String message = cin.readLine();
                System.out.println(message);
            }
        } catch (SocketException e) {
            System.out.println("You left waiting room");
        } catch (IOException exception) {
            System.out.println(exception);
        } finally {
            try {
                cin.close();
            } catch (Exception exception) {
                System.out.println(exception);
            }
        }*/
        }

        /* Method to parse the data received and perform the appropriate action */
        void parseData(Socket socket, String message) throws IOException {
            String user;
            String msg;
            String channel;
            /* message command received */
            if (message.contains("/msg")) {
                /* user not in any channels */
                if ((currentChannel.get(socket)) == null) {
                    /* notify user */
                    out.println("Must join a channel to send a message");
                } else {
                    /* send the message */
                    if (message.length() > 4) {
                        /* name of user sending message */
                        user = userNames.get(socket);
                        /* message to send */
                        String[] arr = message.split(" ", 2);
                        msg = arr[1];
                        broadcastData(socket, user, msg);
                    } else {
                        /* no message provided */
                        out.println("Empty message, nothing sent");
                        //sendMessageToClient("Empty message, nothing sent");
                    }

                }
                /* received private message command */
            } else if (message.contains("/privatemsg")) {
                String [] checkMsg = message.split("\\s+");
                /* check for valid number of arguments */
                if (checkMsg.length >= 3) {
                    String[] arr = message.split(" ", 3);
                    user = arr[1];
                    msg = arr[2];
                    privateMsg(socket, user, msg);
                } else {
                    out.println("Invalid command");
                    //sendMessageToClient("Invalid command");
                }
            } else if (message.contains("/join")) {
                String [] checkMsg = message.split("\\s+");
                if (checkMsg.length == 2) {
                    String[] arr = message.split(" ", 2);
                    channel = arr[1];
                    joinChannel(socket, channel);
                }
            } else {
                out.println("Invalid command");
            }
        }

        /* Function that sends a message to clients connected to
         * the server according to the current channel that the
         * sender is in
         */
        void broadcastData(Socket socket, String user, String message) throws IOException {
            /* stores sockets that have a non-empty current field */
            ArrayList<Socket> validUsers = new ArrayList<>();
            /* go through sockets in connection list */
            for (Socket connection : connections) {
                //for (Socket s : connections) {
                /* don't store client who is sending message */
                if (connection != socket) {
                    /* find sockets with non empty current channel */
                    if (currentChannel.get(connection) != null) {
                        validUsers.add(connection);
                    }
                }
            }
            System.out.println("VALID USERS: " + validUsers);
            /* go through sockets in valid users */
            for (Socket s : validUsers) {
                /* only send to sockets who are looking at the same current channel */
                if (currentChannel.get(s).equals(currentChannel.get(socket))) {
                    /* attempt to send message */
                    PrintWriter cout = new PrintWriter(
                            s.getOutputStream(), true);
                    cout.println(user+": "+message);
                    //sendMessageToClient(user + " : " + message);
                }
            }
            if (validUsers.isEmpty()) {
                out.println("No other users in channel");
            }
        }
        /* Method that handles private message commands */
        void privateMsg(Socket socket, String user, String message) throws IOException {
            String sender = userNames.get(socket);
            /* user tried to send a private message to themselves */
            if (user.equals(sender)) {
                out.println("Cannot send a private message to yourself");
                //sendMessageToClient("Cannot send a private message to yourself");
                return;
            }
            /* check that the username is in the user list */
            if (userList.contains(user)) {
                /* go through userNames to find correct socket */
                for (Map.Entry<Socket, String> entry : userNames.entrySet()) {
                    if (entry.getValue().equals("user")) {
                        /* try sending the private message */
                        out.println("Private message from " + sender + ": " + message);
                        //sendMessageToClient("Private message from " + sender + ": " + message);
                    }
                }
            } else {
                /* username wasn't in user list */
                out.println("User doesn't exist");
                //sendMessageToClient("User doesn't exist");
            }
        }
        /* Method that allows users to join a channel */
        void joinChannel(Socket socket, String channel) throws IOException {
            String user = userNames.get(socket);
            int numberOfChannels = channels.size();
            System.out.println("NUMBER OF CHANNELS: " + channels.size());
            boolean channelFound = false;
            //if (channels.contains(channel)) {
            for (String s : channels) {
                if (s.equals(channel)) {
                    // channel exists
                    if (numberOfChannels < 10) {
                        //channels.add(channel);
                        currentChannel.put(socket, channel);
                        out.println("Joined " + channel);
                        //sendMessageToClient("Joined " + channel);
                        channelFound = true;
                        //broadcastData(socket, user, channel);
                    } else {
                        out.println("Channel limit reached");
                        //sendMessageToClient("Channel limit reached");
                        //out.flush();
                        //dataOutputStream.close();
                    }
                }
            }
            /* channel doesn't exists yet */
            if (!channelFound){
                if (numberOfChannels < 10) {
                    if (channel.contains("#")) {
                        channels.add(channel);
                        currentChannel.put(socket, channel);
                        System.out.println("channel added");
                        System.out.println("CHANNELS" + channels);
                        out.println("Joined " + channel);
                        //sendMessageToClient("Joined " + channel);
                        //broadcastData(socket, user, channel);
                    } else {
                        out.println("Invalid channel name");
                        //sendMessageToClient("Invalid channel name");
                    }
                } else {
                    out.println("Channel limit reached");
                    //sendMessageToClient("Channel limit reached");
                }
            }
        }
    }
}
