import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Signature;
import java.security.SignedObject;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

public class Server {

    public static void main(String [] args) throws Exception{
        System.out.println();
        //port number to run server
        int port = GetPort(args);
        if(port == -1){
            System.out.println("Enter a valid port number");
            System.exit(-1);
        }

        // Stores all Post objects in a list
        List<Post> postList = new ArrayList<>();

        //Create a new socket for connection
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Waiting incoming connection...");

        //An infinite loop so that the server is always awake for connections
        //noinspection InfiniteLoopStatement
        while(true) {
            Socket socket = serverSocket.accept();
            System.out.println("New Client Connected...");
            System.out.println();

            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            try {
                SendAllPosts(postList, dataOutputStream, objectOutputStream);

                boolean postStatus = dataInputStream.readBoolean();
                if(postStatus){
                    Post makePost = GetPostFromClient(dataInputStream, objectInputStream);

                    if(makePost != null){
                        System.out.println("\nReceived post:");
                        System.out.println(makePost.getPostAsString());
                        postList.add(makePost);
                        System.out.println("Post added\n");
                    }
                }
                dataOutputStream.close();
                dataInputStream.close();
                socket.close();

            } catch(Exception e) {
                socket.close();
                System.err.println("Client closed its connection.");
            }
            System.out.println();
        }

    }

    /**
     * The method getPort reads the args and validates and returns a positive integer
     * @param args the command line arguments to read port
     * @return port value
     */
    private static int GetPort(String[] args) {
        if (args.length != 1) {
            System.err.println("Port is required or check no of arguments");
            System.exit(-1);
        }
        else{
            try{
                return Integer.parseInt(args[0]);
            }
            catch(Exception e){
                System.out.println("Enter a valid port number");
                System.exit(-1);
            }
        }
        return -1;
    }

    /**
     * The sendAllPosts method send all the posts available to the user
     * @param postList            List that has all the Post objects
     * @param dataOutputStream    To be able to write data to the client
     * @param objectOutputStream  To be able to write objects to the client
     */
    private static void SendAllPosts(List<Post> postList, DataOutputStream dataOutputStream, ObjectOutputStream objectOutputStream){
        try {
            dataOutputStream.writeInt(postList.size());
            for(Post post : postList){
                objectOutputStream.writeObject(post);
            }
        } catch (IOException e) {
            System.out.println("Failed to send post to client....");
        }
    }

    /**
     * The getPostFromClient method reads the post information from the client,
     * Verifies it signature and creates a post object.
     * @param dataInputStream To be able to read the input from the client
     * @param objectInputStream To be able to read the input from the client
     * @return Post received from client
     */
    private static Post GetPostFromClient(DataInputStream dataInputStream, ObjectInputStream objectInputStream){
        try {
            String userId = dataInputStream.readUTF();
            SignedObject signedObject = (SignedObject) objectInputStream.readObject();
            System.out.println("Post received");
            Post verifiedPost = verifySignature(userId, signedObject);

            if(verifiedPost != null){
                System.out.println("Signature Verified....");
                return verifiedPost;
            }
        } catch (Exception e) {
            System.out.println("Failed to get post from client....");
        }
        return null;
    }

    /**
     * The method verifySignature verifies whether the post is valid or not and returns respective boolean value
     * @param userId        The userId of the client
     * @param signedPost    The signed post object
     * @return Post
     */
    private static Post verifySignature(String userId, SignedObject signedPost){
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            RSAPublicKey publicKey = GetPublicKey(userId);
            boolean isValidSign = signedPost.verify(publicKey, signature);
            if(isValidSign){
                return (Post) signedPost.getObject();
            }
            else{
                System.out.println("Wrong key or signature....");
            }
        }
        catch (Exception e) {
            System.out.println("Failed to verify signature....");
        }
        return null;
    }

    /**
     * The method getPublicKey gets the userId then reads the respective file to get the object from it and
     * reads the object to return its respective RSAPublicKey
     * @param userId          The userId of the required clients public key
     * @return RSAPublicKey
     */
    private static RSAPublicKey GetPublicKey(String userId) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(userId + ".pub");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        RSAPublicKey publicKey = (RSAPublicKey) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
        return publicKey;
    }
}
