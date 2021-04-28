import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Scanner;

public class IRCClient {

    public static void main(String[] args) {
        String name = "empty";
        String reply = "empty";
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your screen name: ");
        reply = sc.nextLine();
        name = reply;

        try (Socket socket = new Socket("localhost", 5000)) {
            PrintWriter cout = new PrintWriter(socket.getOutputStream(), true);

            try {
                ThreadClient threadClient = new ThreadClient(socket);
                new Thread(threadClient).start(); // start thread to receive message
            } catch (Exception e) {

            }

            cout.println(name /*+ ": has joined chat-room."*/);
            do {
                //String message = (name + " : ");
                reply = sc.nextLine();
                if (reply.equals("logout")) {
                    cout.println("logout");
                    break;
                }
                cout.println(/*message + */reply);
            } while (!reply.equals("logout"));
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    /**
     * Thread for Client
     */
    static class ThreadClient implements Runnable {

        private Socket socket;
        private BufferedReader cin;

        public ThreadClient(Socket socket) throws IOException {
            this.socket = socket;
            this.cin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = cin.readLine();
                    System.out.println(message);
                }
            } catch (SocketException e) {
                System.out.println("You left the chat-room");
            } catch (IOException exception) {
                System.out.println(exception);
            } finally {
                try {
                    cin.close();
                } catch (Exception exception) {
                    System.out.println(exception);
                }
            }
        }
    }
}
