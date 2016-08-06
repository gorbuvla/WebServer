import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Created by vlad on 25/04/16.
 */
public class HttpHandler {

    private String baseDir;
    private ResponseBuilder rb;
    private FileUtil fileUtil;


    Logger logger = Logger.getLogger("http_handler");


    public HttpHandler(String serverDir){
        this.baseDir = serverDir;
        rb = new ResponseBuilder(serverDir);
        fileUtil = new FileUtil(serverDir);
    }


    /**
     * Handle HTTP GET request
     * @param query     GET query
     * @param authType  http header parameter
     * @param authHash  http header parameter
     * @param out       stream we are sending response to
     */
    public void handleGET(String query, String authType, String authHash, DataOutputStream out) throws IOException, NoSuchAlgorithmException {

        File requestedFile = new File(baseDir + query);

        if(requestedFile.exists()){

            if(requestedFile.isHidden()) {
                logger.warning("Attempt to get hidden file: " + requestedFile.getAbsolutePath());
                send404(out);
                return;
            }

            File htaccess = fileUtil.getHTACCESS(requestedFile);

            if(htaccess != null) {             //requested file/dir is protected

                logger.info("Protected file/dir requested: " + requestedFile.getAbsolutePath());

                if (authHash == null || !authType.equals("Basic")) {
                    send401(out);
                } else {
                    if (fileUtil.isPassValid(authHash, htaccess)) {
                        sendReqContent(requestedFile, out);
                    } else {
                        send403(out);
                    }
                }

            }else{
                sendReqContent(requestedFile, out);
            }

        }else{
            logger.info("Requested resource does not exist");
            send404(out);
        }
    }


    /**
     * Handle PUT request
     * @param query           PUT query
     * @param contentLength   http parameter
     * @param authType        http parameter
     * @param authHash        http parameter
     * @param in              stream we might receive data from (file after complete params and access validation)
     * @param out             stream we are sending response to
     */
    public void handlePUT(String query, String contentLength, String authType, String authHash, InputStream in, DataOutputStream out) throws IOException, NoSuchAlgorithmException {

        File fileToPUT = new File(baseDir + query);

        File htaccess = fileUtil.getHTACCESS(fileToPUT);

        System.out.println("IN PUT");
        if(validate(query)){     //if not hidden

            System.out.println("VALIDATED");
            if(htaccess != null){

                System.out.println("HTACCESS NOT NULL");
                if (authHash == null || !authType.equals("Basic")) {
                    System.out.println("NO AUTH");
                    send401(out);
                } else {

                    System.out.println("AUTH");
                    if (fileUtil.isPassValid(authHash, htaccess) && fileUtil.createFolders(query)) {
                        fileUtil.writeFile(fileToPUT, in, Long.parseLong(contentLength));
                        rb.updateBuilder();
                        send200(out);
                    }else {
                        send403(out);
                    }
                }

            }else {
                if(fileUtil.createFolders(query)){
                    fileUtil.writeFile(fileToPUT, in, Long.parseLong(contentLength));
                    rb.updateBuilder();
                }else {
                    send403(out);
                }
            }
        }else{
            System.out.println("NOT VALIDATED");
            logger.warning("Attempt to PUT hidden file: " + query);
            send403(out);
        }
    }


    /**
     * Handle DELETE request
     * @param query      DELETE query
     * @param authType   http parameter
     * @param authHash   http parameter
     * @param out        stream to send response to
     */
    public void handleDELETE(String query, String authType, String authHash, DataOutputStream out) throws IOException, NoSuchAlgorithmException {
        File requestedFile = new File(baseDir + query);

        System.out.println("DELETE");
        if(requestedFile.exists()){

            System.out.println("EXISTS");
            if(requestedFile.isHidden()) {
                System.out.println("IS HIDDEN");
                logger.info("Attempt to delete hidden file: " + query);
                send403(out);
                return;
            }

            File htaccess = fileUtil.getHTACCESS(requestedFile);

            if(htaccess != null) {
                System.out.println("PROTECTED");
                logger.info("Request delete protected resource: " + query);

                if (authHash == null) {
                    send401(out);
                } else {

                    if(fileUtil.isPassValid(authHash, htaccess)) {
                        deleteFile(requestedFile, out);
                        rb.updateBuilder();
                    }else {
                        send403(out);
                    }
                }

            }else{
                System.out.println("NOT PROTECTED");
                deleteFile(requestedFile, out);
                rb.updateBuilder();
            }

        }else{
            System.out.println("NOT EXIST");
            logger.info("Requested resource does not exist: " + query);
            send404(out);
        }
    }


    /**
     * Checks if query contains a path to hidden file
     * @param query
     * @return  boolean
     */
    private boolean validate(String query){
        int i = query.lastIndexOf("/");
        String tmp = query.substring(i + 1, query.length() - 1);
        if(!tmp.startsWith(".") || query.endsWith("/")){
            return true;
        }
        return false;
    }



    public void handleUNKNOWN(DataOutputStream out) {
        send501(out);
    }


