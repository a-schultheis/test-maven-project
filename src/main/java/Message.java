import javax.management.InvalidAttributeValueException;

/**
 * This class represents a message, which can be send and received by {@link Process}es.
 */
public class Message {

    /**
     * This enumeration contains all allowed values for the network communication.
     */
    public enum MessageType {
        /**
         * This value represents the REQUEST message. It's send via a multicast.
         */
        REQUEST,

        /**
         * This value represents the ACKNOWLEDGE message. It's send via a unicast.
         */
        ACKNOWLEDGE,

        /**
         * This value represents the RELEASE message. It's send via a multicast.
         */
        RELEASE,

        /**
         * This value represents a command, which is send from one {@link Process} to another. It ensures, that the other Process starts it's run command.
         * The value doesn't represent a real command of the lamport mutual exclusion algorithm. It's only integrated for the simulation. It's send via a unicast.
         */
        RUN_COMMAND
    }

    /**
     * The id, which identifies the sender Process. The senderId can't be changed, if it was set once.
     */
    private final int senderId;

    /**
     * The id, which identifies the receiver Process. If it's set to -1, it represents, that all Processes should be received. This does only work, if a multicast (request or release) is send.
     * It's the only case where this id is allowed to be modified.
     */
    private int receiverId;

    /**
     * The type of the message represented by the enumeration {@link MessageType}. By this type, it's controlled, whether the message is a unicast or a multicast.
     * The messageType can't be changed, if it was set once.
     */
    private final MessageType messageType;

    /**
     * The {@link LamportClock}, which is unique for each {@link Process}. It can't be changed, if it was set once.
     */
    private final long lamportTime;

    /**
     * Constructor for a message.
     * @param senderId Id of the sender {@link Process}.
     * @param receiverId Id of the receiver Process. Can be -1, if the message is a multicast.
     * @param messageType The type of the message as {@link MessageType}.
     * @param lamportTime The time of the {@link LamportClock}, when the message was send.
     */
    public Message(int senderId, int receiverId, MessageType messageType, long lamportTime ) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.lamportTime = lamportTime;
    }

    /**
     * Getter method for the id of the sender {@link Process}.
     * @return Id of the sender Process.
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * Getter method for the id of the receiver {@link Process}.
     * @return Id of the receiver Process.
     */
    public int getReceiverId() {
        return receiverId;
    }

    /**
     * Setter method for the receiverId. It can only be changed, if the id was a negative value before. This is used to represent a multicast.
     * @param receiverId The id of the receiver {@link Process}.
     */
    public void setReceiverId(int receiverId) {
        if(this.receiverId >= 0) {
            new InvalidAttributeValueException("ReceiverId isn't allowed to be changed!").printStackTrace();
            return;
        }
        this.receiverId = receiverId;
    }

    /**
     * Getter method for the {@link MessageType} of the message.
     * @return The MessageType of the message.
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Getter method for the lamport timestamp of the message.
     * @return The lamport time, when the message was send, as long.
     */
    public long getLamportTime() {
        return lamportTime;
    }

    /**
     * A method to clone this message object.
     * @return A new message object, that contains the values of this object.
     */
    @Override
    public Message clone() {
        return new Message(senderId, receiverId, messageType, lamportTime);
    }

    /**
     * A class specific toString method, that prints the object.
     * @return A string that represents this object.
     */
    @Override
    public String toString() {
        return messageType.toString() + "," + senderId + "," + receiverId + "," + lamportTime;
    }
}