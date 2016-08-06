import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Created by vlad on 22/05/16.
 */
public class FileUtil {

    private String baseDir;


    public FileUtil(String baseDir){
        this.baseDir = baseDir;
    }


    /**
     * Check if received base64 matches with stored hash
     * @param authHash  received hash
     * @param htaccess  corresponding .htaccess file
     * @return  boolean
     */
    protected boolean isPassValid(String authHash, File htaccess) throws IOException, NoSuchAlgorithmException {
        //decode received hash
        byte[] decoded = Base64.getDecoder().decode(authHash);
        String decString = new String(decoded);


        //get user & pass from authString
        StringTokenizer st = new StringTokenizer(decString);
        String user;
        String passUnhash;
        try {
            user = st.nextToken(":");
            passUnhash = st.nextToken();
        }catch (NoSuchElementException e){
            return false;
        }

        //hash decoded pass
        String pass = md5Hash(passUnhash);

        //read stored values
        FileReader fr = new FileReader(htaccess);
        BufferedReader br = new BufferedReader(fr);
        String userStored;
        String passStored;

        try{
            String line = br.readLine();
            StringTokenizer st1 = new StringTokenizer(line);
            userStored = st1.nextToken(":");
            passStored = st1.nextToken();
        }catch (NoSuchElementException | NullPointerException e1){
            return false;
        }
        br.close();
        fr.close();
        //compare
        return user.equals(userStored) && pass.equals(passStored);

    }


    /**
     * Examine baseDir structure and return .htaccess file if present
     * @param file  file we are searching .htaccess for
     * @return .htaccess file on success / null on fail
     */
    protected File getHTACCESS(File file){
        String[] tmp = file.getAbsolutePath().split(baseDir);
        String path;

        if(tmp.length == 0){  //when root "/" is requested
            path = "";
        }else {
            path = tmp[1];    // when something else
        }

        if(file.isFile()){                      //cut the filename
            int i = path.lastIndexOf("/");
            path = path.substring(0, i+1);
        }else if(file.isDirectory()){           //if request is directory, add "/" to look for .htaccess in this dir
            path += "/";
        }


        //cut and parse all dirs for .htaccess until find one or reach root dir
        while(path.length() >= 1){

            File lookFile = new File(baseDir + path);
            if(lookFile.exists()) {
                File[] files = lookFile.listFiles();

                if (files != null) {

                    for (File f : files) {
                        if (f.getName().equals(".htaccess")) {
                            return f;
                        }
                    }
                }
            }

            if(path.length() > 2) {
                path = path.substring(0, path.length() - 2);
                int i = path.lastIndexOf("/");
                path = path.substring(0, i + 1);
            }else {
                break;
            }
        }
        return null;
    }


    /**
     * Send file via stream
     * @param file file desired to send
     * @param out  socket out stream we are sending to
     */
    public void sendFile(File file, DataOutputStream out) throws IOException {
        FileInputStream fin = new FileInputStream(file);

        byte[] buff = new byte[1024];
        int bytesRead;

        while((bytesRead = fin.read(buff)) != -1){
            out.write(buff, 0, bytesRead);
        }
        fin.close();
    }


    /**
     * Write a file to server
     * @param file file we should write to
     * @param in   stream we are reading data from
     * @param contentLength  content-length of file retrieved from http header
     */
    public void writeFile(File file, InputStream in, long contentLength) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);

        int recvSize = 0;
        long recvAll = 0;

        byte[] buff = new byte[1024];


        while(recvAll != contentLength){

            if((contentLength - recvAll) / 1024 > 0){

                recvSize = in.read(buff, 0 , 1024);
                recvAll += recvSize;

            }else{

                recvSize = in.read(buff, 0, (int)(contentLength - recvAll));
                recvAll += recvSize;
            }
            fos.write(buff, 0, recvSize);
        }

        fos.close();
    }


    /**
     * Create folders from HTTP PUT request if not present
     * @param query  query to examine
     * @return true on success (dirs created/dirs already present) / false on fail
     */
    public boolean createFolders(String query){
        int i = query.lastIndexOf("/");

        //check only last dir from query and create it/them with mkdirs
        if(i > 0) {
            String path = query.substring(0, i);
            File dir = new File(baseDir + path);
            if (!dir.exists()){
                return dir.mkdirs();
            }else if(!dir.isDirectory()){
                return false;
            }
        }

        return true;
    }


    /**
     * Get content type of a file, we are sending to browser
     * @param file the file you want get type of
     * @return String with type name
     */
    public String getContentType(File file){
        return new MimetypesFileTypeMap().getContentType(file).split("/")[1];
    }


    /**
     * Hash supplied password with MD5
     * Example from: http://www.mkyong.com/java/java-md5-hashing-example/
     * @param password your unhashed password
     * @return hashed password
     */
    public String md5Hash(String password) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());

        byte bytes[] = md.digest();

        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; i++){
            String hex = Integer.toHexString(0xff & bytes[i]);
            if(hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

}
