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

    @Override
    public int compareTo(TimeMoment other) {
        if (this.day != other.day) {
            return Integer.compare(this.day, other.day);
        }
        if (this.hour != other.hour) {
            return Integer.compare(this.hour, other.hour);
        }
        return Integer.compare(this.minute, other.minute);
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