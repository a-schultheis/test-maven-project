import javax.management.InvalidAttributeValueException;

/**
 * This class represents a Lamport clock. It contains a reference to the {@link Process}, so that the identifier condition of the extended lamport time can be fulfilled.
 */
public class LamportClock {

    /**
     * The reference to the {@link Process}, the LamportClock belongs to. It's used as unique identifier to fulfill the condition of the extended lamport time.
     * The Process can't be changed, if it was set once.
     */
    private final Process identifier;

    /**
     * The time of the LamportClock is represented as a Long. A long value corresponds 64 bit, an int value only 32 bit. So, by using the long value, a much higher time interval can be mapped.
     * (If there would be 1000 events per second, int would correspond to 50 days, long about 584 million years.)
     */
    private long lamportTime = 0;

    /**
     * Constructor for a new LamportClock.
     * @param identifier A reference to the unique {@link Process} object, the LamportClock belongs to.
     */
    public LamportClock(Process identifier) {
        this.identifier = identifier;
    }

    /**
     * Getter method for the unique {@link Process} object.
     * @return Reference to the unique Process object.
     */
    public Process getIdentifier() {
        return identifier;
    }

    /**
     * Getter method for the lamport time.
     * @return The lamport time as a long.
     */
    public long getLamportTime() {
        return lamportTime;
    }

    /**
     * Setter method to change the lamport time. This method may only be used to update the time to a higher value.
     * @param lamportTime New value for the lamport time as long.
     */
    private void setLamportTime(long lamportTime) {
        if(lamportTime < this.lamportTime) {
            new InvalidAttributeValueException("Non valid value for LamportClock! A new lamport time has to be higher or equal.").printStackTrace();
            return;
        }
        this.lamportTime = lamportTime;
    }

    /**
     * Method to increase the lamport time for one time unit.
     */
    public void increaseLamportTime() {
        lamportTime++;
    }

    /**
     * A method that compares two lamportClocks and uses the highest value for the lamport time for both.
     * @param lamportClock Another lamportClock, to which the current clock should be compared.
     */
    public void compareLamportClocks(LamportClock lamportClock) {
        this.lamportTime = Math.max(this.lamportTime, lamportClock.getLamportTime());
        lamportClock.setLamportTime(Math.max(this.lamportTime, lamportClock.getLamportTime()));
    }

    /**
     * A method that compares the time of the lamport clock with a timestamp. The highest value is chosen for this lamportClock.
     * @param lamportTime A value of a lamportClock as long.
     */
    public void compareLamportClocks(long lamportTime) {
        this.lamportTime = Math.max(this.lamportTime, lamportTime);
    }
}