import javax.management.InvalidAttributeValueException;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * This class represents a process, which is executed as a Thread.
 */
public class Process implements Runnable {

    /**
     * Id of the current Process as int. When set in the constructor, it can't be changed afterwards.
     */
    private final int processId;

    /**
     * Reference to the {@link LamportClock} of the current Process. When set in the constructor, it can't be changed afterwards.
     */
    private final LamportClock lamportClock;

    /**
     * Reference to the {@link Network} of the current Process. When set in the constructor, it can't be changed afterwards.
     */
    private final Network network;

    /**
     * Boolean value, that tells the Process to send a Request {@link Message}. This value is only used for the simulation.
     */
    private Boolean run = false;

    /**
     * Counter for the received Acknowledge {@link Message}s as int. If the value equals the count of all other Process in the {@link Network}, this Process has the permission to operate on
     * the critical section.
     */
    private Integer permissionsReceived = 0;

    /**
     * Boolean value, that tells the Process to end it's run method.
     */
    private Boolean stop = false;

    /**
     * A queue, that contains the Request {@link Message}s as {@link QueueEntry QueueEntries} as a PriorityQueue. It contains a Comparator, that sorts the Messages in order of the extended lamport time
     * (in order of their timestamp and their id).
     */
    private PriorityQueue<QueueEntry> processQueue = new PriorityQueue<>((entry1, entry2) -> {
            if (entry1.getTimestamp() < entry2.getTimestamp() ||
                    entry1.getTimestamp() == entry2.getTimestamp() && entry1.getProcessId() < entry2.getProcessId())
                return -1;
            else
                return 1;
    });


    /**
     * A queue, that contains the ingoing {@link Message}s as a PriorityQueue. It contains a Comparator, that sorts the Messages in order of the extended lamport time
     * (in order of their timestamp and their id).
     */
    private PriorityQueue<Message> messageQueue = new PriorityQueue<>((message1, message2) -> {
        if(message1.getLamportTime() < message2.getLamportTime() ||
                message1.getLamportTime() == message2.getLamportTime() && message1.getSenderId() < message2.getSenderId())
            return -1;
        else
            return 1;
    });


    /**
     * The constructor for a Process.
     * @param network A reference to the {@link Network}, that created this process.
     * @param processId A unique id for this Process as int.
     */
    public Process(Network network, int processId) {
        this.network = network;
        this.processId = processId;

        // create new LamportClock, with reference to this Process
        this.lamportClock = new LamportClock(this);
    }

    /**
     * Getter method for the id of the Process.
     * @return The id of the Process as int.
     */
    public int getProcessId() {
        return processId;
    }

    /**
     * Getter method for the {@link LamportClock} of the Process.
     * @return The LamportClock of the Process.
     */
    private LamportClock getLamportClock() {
        return lamportClock;
    }

    /**
     * Method that increases the {@link LamportClock} for each operation of the Process.
     */
    private void increaseLamportClock() {
        lamportClock.increaseLamportTime();
    }

    /**
     * A method that represents the receive of a {@link Message}. It's added afterwards to the messageQueue.
     * @param message The Message, which was send to this Process by using the {@link Network}.
     */
    public void receive(Message message) {

        // check if thread is still running, if not, stop method
        if (stop)
            return;

        // add message to messageQueue
        synchronized (messageQueue) {
            messageQueue.add(message);
        }
    }

    /**
     * Method that processes {@link Message}s according to their {@link Message.MessageType}. They can be handeld as REQUEST, ACKNOWLEDGE, RELEASE or the simulation specific RUN_COMMAND.
     * @param message The Message, that should be processed.
     */
    public void processMessage(Message message) {

        // compare lamportClocks and take higher value
        this.lamportClock.compareLamportClocks(message.getLamportTime());

        // increase lamport time for action
        increaseLamportClock();

        // for a RUN_COMMAND Message (only for simulation), set run to true
        if(message.getMessageType().equals(Message.MessageType.RUN_COMMAND)) {
            // print RUN_COMMAND receive
            printAction(Message.MessageType.RUN_COMMAND, this.lamportClock.getLamportTime(), false);

            synchronized (run) {
                run = true;
            }
        }

        // for a REQUEST Message,
        else if(message.getMessageType().equals(Message.MessageType.REQUEST)) {

            // print REQUEST receive
            printAction(Message.MessageType.REQUEST, this.lamportClock.getLamportTime(), false);

            // receive the message and add new entry to queue
            synchronized (processQueue) {
                processQueue.add(new QueueEntry(message.getSenderId(), message.getLamportTime()));
            }

            // reply with acknowledge-message
            network.sendMessage(new Message(processId, message.getSenderId(), Message.MessageType.ACKNOWLEDGE, lamportClock.getLamportTime()));

            // print ACKNOWLEDGE send
            printAction(Message.MessageType.ACKNOWLEDGE, this.lamportClock.getLamportTime(), true);
        }

        // for a ACKNOWLEDGE message
        else if(message.getMessageType().equals(Message.MessageType.ACKNOWLEDGE)) {

            // print ACKNOWLEDGE receive
            printAction(Message.MessageType.ACKNOWLEDGE, this.lamportClock.getLamportTime(), false);

            // count counter permissionsReceived
            synchronized (permissionsReceived) {
                permissionsReceived++;
            }

            // check if all permissions are granted (if so, do some action)
            checkAcknowledge();
        }

        // for a RELEASE message
        else if(message.getMessageType().equals(Message.MessageType.RELEASE)) {
            synchronized (processQueue) {
                // check if senderId equals id of first Process in queue, if not, print exception and stop method
                if (message.getSenderId() != processQueue.peek().getProcessId()) {
                    new InvalidAttributeValueException("Retrieved RELEASE message from process " + processId + ", which wasn't first in queue!").printStackTrace();
                    return;
                }

                // remove first entry from processQueue
                processQueue.poll();

                // if current process is first in queue, check ACKNOWLEDGE
                if (processQueue.size() > 0 && processQueue.peek().getProcessId() == processId) {
                    checkAcknowledge();
                }
            }
        }
    }

