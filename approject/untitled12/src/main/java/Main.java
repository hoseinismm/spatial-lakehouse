import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Gson gson = new Gson();

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                     DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

                    String message = "nothing";
                    byte[] buffer = new byte[256];
                    int bytesRead = input.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);

                    JsonObject jsonObject = JsonParser.parseString(receivedMessage).getAsJsonObject();
                    String action = jsonObject.has("action") ? jsonObject.get("action").getAsString() : "";
                    System.out.println("Action: " + action);

                    switch (action) {
                        case "getallitems":
                            System.out.println("getallitems");
                            ArrayList<Item> items = CommandManager.getAllItems();
                            String i = jsonObject.get("i").getAsString();
                            message = gson.toJson(items.get(Integer.parseInt(i)));
                            break;
                        case "checkuser":
                            String username = jsonObject.get("username").getAsString();
                            String password = jsonObject.get("password").getAsString();
                            message = String.valueOf(CommandManager.checkUser(username, password));
                            break;
                        case "returnuserbyid":
                            System.out.println(receivedMessage);
                            int userId = jsonObject.get("userid").getAsInt();
                            User user = CommandManager.getUser(userId);
                            message = gson.toJson(user);
                            break;
                        case "getviews":
                            int id = jsonObject.get("id").getAsInt();
                            List<Viewpoint> views = CommandManager.getViews(id);
                            message = gson.toJson(views);
                            break;
                        case "addview":
                            id = jsonObject.get("id").getAsInt();
                            String reviewText = jsonObject.get("review_text").getAsString();
                            int userid = jsonObject.get("userid").getAsInt();
                            message = CommandManager.comment(userid, id, reviewText) ? "true" : "false";
                            break;
                        case "addtoshoppingcart":
                            id = jsonObject.get("id").getAsInt();
                            userid = jsonObject.get("userid").getAsInt();
                            message = CommandManager.addtoshoppingcart(id, userid) ? "true" : "false";
                            break;
                        case "decreasefromshoppingcart":
                            id = jsonObject.get("id").getAsInt();
                            userid = jsonObject.get("userid").getAsInt();
                            message = CommandManager.decreasefromshoppingcart(id, userid) ? "true" : "false";
                            break;
                        default:
                            message = "Invalid action";
                    }

                    System.out.println("Received from Flutter: " + receivedMessage);
                    byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                    output.write(messageBytes);
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
