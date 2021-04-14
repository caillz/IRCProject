import java.io.PrintWriter;
import java.net.Socket;
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

            cout.println(name + ": has joined chat-room.");
            do {
                String message = (name + " : ");
                reply = sc.nextLine();
                if (reply.equals("logout")) {
                    cout.println("logout");
                    break;
                }
                cout.println(message + reply);
            } while (!reply.equals("logout"));
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }
}
