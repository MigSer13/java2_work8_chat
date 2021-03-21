package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class Server {
    SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss");
    List<ClientHandler> clients;
   // private AuthService authService;

    private static int PORT = 8189;
    ServerSocket server = null;
    Socket socket = null;
    private Connection connection;
    private Statement statement;

    public Server()
    {
        clients = new Vector<>();
        //authService = new SimpleAuthService();

        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен");
            connect();

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }

        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg)
    {
        String message = String.format("%s %s : %s", formater.format(new Date()), sender.getNickName(), msg);
        for (ClientHandler client : clients) {
            client.sendMsg(message);
        }
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg)
    {
        String message = String.format("%s [%s] private [%s] : %s", formater.format(new Date()), sender.getNickName(), receiver, msg);
        for (ClientHandler c : clients) {
            if (c.getNickName().equals(receiver)) {
                c.sendMsg(message);
                if (!c.equals(sender)) {    // отправитель != получателю
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg("not found user: " + receiver);
    }

    public void subscribe(ClientHandler clientHandler)
    {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler)
    {
        clients.remove(clientHandler);
        broadcastClientList();
    }

//    public AuthService getAuthService()
//    {
//        return authService;
//    }

    public boolean isLoginAuthenticated(String login)
    {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    private void broadcastClientList()
    {
        StringBuilder sb = new StringBuilder("/clientsList ");
        for (ClientHandler c : clients) {
            sb.append(c.getNickName()).append(" ");
        }
        String msg = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

    public String findUserInDatabase(String login, String password)
    {
        try {
            ResultSet rs = statement.executeQuery("SELECT (nick, password)  FROM users WHERE login = " + login);
            if (rs.next()) {
                String passwordInDatabase = rs.getString("password");
                if (passwordInDatabase.equals(password)) {
                    return "/authok " + rs.getString("nick");
                } else {
                    return "пароль введен не верно";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "/userNotFound пользователя с таким login не найдено";
    }

    public boolean iSregistrationUser(String login, String password, String nickname)
    {
        String answer = findUserInDatabase(login, password);
        if (answer.startsWith("/userNotFound")) {
            try {
                statement.addBatch(String.format("INSERT INTO users (login, password, nick) VALUES (%s, %s, %s)",
                        login, password, nickname));
                int[] i = statement.executeBatch();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void connect() throws SQLException, ClassNotFoundException
    {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:dataDB.db");
            statement = connection.createStatement();
    }

}
