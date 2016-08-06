import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by vlad on 24/04/16.
 */
public class Protocol implements Runnable{

    private Socket client;
    private InputStream in;
    private DataOutputStream out;

    private HttpHandler handler;



    public Protocol(Socket client, String baseDir){

        try{
            this.client = client;
            this.in = client.getInputStream();
            this.out = new DataOutputStream(client.getOutputStream());
            this.handler = new HttpHandler(baseDir);
        } catch (IOException e) {
            try {
                client.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }


    private void handleClient() throws IOException, NoSuchAlgorithmException {

        Map<String, String> header = handler.getHeaderHTTP(in);

        String method = header.get("method");

        if(method != null) {

            if (method.equals("GET")) {
                handler.handleGET(header.get("query"), header.get("authType"), header.get("authHash"), out);
            } else if (method.equals("PUT")) {
                System.out.println("calling PUT");
                handler.handlePUT(header.get("query"), header.get("content-length"), header.get("authType"), header.get("authHash"), in, out);
            } else if (method.equals("DELETE")) {
                handler.handleDELETE(header.get("query"), header.get("authType"), header.get("authHash"), out);
            } else {
                handler.handleUNKNOWN(out);
            }
        }else {
            handler.handleUNKNOWN(out);
        }

        client.close();
    }


    public void run() {
        try {
            handleClient();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
