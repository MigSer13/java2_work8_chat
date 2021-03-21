package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickName;
    private String login;

    public ClientHandler(Server server, Socket socket) {

        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    // цикл аутентифиукаии
                    socket.setSoTimeout(10000); //10 секунд
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/auth")) {
                            String[] token = str.split("\\s");
                            String login = token[1];
                            String password = token[2];
                            if (token.length <3){
                                continue;
                            }
//                            String newNick = server.getAuthService().getNickByLoginAndPassword(login, password);
                            String answer = server.findUserInDatabase(login, password);
                            String newNick = null;
                            if(answer.startsWith("/authok")){
                               newNick = answer.split("\\s")[1];
                               this.login = login;
                            } else if(answer.startsWith("/userNotFound")){
                                sendMsg(answer.split("\\s")[1]);
                            }
                            else {
                                sendMsg(answer);
                            }
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickName = newNick;
                                    sendMsg("/authok " + nickName);
                                    server.subscribe(this);
                                    System.out.println("Клиент " + nickName + " подключился");
                                    break;
                                } else {
                                    sendMsg("С данной учетной записью уже зашли");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }

                        if(str.startsWith("/reg")){
                            String[] token = str.split("\\s");
                            if(token.length < 4){
                                continue;
                            }
                            String login = token[1];
                            String password = token[2];
                            String nickname = token[3];
                            //boolean isRegistration = server.getAuthService().registration(login, password, nickname);
                            boolean isRegistration = server.iSregistrationUser(login, password, nickname);
                            if (isRegistration){
                                sendMsg("/regok");
                            }else {
                                sendMsg("/regno");
                            }
                        }

                    }

                    socket.setSoTimeout(0);
                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            System.out.println(str);
                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }
                            if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }
                        } else if(str.startsWith("/rename")){
                            String[] token = str.split("\\s");
                            if(token.length < 2){
                                continue;
                            }
                            String newNick = token[1];
                            if (server.renameNick(this.login, newNick)){
                                sendMsg("ник поменян на " + newNick);
                                this.nickName = newNick;
                            }
                        }else {
                            server.broadcastMsg(this, str);
                        }
                    }
                }catch (SocketTimeoutException e) {
                    try {
                        System.out.println("Соединение закрыто, так как не было авторизации");
                        socket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    System.out.println("Клиент отключился");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickName() {
        return nickName;
    }

    public String getLogin() {
        return login;
    }
}
