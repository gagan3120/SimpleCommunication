import java.io.Serializable;

/**
 * The post object is used to store the post information
 */
class Post implements Serializable {
    private String userId;
    private String message;
    private String timeStamp;

    public Post(){
        super();
    }

    /**
     * Constructor to create Post object
     * @param userId        The userId of the client
     * @param message       The message to be sent to the respective client
     * @param timeStamp     The date and time when the message is sent
     */
    public Post(String userId, String message, String timeStamp){
        super();
        this.userId = userId;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getPostAsString(){
        String sender ="Sender: " + userId;
        String date = "Date: " + timeStamp;
        String messageOut = "Message: " + message;
        String postString = sender + System.getProperty("line.separator") + date + System.getProperty("line.separator") + messageOut;
        return ("--------- Post Start ---------\n" + postString + "\n---------  Post End  ---------");
    }

    /*
    //Debug purpose
    System.err.println("Error message: " + e.getMessage());
    System.err.println("Trace: ");
    e.printStackTrace();
    */
}
