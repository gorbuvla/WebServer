import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

import static org.junit.Assert.assertTrue;

/**
 * Created by vlad on 22/05/16.
 */
public class TestHttpHandler {


    Server server;
    String base;

    @Before
    public void prepareTest() throws IOException {
        server = new Server(8888);
        base = System.getProperty("user.dir");
    }



    @Test
    public void testHandleGETNotSafeNotAuth() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("GET / HTTP/1.1\r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();

        assertTrue(line.equals("HTTP/1.1 200 OK "));
        socket.close();
    }


    @Test
    public void testHandleGETNotFound() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("GET /unknown_dir HTTP/1.1\r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();

        assertTrue(line.equals("HTTP/1.1 404 Not Found "));
        socket.close();
    }

    @Test
    public void testHandleGETSafeNotAuth() throws IOException {
        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("GET /safe_dir HTTP/1.1\r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();

        assertTrue(line.equals("HTTP/1.1 401 Unauthorized "));
        socket.close();

    }


    @Test
    public void testHandleGETSafeAuth() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("GET /safe_dir HTTP/1.1\r\n");
        out.writeBytes("Authorization: Basic d2VidXNlcjE6cGFzc3dvcmQx \r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();

        assertTrue(line.equals("HTTP/1.1 200 OK "));
        socket.close();
    }


    @Test
    public void testHandleGETHidden() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("GET /safe_dir/.htaccess HTTP/1.1\r\n");
        out.writeBytes("Authorization: Basic d2VidXNlcjE6cGFzc3dvcmQx \r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();

        assertTrue(line.equals("HTTP/1.1 404 Not Found "));
        socket.close();

    }

    @Test
    public void testHandlePUTNoAuth() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("PUT /dir1/file.txt HTTP/1.1\r\n");
        out.writeBytes("Content-Type: text/html \r\n");
        out.writeBytes("Content-Length: 9 \r\n");
        out.writeBytes("\r\n");

        out.writeBytes("some text");

        File file = new File(base + "/server/dir1/file.txt");
        assertTrue(file.exists());
        file.getAbsoluteFile().delete();
        socket.close();
    }


    @Test
    public void testHandlePUTHidden() throws IOException {
        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("PUT /dir1/.htaccess HTTP/1.1\r\n");
        out.writeBytes("Content-Type: text/html \r\n");
        out.writeBytes("Content-Length: 0 \r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();
        System.out.println("PUT LINE: " + line);
        assertTrue(line.equals("HTTP/1.1 403 Forbidden "));
        socket.close();
    }


    @Test
    public void testHandlePUTAuthNoPass() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("PUT /safe_dir/file.txt HTTP/1.1\r\n");
        out.writeBytes("Content-Type: text/html \r\n");
        out.writeBytes("Content-Length: 0 \r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();
        System.out.println("PUT LINE: " + line);
        assertTrue(line.equals("HTTP/1.1 401 Unauthorized "));
        socket.close();

    }


    @Test
    public void testHandlePUTAuthWithPass() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("PUT /safe_dir/file.txt HTTP/1.1\r\n");
        out.writeBytes("Content-Type: text/html \r\n");
        out.writeBytes("Content-Length: 9 \r\n");
        out.writeBytes("Authorization: Basic d2VidXNlcjE6cGFzc3dvcmQx \r\n");

        out.writeBytes("\r\n");

        out.writeBytes("some text");

        File file = new File(base + "/server/safe_dir/file.txt");
        assertTrue(file.exists());
        assertTrue(file.getAbsoluteFile().delete());
        socket.close();
    }


    @Test
    public void testHandleDELETENotFound() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("DELETE /dir1/.htaccess HTTP/1.1\r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();

        assertTrue(line.equals("HTTP/1.1 404 Not Found "));
        socket.close();
    }


    @Test
    public void testHandleDELETEHidden() throws IOException {

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("DELETE /.DS_Store HTTP/1.1\r\n");
        out.writeBytes("\r\n");

        String line = in.readLine();

        assertTrue(line.equals("HTTP/1.1 403 Forbidden "));
        socket.close();


    }

    @Test
    public void testHandleDELETEWithAuth() throws IOException {
        File tmp = new File(base + "/server/safe_dir/test.txt");
        tmp.createNewFile();

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("DELETE /safe_dir/test.txt HTTP/1.1\r\n");
        out.writeBytes("Authorization: Basic d2VidXNlcjE6cGFzc3dvcmQx \r\n");
        out.writeBytes("\r\n");


        assertTrue(!tmp.getAbsoluteFile().exists());
        socket.close();

    }

    @Test
    public void testHandleDELETENoAuth() throws IOException {

        File tmp = new File(base + "/server/dir1/test.txt");
        tmp.createNewFile();

        Socket socket = new Socket("127.0.0.1", 8888);
        server.exec(server.acceptClient());

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeBytes("DELETE /dir1/test.txt HTTP/1.1\r\n");
        out.writeBytes("\r\n");

        assertTrue(!tmp.getAbsoluteFile().exists());
        socket.close();
    }

    @After
    public void finishTest() throws IOException {
        server.destroy();
        server.unbind();
    }


}
