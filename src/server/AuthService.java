package server;

public interface AuthService {

    /**
     * @return nickname если пользователь есть
     * @return null если пользователя нет
     */
    String getNickByLoginAndPassword(String login, String password);
}