    /**
     * Sends requested GET content either dir or a file
     * @param requestedFile
     * @param out
     * @throws IOException
     */
    private void sendReqContent(File requestedFile, DataOutputStream out) throws IOException {
        String httpResponse;

        if (requestedFile.isHidden()) {
            send404(out);
        } else if (requestedFile.isDirectory()) {
            String generatedHtml = rb.generateDir(requestedFile.getAbsolutePath());
            httpResponse = rb.makeHTTP(200, "text/html", (long) generatedHtml.getBytes().length, "close");
            out.writeBytes(httpResponse);
            out.writeBytes(generatedHtml);

        } else if (requestedFile.isFile()) {
            httpResponse = rb.makeHTTP(200, fileUtil.getContentType(requestedFile), requestedFile.length(), "close");
            out.writeBytes(httpResponse);
            fileUtil.sendFile(requestedFile, out);
        } else {
            send404(out);
        }
    }


    /**
     * Deletes a file and sends http response on success/failure
     * @param request
     * @param out
     * @throws IOException
     */
    private void deleteFile(File request, DataOutputStream out) throws IOException {
        String html;
        String http;
        if(request.delete()){
            html = rb.makeResponse("File " + request.getName() + " deleted successfully.");
            http = rb.makeHTTP(200, "text/html", (long)html.getBytes().length, "close");
        }else{
            html = rb.makeResponse("Error while deleting file: " + request.getName());
            http = rb.makeHTTP(500, "text/html", (long)html.getBytes().length, "close");
        }
        out.writeBytes(http);
        out.writeBytes(html);

    }


    private void send200(DataOutputStream out){
        String http = rb.makeHTTP(200, "text/html", 0l, "close");
        try{
            out.writeBytes(http);
        } catch (IOException e){
            logger.severe("Error sending 200");
        }
    }

    private void send401(DataOutputStream out){
        String html = rb.makeResponse("You need to authenticate");
        String http = rb.makeHTTP(401, "text/html", (long) html.getBytes().length, "close");
        try {
            out.writeBytes(http);
            out.writeBytes(html);
        } catch (IOException e) {
            logger.severe("Error sending 401");
        }
    }

    private void send403(DataOutputStream out){
        String html = rb.makeResponse("403 Forbidden \n Invalid username or password");
        String http = rb.makeHTTP(403, "text/html", (long) html.getBytes().length, "close");
        try {
            out.writeBytes(http);
            out.writeBytes(html);
        } catch (IOException e) {
            logger.severe("Error sending 403");
        }
    }


    private void send404(DataOutputStream out){
        String generatedHtml = rb.makeResponse("404 Not Found ........");
        String httpResponse = rb.makeHTTP(404, "text/html", (long) generatedHtml.getBytes().length, "close");
        try {
            out.writeBytes(httpResponse);
            out.writeBytes(generatedHtml);
        } catch (IOException e) {
            logger.severe("Error sending 404");
        }
    }


    private void send501(DataOutputStream out){
        String html = rb.makeResponse("501 Not Implemented");
        String http = rb.makeHTTP(501, "text/html", (long)html.getBytes().length, "close");
        try {
            out.writeBytes(http);
            out.writeBytes(html);
        } catch (IOException e) {
            logger.severe("Error sending 501");
        }
    }


    /**
     * Read and parse input data until \r\n \r\n is reached
     * @param bin stream to read header from
     * @return  parameter map
     */
    public Map<String, String> getHeaderHTTP(InputStream bin) throws IOException {

        Map<String, String> httpParams = new HashMap<>();

        String line = "";
        boolean methodLine = true;
        boolean stop = false;
        int r = 0;
        int n = 0;

        String method;
        String query;
        String contentLength;
        String authHash;
        String authType;

        while(!stop){

            int i = bin.read();
            char c = (char)i;

            if(c == '\r'){
                r+=1;
            }else if(c == '\n') {
                n += 1;
            }else if(i == -1){
                break;
            }else{
                r = 0;
                n = 0;
                line += c;
            }

            if(r == 1 && n == 1){

                System.out.println(line);
                logger.info(line);

                if(methodLine){
                    StringTokenizer st = new StringTokenizer(line);
                    try {
                        method = st.nextToken(" ");
                        query = st.nextToken(" ");
                        httpParams.put("method", method);
                        httpParams.put("query", query);
                    }catch (NoSuchElementException e){
                        return httpParams;
                    }

                    methodLine = false;

                }else if(line.startsWith("Content-Length: ")){
                    contentLength = line.substring(16, line.length() - 1);
                    httpParams.put("content-length", contentLength);
                }else if(line.startsWith("Authorization:")){
                    StringTokenizer st = new StringTokenizer(line);
                    st.nextToken(" ");
                    authType = st.nextToken(" ");
                    authHash = st.nextToken(" ");
                    httpParams.put("authType", authType);
                    httpParams.put("authHash", authHash);
                }

                line = "";
            }

            if(r == 2 && n ==2){
                stop = true;
                System.out.println("---------------------------END---------------------------");
            }
        }
        return httpParams;

    }




}
