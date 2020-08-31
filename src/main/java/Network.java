import javax.management.InvalidAttributeValueException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class represents the network, that creates and stops the several {@link Process}es.
 */
public class Network {
    /**
     * A list that contains all created {@link Process}es.
     */
    private List<Process> processes = new ArrayList<Process>();

    /**
     * A ThreadPoolExecutor, that is used to execute the {@link Process}es.
     */
    private ThreadPoolExecutor threadPool;

    /**
     * The value for the duration of the ThreadPool, represented in milliseconds. If it was set once, it can't be changed.
     */
    private final int duration;

    /**
     * A {@link MessageQueue}, that saves all messages, that were sent using this network.
     */
    private MessageQueue messageQueue = new MessageQueue();

    /**
     * A List, that saves the operations made in the critical section as Strings.
     */
    private ArrayList<String> operationsOnCriticalSection = new ArrayList<>();
    /**
     * A unimportant int value, that could be any number. It's used to do operations in the critical section.
     */
    private int criticalInt = 10;
    /**
     * The number of the operations, that were made in the critical section, as int.
     */
    private int operationCount = 0;

    /**
     * The constructor for a network, which creates the {@link Process}es.
     * @param processCount The number of Processes, that should be created.
     * @param duration The duration in milliseconds till the Process Threads should be shutdown.
     */
    public Network(int processCount, int duration) {
        // create the processes
        for (int i = 0; i < processCount; i++) {
            processes.add(new Process(this, i));
        }
        // creation of the thread pool
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(processCount);

        this.duration = duration;
    }

    /**
     * A run method, that executes all {@link Process}es. After the duration time is reached, the thread pool is shutdown and log of the messages and operations in the cricital sections are printed.
     */
    public void run() {
        for (Runnable process: processes) {
            threadPool.execute(process);
        }

        // set keepAliveTime to duration
        threadPool.setKeepAliveTime(duration, TimeUnit.SECONDS);

        // wait statement, till threads are shutdown, before going on
        while(threadPool.getActiveCount() > 0) { }

        // shutdown thread pool
        threadPool.shutdown();
        System.out.println("Threadpool shutdown!");

        // write log files for messages and operations in critical section
        logMessages();
        logCriticalSection();
    }

    /**
     * A method that represents the communication in the network. A {@link Process} can use this method to send a {@link Message} to another Process or all Processes. The destination is specified in the message.
     * Depending on the {@link Message.MessageType}, several operations will be executed. The messages will be send to the respective Process by using their receive method.
     * If a message contains a lamport time, that is higher than the duration or equal to it, the method tells all Processes to stop and than terminates immediately.
     * @param message The Message to be send.
     */
    public void sendMessage(Message message) {
        // if the lamport time is higher than the duration or equal, all Processes are stopped and this method terminates
        if(message.getLamportTime() >= duration) {
            for (Process process: processes)
                process.stop();
            return;
        }

        // check the sender id, print an exception, if it's wrong, and terminate
        if ((message.getSenderId() < 0) || (message.getSenderId() >= processes.size())) {
            new InvalidAttributeValueException("The senderId " + message.getSenderId() + " doesn't exist!");
            return;
        }

        // case for unicast: ACKNOWLEDGE or RUN_COMMAND
        if(message.getMessageType().equals(Message.MessageType.ACKNOWLEDGE) || message.getMessageType().equals(Message.MessageType.RUN_COMMAND)) {

            // check the receiver id, print an exception, if it's wrong, and terminate
            if (message.getReceiverId() < 0 || message.getReceiverId() >= processes.size()) {
                new InvalidAttributeValueException("The receiver " + message.getReceiverId() + " doesn't exist!");
                return;
            }

            // add new message to message queue
            synchronized (messageQueue) {
                messageQueue.put(message);
            }

            // find Process with receiver id and pass message to it
            for (Process process: processes) {
                if(process.getProcessId() == message.getReceiverId()) {
                    process.receive(message);
                    break;
                }
            }
        }

        // case for multicast: REQUEST or RELEASE
        else if(message.getMessageType().equals (Message.MessageType.REQUEST) || message.getMessageType().equals (Message.MessageType.RELEASE)) {

            // create new message for each Process (except the sender) and pass it to it
            for (Process process : processes) {

                // skip the sender Process
                if (process.getProcessId() == message.getSenderId()) continue;

                // create new message and set id for Process
                Message newMessage = message.clone();
                newMessage.setReceiverId(process.getProcessId());
                process.receive(message);

                // add new message to message queue
                synchronized (messageQueue) {
                    messageQueue.put(newMessage);
                }
            }

        }
    }

    /**
     * A method, that represents the critical section, where only one {@link Process} thread is allowed to work in at once. This method increases or decreases an Integer value, depending on if the id of the process is a
     * odd or even number. The time of accesses is count as operationCount. The operationCount, the criticalInt and the id of the current working process is saved as a String and added to the list, which contains the operations.
     * @param process The reference to the process object, which is runing this method.
     */
    public void criticalSection(Process process) {
        operationsOnCriticalSection.add("Operation " + operationCount++ + ": Process " + process.getProcessId() + " changed critical int from " + criticalInt + " to " +
                ((process.getProcessId() % 2 == 0) ? ++criticalInt : -- criticalInt));
    }

    /**
     * Getter method for the count of active {@link Process}es.
     * @return The count of processes as int.
     */
    public int getProcessCount() {
        return processes.size();
    }

    /**
     * This methods creates a log of the messages as csv file in the output folder.
     */
    private void logMessages() {
        try {
            FileWriter fileWriter = new FileWriter(new File("").getAbsolutePath() + "/output/messageLog.csv");
            fileWriter.append("messageType,senderId,receiverId,timestamp");

            for (Message message: messageQueue.getQueue())
                fileWriter.append("\n" + message.toString());

            fileWriter.flush();
            fileWriter.close();

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * This method creates a log of the critical section as txt file in the output folder.
     */
    private void logCriticalSection() {
        try {
            FileWriter fileWriter = new FileWriter(new File("").getAbsolutePath() + "/output/criticalSectionLog.txt");
            fileWriter.append("Operations at critical section:");
            for (String operation: operationsOnCriticalSection) {
                fileWriter.append("\n" + operation);
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * A class to represent the message queue. This class isn't necessary, but it's used to allow only a put method for the queue.
     */
    public class MessageQueue {

        /**
         * The queue represented as final PriorityQueue. It contains a Comparator, that sorts the {@link Message}s in order of the extended lamport time (in order of their timestamp and their id).
          */
        private final PriorityQueue<Message> queue = new PriorityQueue<>((message1, message2) -> {
            if(message1.getLamportTime() < message2.getLamportTime() ||
                    message1.getLamportTime() == message2.getLamportTime() && message1.getSenderId()<message2.getSenderId())
                return -1;
            else
                return 1;
        });

        /**
         * Method to put a {@link Message} into the queue.
         * @param message The message to be put into the queue.
         */
        public void put(Message message) {
            queue.add(message);
        }

        /**
         * Getter method for a reference to the {@link Message} queue.
         * @return A reference to the message queue.
         */
        public PriorityQueue<Message> getQueue() {
            return queue;
        }
    }
}