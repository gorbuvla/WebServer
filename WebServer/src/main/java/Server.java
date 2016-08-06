import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by vlad on 24/04/16.
 */
public class Server {

    private static String baseDir = System.getProperty("user.dir") + "/server";
    private static int port = 8888;


    private ServerSocket server;
    private ExecutorService service;



    public Server(int port) throws IOException {
        server = new ServerSocket(port);
        service = Executors.newCachedThreadPool();
    }


    public static void main(String[] args) throws IOException {
        Server server = new Server(port);
        System.out.println();

        while (true) {
            Socket client = server.acceptClient();
            server.exec(client);
        }
    }

    public Socket acceptClient() throws IOException {
        return server.accept();
    }

    public void exec(Socket client) throws IOException {
        service.execute(new Protocol(client, baseDir));
    }

    public void destroy() {
        service.shutdown();
    }

    public void unbind() throws IOException {
        server.close();
    }

}