    /**
     * Method that sends a REQUEST {@link Message} and adds this process to the processQueue.
     */
    private void request() {
        // increase lamport time for action
        increaseLamportClock();

        // process is added to processQueue
        synchronized (processQueue) {
            processQueue.add(new QueueEntry(processId, lamportClock.getLamportTime()));
        }

        // send new REQUEST Message to the other Processes
        network.sendMessage(new Message(this.processId, -1, Message.MessageType.REQUEST, lamportClock.getLamportTime()));

        // print REQUEST action
        printAction(Message.MessageType.REQUEST, this.lamportClock.getLamportTime(),true);
    }

    /**
     * Method that checks if all ACKNOWLEDGE {@link Message}s are received and current Process is first in queue. In this case, the method, which simulates some action, is called.
     */
    private void checkAcknowledge() {
        synchronized (processQueue) {
            if (permissionsReceived == network.getProcessCount() - 1 && processQueue.peek().getProcessId() == processId) {
                doSomeAction();
            }
        }
    }

    /**
     * Method that simulates some action of the Process Thread. Afterwards a RELEASE message is send to the other Processes.
     */
    private void doSomeAction() {
        // increase lamport time for action
        increaseLamportClock();

        // call method that simulates actions in the critical section
        network.criticalSection(this);
//
        // afterwards reset permissionsReceived, send release message and remove this process from processQueue
        synchronized (permissionsReceived) {
            permissionsReceived = 0;
        }
        network.sendMessage(new Message(processId, -1, Message.MessageType.RELEASE, lamportClock.getLamportTime()));
        synchronized (processQueue) {
            processQueue.poll();
        }

        // print RELEASE action
        printAction(Message.MessageType.RELEASE, this.lamportClock.getLamportTime(), true);
    }

    /**
     * The run method, that is called by the threadPool and executed till the stop method is called.
     */
    public void run() {

        // the process with the id 0 starts the network by sending a request and sending a RUN_COMMAND to the next process (because every thread sends such a command, multiple REQUEST message will accumulate and the processQueues will be filled)
        if(processId == 0) {
            request();
            network.sendMessage(new Message(processId, (processId +1)%network.getProcessCount(), Message.MessageType.RUN_COMMAND, lamportClock.getLamportTime()));
        }

        // thread loop
        while (true) {

            // handle all incoming messages
            if (messageQueue.size() > 0) {
                synchronized (messageQueue) {
                    processMessage(messageQueue.poll());
                }
            }

            // if RUN_COMMAND was received and processed, send a REQUEST message and a RUN_COMMAND to next process
            synchronized (run) {
                if (run) {
                    request();
                    network.sendMessage(new Message(processId, (processId + 1) % network.getProcessCount(), Message.MessageType.RUN_COMMAND, lamportClock.getLamportTime()));
                    printAction(Message.MessageType.RUN_COMMAND, this.lamportClock.getLamportTime(), true);
                    run = false;
                }
            }

            // if stop method was called, break loop (easier this way instead of using stop for the loop)
            synchronized (stop) {
                if(stop)
                    break;
            }
        }

    }

    /**
     * A method for printing an action to the console.
     * @param messageType The type of the {@link Message} as {@link Message.MessageType}.
     * @param send Boolean parameter, if the Message was send (true) or received (false).
     */
    private void printAction(Message.MessageType messageType, long timestamp, boolean send) {
        System.out.println("Time " + timestamp + ": Process " + processId + ((send) ? " send ": " received ") + messageType.toString());
    }

    /**
     * Method that tells Process to stop it's run method.
     */
    public void stop() {
        synchronized (stop) {
            stop = true;
            synchronized (processQueue) {
                System.out.println("Time " + lamportClock.getLamportTime() + ": Process " + processId + " stopped! Size of process queue at the end: " + processQueue.size());
            }
        }
    }

    /**
     * Class that represents the entries in the queue, consisting of processId and timestamp.
     */
    public class QueueEntry {

        /**
         * Id of the Process as int. When set in the constructor, it can't be changed afterwards.
         */
        private final int processId;

        /**
         * Timestamp of the {@link LamportClock} as long. When set in the constructor, it can't be changed afterwards.
         */
        private final long timestamp;

        /**
         * Constructor for an entry in the Request waiting queue.
         * @param processId Id of the Process, that send an Request {@link Message}.
         * @param timestamp Timestamp of the {@link LamportClock}, when Message was send.
         */
        public QueueEntry(int processId, long timestamp) {
            this.processId = processId;
            this.timestamp = timestamp;
        }

        /**
         * Getter method for the id of the Process.
         * @return The id of the Process as int.
         */
        public int getProcessId() {
            return processId;
        }

        /**
         * Getter method for the timestamp of the {@link LamportClock}, when {@link Message} was send.
         * @return Timestamp when Message was send as long.
         */
        public long getTimestamp() {
            return timestamp;
        }
    }
}