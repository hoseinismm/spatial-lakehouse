import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server is listening on port 8080");

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("New client connected");

                    // دریافت پیام از فلاتر
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    byte[] buffer = new byte[256];
                    int bytesRead = input.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytesRead, "UTF-8");
                    System.out.println("Received from Flutter: " + receivedMessage);

                    // ارسال پیام به فلاتر با استفاده از DataOutputStream و متد write
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    String message = "Hello from Java!";
                    byte[] messageBytes = message.getBytes("UTF-8");
                    output.write(messageBytes);
                    output.flush();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
