import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.SignedObject;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;

public class Client {
    public static void main(String [] args) throws Exception {

        System.out.println();
        String host = "";        // hostname of server
        int port = 0;           // port of server
        String userId = "";     // client userId

        if(isArgsValid(args)){
            host = args[0];
            port = Integer.parseInt(args[1]);
            userId = args[2];
        }
        else{
            System.exit(-1);
        }


        Socket socket = new Socket(host, port);             //Connects to the server

        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        try {

            printAllPosts(userId, dataInputStream, objectInputStream);

            System.out.println("\nDo you want to add a post? [y/n]");
            String makePost = input.readLine();

            if(makePost.equals("y")){
                dataOutputStream.writeBoolean(true);
                System.out.println();
                System.out.println("Enter the recipient userid (type \"all\" for posting without encryption):");
                SendPost(userId, dataOutputStream, objectOutputStream, input);
            }
            else if(makePost.equals("n")){
                dataOutputStream.writeBoolean(false);
                socket.close();
            }
            socket.close();

        } catch (Exception e) {
            System.out.println();
            System.err.println("Cannot connect to server.");
        }
    }

    private static boolean isArgsValid(String[] args) {
        switch (args.length) {
            case 0:
                System.err.println("No class arguments are given");
                return false;
            case 1:
                System.err.println("Only one argument given, Required 3");
                return false;
            case 2:
                System.err.println("Only two arguments given, Required 3");
                return false;
            case 3:
                return true;
            default:
                System.err.println("There are more than 3 arguments");
                return false;
        }
    }

    /**
     * @param userId                    The user id of the client
     * @param dataInputStream           To be able to read data from the server
     * @param objectInputStream         To be able to read object from the server
     */
    private static void printAllPosts(String userId, DataInputStream dataInputStream, ObjectInputStream objectInputStream) throws Exception {
        int noOfPosts = dataInputStream.readInt();
        System.out.println("There are " + noOfPosts + " post(s).");

        int iterator=0;
        while (iterator<noOfPosts) {
            Post receivedPost = (Post) objectInputStream.readObject();
            String decryptedMessage = DecryptMessage(userId, receivedPost.getMessage());
            receivedPost.setMessage(decryptedMessage);

            System.out.println("\nThis is post number: " + iterator);
            System.out.println(receivedPost.getPostAsString());
            System.out.println();
            iterator++;
        }
    }

    /**This method get the post details from the terminal of client,
     * does the required encryption and sends the signed object to the server
     * @param userId                    The user id of the client
     * @param dataOutputStream          To be able to write data to the server
     * @param objectOutputStream        To be able to write object to the server
     * @param input                     To be able to read input from console
     */
    private static void SendPost(String userId, DataOutputStream dataOutputStream, ObjectOutputStream objectOutputStream, BufferedReader input) throws Exception {
        String recipientUserId = input.readLine();

        System.out.println();
        System.out.println("Enter your message:");
        String messageToSend = input.readLine();
        Date date = new Date();
        String timeStamp = date.toString();

        if(!recipientUserId.equals("all")){
            messageToSend = EncryptMessage(recipientUserId, messageToSend);
        }

        dataOutputStream.writeUTF(userId);
        Post postToSend = new Post(userId, messageToSend, timeStamp);
        SignedObject signedPost = SignPost(userId, postToSend);
        objectOutputStream.writeObject(signedPost);
    }

    /**
     * The method DecryptMessage tries to decrypt the message with the given user id, if it is successful
     * returns the decrypted message or else returns the original string
     * @param userId        The userId to be used to decrypt the message
     * @param message       The message that is to be decrypted
     * @return Decrypted Message
     */
    public static String DecryptMessage(String userId, String message) throws Exception{

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, GetPrivateKey(userId));
        String outMessage = "";
        try{
            byte[] decodedMessage = Base64.getDecoder().decode(message);
            byte[] byteMessage = cipher.doFinal(decodedMessage);//"UTF8"
            outMessage = new String(byteMessage, StandardCharsets.UTF_8);
        }
        catch (Exception e){
            outMessage = message;
        }
        return outMessage;
    }

    /**
     * The method EncryptMessage encrypts the message with the given user id
     * @param userId        The userId to be used to decrypt the message
     * @param message       The message that is to be decrypted
     * @return Encrypted Message
     */
    public static String EncryptMessage(String userId, String message) throws Exception{

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, GetPublicKey(userId));
        byte[] byteMessage = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));//"UTF8"
        String outMessage = Base64.getEncoder().encodeToString(byteMessage);
        return outMessage;
    }

    /**
     The method SignPost creates and returns the signed object
     * @param userId        The userId of the client
     * @param postToSign    The signed post object
     * @return Signed Object of Post
     */
    public static SignedObject SignPost(String userId, Post postToSign) throws Exception{
        Signature signature = Signature.getInstance("SHA1withRSA");
        RSAPrivateKey privateKey = GetPrivateKey(userId);
        SignedObject signedObject = new SignedObject(postToSign, privateKey, signature);
        return  signedObject;
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

    /**
     * The method getPrivateKey gets the userId then reads the respective file to get the object from it and
     * reads the object to return its respective RSAPrivateKey
     * @param userId          The userId of the required clients private key
     * @return RSAPrivateKey
     */
    public static RSAPrivateKey GetPrivateKey(String userId) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(userId + ".prv");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        RSAPrivateKey privateKey = (RSAPrivateKey) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
        return privateKey;
    }
}