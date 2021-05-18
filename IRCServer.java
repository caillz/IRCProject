import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

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
                /* Displaying that new client is connected
                 * to server
                 */
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

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                int user = 0;
                //boolean usernameTaken = false;

                String line;
                while ((line = in.readLine()) != null) {
                    if (user == 0) {
                        /* message to server */
                        System.out.printf(" Sent from the client: %s\n",
                                line);

                        /* Check the user's chosen username to make sure
                         * it is not already taken by another user
                         */
                        while (usernameTaken(line)) {
                            out.println("Username is taken. Try again: ");
                            line = in.readLine();
                        }

                        userNames.put(clientSocket, line);
                        userList.add(line);
                        connections.add(clientSocket);
                        System.out.println(userNames);
                        /* notify client */
                        out.println("User name saved.");
                        user++;
                        continue;


                    }
                    if (user == 1) {
                        /* message to server */
                        System.out.printf(" Sent from %s: %s\n", userNames.get(clientSocket),
                                line);
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
                    /* notify user */
                    out.println("Invalid command");
                }
            } else if (message.contains("/join")) {
                String[] checkMsg = message.split("\\s+");
                if (checkMsg.length == 2) {
                    String[] arr = message.split(" ", 2);
                    channel = arr[1];
                    joinChannel(socket, channel);
                } else {
                    /* notify user */
                    out.println("Number of arguments is wrong. Type '/help' for usage");
                }
            } else if (message.contains("/leave")) {
                String[] checkMsg = message.split("\\s+");
                if (checkMsg.length == 2) {
                    String[] arr = message.split(" ", 2);
                    channel = arr[1];
                    leaveChannel(socket, channel);
                } else {
                    /* notify user */
                    out.println("Number of arguments is wrong. Type '/help' for usage");
                }
            } else if (message.contains("/listChannels")) {
                String[] checkMsg = message.split("\\s+");
                if (checkMsg.length == 1) {
                    listAllChannels();
                } else {
                    /* notify user */
                    out.println("Too many arguments. Type '/help' for usage");
                }
            } else if (message.contains("/removeChannel")) {
                String[] checkMsg = message.split("\\s+");
                if (checkMsg.length == 2) {
                    String[] arr = message.split(" ", 2);
                    channel = arr[1];
                    removeChannel(channel);
                } else {
                    /* notify user */
                    out.println("Number of arguments is wrong. Type '/help' for usage");
                }
            } else if (message.contains("/help")) {
                out.println("List of valid commands and their usage:\n"
                        + "/join <#channel-name>\n"
                        + "/leave <#channel-name>\n"
                        + "/msg <message-string>\n"
                        + "/privateMsg <username> <message-string>\n"
                        + "/listChannels\n"
                        + "/removeChannel <#channel-name>\n"
                        + "/logout\n");
            } else if (message.contains("/logout")) {
                out.println();
            } else {
                /* notify user */
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
                /* don't store client who is sending message */
                if (connection != socket) {
                    /* find sockets with non empty current channel */
                    if (currentChannel.get(connection) != null) {
                        validUsers.add(connection);
                    }
                }
            }
            /* go through sockets in valid users */
            for (Socket s : validUsers) {
                /* only send to sockets who are looking at the same current channel */
                if (currentChannel.get(s).equals(currentChannel.get(socket))) {
                    /* attempt to send message */
                    PrintWriter cout = new PrintWriter(
                            s.getOutputStream(), true);
                    cout.println(user+": "+message);
                }
            }
            if (validUsers.isEmpty()) {
                out.println("No other users in channel");
            }
        }
        /* Method that handles private message commands */
        void privateMsg(Socket socket, String userToReceiveMsg, String message) throws IOException {
            String sender = userNames.get(socket);
            /* user tried to send a private message to themselves */
            if (userToReceiveMsg.equals(sender)) {
                /* notify user */
                out.println("Cannot send a private message to yourself");
                return;
            }
            /* check that the username is in the user list */
            if (userList.contains(userToReceiveMsg)) {
                /* go through userNames to find correct socket */
                for (Map.Entry<Socket, String> entry : userNames.entrySet()) {
                    if (entry.getValue().equals(userToReceiveMsg)) {
                        /* attempt to send private message */
                        PrintWriter cout = new PrintWriter(
                                entry.getKey().getOutputStream(), true);
                        cout.println("Private message from " + sender+": "+message);
                    }
                }
            } else {
                /* username wasn't in user list */
                out.println("User doesn't exist");
            }
        }
        /* Method that allows users to join a channel */
        void joinChannel(Socket socket, String channel) throws IOException {
            int numberOfChannels = channels.size();
            boolean channelFound = false;
            for (String s : channels) {
                if (s.equals(channel)) {
                    /* channel exists */
                    if (numberOfChannels < 10) {
                        currentChannel.put(socket, channel);
                        /* notify user */
                        out.println("Joined " + channel);
                        channelFound = true;
                    } else {
                        /* notify user */
                        out.println("Channel limit reached");
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
                        /* notify user */
                        out.println("Joined " + channel);
                    } else {
                        /* notify user */
                        out.println("Invalid channel name");
                    }
                } else {
                    /* notify user */
                    out.println("Channel limit reached");
                }
            }
        }
        /* Allows a user to leave a channel */
        void leaveChannel(Socket socket, String channel) {
            String user = userNames.get(socket);
            if (channels.contains(channel)) {
                if (currentChannel.get(socket).equals(channel)) {
                    currentChannel.remove(socket);
                    /* notify user */
                    out.println("You have left the channel");
                } else {
                    /* notify user */
                    out.println("You cannot leave a channel you are not in");
                }
            } else {
                /* notify user */
                out.println("Channel does not exist");
            }
        }
        /* List all channels */
        void listAllChannels() {
            out.println("Current channels: ");
            out.println(String.join("\n", channels));
        }

        /* Allows user to delete a channel */
        void removeChannel (String channel) {
            if (channel.contains("#")) {
                channels.remove(channel);
                out.println("Channel removed");
            } else {
                out.println("Channel name must begin with '#'");
            }
        }

        /* Checks the userList for potential usernames to make sure the
         * username doesn't already exist
         */
        boolean usernameTaken(String user) {
            for (String s : userList) {
                if (s.equals(user)){
                    return true;
                }
            }
            return false;
        }
    }
}
