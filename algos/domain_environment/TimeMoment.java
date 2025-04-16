package domain_environment;
public class TimeMoment implements Cloneable, Comparable<TimeMoment> {
    public int day;
    public int hour;
    public int minute;

    @Override
    public TimeMoment clone() {
        TimeMoment clone = new TimeMoment();
        clone.day = this.day;
        clone.hour = this.hour;
        clone.minute = this.minute;
        return clone;
    }

    public void addMinutes(int minutes) {
        this.minute += minutes;
        if (this.minute >= 60) {
            this.hour += this.minute / 60;
            this.minute = this.minute % 60;
        }
    }

    @Override
    public int compareTo(TimeMoment other) {
        int thisTotal = this.day * 1440 + this.hour * 60 + this.minute;
        int otherTotal = other.day * 1440 + other.hour * 60 + other.minute;
        return Integer.compare(thisTotal, otherTotal);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TimeMoment)) {
            return false;
        }
        TimeMoment other = (TimeMoment) obj;
        return this.day == other.day && 
               this.hour == other.hour && 
               this.minute == other.minute;
    }

    @Override
    public int hashCode() {
        int result = day;
        result = 31 * result + hour;
        result = 31 * result + minute;
        return result;
    }
} 