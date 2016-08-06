import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Created by vlad on 26/04/16.
 */
public class ResponseBuilder {

    private static final String HTML_HEAD = "<html>" +
             "<h1>Java A4B77ASS Server<h1>" +
            "<body><hr>";

    private static final String HTML_END = "</body>" + "</html>";

    private String baseDir;
    private CacheMap<String> dirCache;


    public ResponseBuilder(String baseDir){
        this.baseDir = baseDir;
        this.dirCache = CacheMap.getDirCache();
    }


    /**
     * Makes a HTTP response header depending on supplied status code
     * @param status
     * @param content
     * @param contentLength
     * @param connection
     * @return  header as string
     */
    public String makeHTTP(int status, String content, Long contentLength, String connection){
        String httpResponse = "HTTP/1.1 ";
        switch (status){
            case 200: httpResponse += "200 OK \r\n";
                break;
            case 401: httpResponse += "401 Unauthorized \r\n";
                break;
            case 403: httpResponse += "403 Forbidden \r\n";
                break;
            case 501: httpResponse += "501 Not Implemented \r\n";
            default:
                httpResponse += "404 Not Found \r\n";
        }

        httpResponse += "Server: A4B77ASS Server \r\n";
        httpResponse += "Content-Length: " + contentLength + " \r\n";
        httpResponse += "Content-Type: " + content + " \r\n";
        httpResponse += "Connection: " + connection + "\r\n";


        if(status == 401){
            httpResponse += "WWW-Authenticate: Basic realm=\"basic_realm\"\r\n";
        }

        httpResponse += "\r\n";
        return httpResponse;
    }


    /**
     * Generates html table with content from of supplied dir
     * @param path supplied dir
     * @return generated content as string
     */
    private String generateDirContent(String path) throws IOException {
        String ret = "" + HTML_HEAD;

        File folder = new File(path);
        File[] files = folder.listFiles();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        ret += "<table style=\"width:50%\">";

        ret += "<tr align=\"left\">\n" +
                "    <th> </th>\n" +
                "    <th>Name</th>\n" +
                "    <th>Size</th>\n" +
                "    <th>Last Modified </th>\n" +
                "  </tr>";


        if(files != null) {

            for (File file : files) {

                if (!file.isHidden()) {

                    String tableRow = "<tr align=\"left\">\n";


                    if (file.isDirectory()) {
                        tableRow += "<td><img src=\"http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Folder-icon.png\" height=\"42\" width=\"42\"></td>\n";
                    } else if (file.isFile()) {
                        tableRow += "<td><img src=\"http://neowin.s3.amazonaws.com/forum/uploads/monthly_04_2013/post-360412-0-09676400-1365986245.png\" height=\"42\" width=\"42\"></td>\n";
                    }


                    tableRow += "<td>" + "<a href=" + file.getAbsolutePath().split(baseDir)[1] + ">" + file.getName() + "</a>" + "</td>";   //get absolute path without user/vlad bla bla


                    if (file.isDirectory()) {
                        tableRow += "<td> DIR </td>";
                    } else if (file.isFile()) {
                        tableRow += "<td>" + file.length() / 1000 + "kB</td>";
                    }

                    tableRow += "<td>" + sdf.format(file.lastModified()) + "</td>";

                    tableRow += "</tr>";
                    ret += tableRow;
                }
            }
        }

        ret += HTML_END;

        return ret;
    }


    /**
     * Manages the dirCache and returns desired content
     * @param path
     * @return content string
     */
    public String generateDir(String path) throws IOException {
        String ret = dirCache.get(path);

        if(ret == null){
            ret = generateDirContent(path);
            dirCache.put(path, ret);
        }
        return ret;
    }


    /**
     * Makes simple response page used on sending error to browser
     * @param text text to display in browser
     * @return content string
     */
    public String makeResponse(String text){
        return HTML_HEAD + "<b>" + text + "</b>" + HTML_END;
    }


    /**
     * Cleans managed dirCache after each successful PUT and DELETE request
     */
    public void updateBuilder(){
        dirCache.clean();
    }





}
