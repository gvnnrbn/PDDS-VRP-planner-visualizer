package utils;

// TODO: month handling is incorrect, it doesn't matter, just use days
public class Time implements Comparable<Time> {
    private int year;
    private int month;
    private int day;
    private int hour;
    private int min;

    public Time(int year,int month, int day, int hour, int min) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.min = min;
    }

    @Override
    public String toString() {
        return String.format("%04d/%02d/%02d %02d:%02d", year, month, day, hour, min);
    }

    @Override
    public int compareTo(Time other) {
        if (this.year != other.year) return this.year - other.year;
        if (this.month != other.month) return this.month - other.month;
        if (this.day != other.day) return this.day - other.day;
        if (this.hour != other.hour) return this.hour - other.hour;
        return this.min - other.min;
    }

    public boolean isBefore(Time other) {
        if (this.year != other.year) return this.year < other.year;
        if (this.month != other.month) return this.month < other.month;
        if (this.day != other.day) return this.day < other.day;
        if (this.hour != other.hour) return this.hour < other.hour;
        return this.min < other.min;
    }

    public boolean isBeforeOrAt(Time other) {
        if (this.year != other.year) return this.year < other.year;
        if (this.month != other.month) return this.month < other.month;
        if (this.day != other.day) return this.day < other.day;
        if (this.hour != other.hour) return this.hour < other.hour;
        return this.min <= other.min;
    }
    

    public boolean isAfter(Time other) {
        return other.isBefore(this);
    }

    public Time addMinutes(int minutes) {
        int totalMinutes = this.min + minutes;
        int newMin = totalMinutes % 60;
        int extraHours = totalMinutes / 60;
        
        int totalHours = this.hour + extraHours;
        int newHour = totalHours % 24;
        int extraDays = totalHours / 24;
        
        int newDay = this.day + extraDays;
        int newMonth = this.month;
        int newYear = this.year;
        // Adjust for months and years (assuming 30 days per month)
        while (newDay > 30) {
            newDay -= 30;
            newMonth++;
            if (newMonth > 12) {
                newMonth = 1;
                newYear++;
            }
        }
        
        return new Time(newYear, newMonth, newDay, newHour, newMin);
    }

    public Time subtractMinutes(int minutes) {
        return addMinutes(-minutes);
    }

    public int minutesUntil(Time other) {
        int yearsDiff = (other.year - this.year) * 12 * 30 * 24 * 60;
        int monthsDiff = (other.month - this.month) * 30 * 24 * 60;
        int daysDiff = (other.day - this.day) * 24 * 60;
        int hoursDiff = (other.hour - this.hour) * 60;
        int minsDiff = other.min - this.min;
        
        return yearsDiff + monthsDiff + daysDiff + hoursDiff + minsDiff;
    }

    public int minutesSince(Time other) {
        return -other.minutesUntil(this);
    }

    public int getHour() {
        return this.hour;
    }
    public Time copy() {
        return new Time(this.year, this.month, this.day, this.hour, this.min);
    }
    public Time addTime(Time other) {
        int totalMinutes = this.min + other.min;
        int newMin = totalMinutes % 60;
        int extraHours = totalMinutes / 60;

        int totalHours = this.hour + other.hour + extraHours;
        int newHour = totalHours % 24;
        int extraDays = totalHours / 24;

        int totalDays = this.day + other.day + extraDays;
        int newDay = totalDays;
        int newMonth = this.month;
        int newYear = this.year;

        // Adjust for months and years (assuming 30 days per month)
        while (newDay > 30) {
            newDay -= 30;
            newMonth++;
            if (newMonth > 12) {
                newMonth = 1;
                newYear++;
            }
        }

        return new Time(newYear, newMonth, newDay, newHour, newMin);
    }
}
