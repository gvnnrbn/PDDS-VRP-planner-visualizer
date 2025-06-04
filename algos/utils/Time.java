package utils;

public class Time implements Comparable<Time>, Cloneable {
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

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return min;
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
        int totalMinutes = this.min - minutes;
        int newMin = (totalMinutes % 60 + 60) % 60; // Handle negative minutes
        int extraHours = (int)Math.floorDiv(totalMinutes, 60);
        
        int totalHours = this.hour + extraHours;
        int newHour = (totalHours % 24 + 24) % 24; // Handle negative hours
        int extraDays = (int)Math.floorDiv(totalHours, 24);
        
        int newDay = this.day + extraDays;
        int newMonth = this.month;
        int newYear = this.year;
        
        // Handle days underflow (assuming 30 days per month)
        while (newDay < 1) {
            newMonth--;
            if (newMonth < 1) {
                newMonth = 12;
                newYear--;
            }
            newDay += 30; // Add days from previous month
        }
        
        return new Time(newYear, newMonth, newDay, newHour, newMin);
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
        return -this.minutesUntil(other);
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

    public boolean isSameDate(Time other) {
        return this.year == other.year && this.month == other.month && this.day == other.day;
    }

    @Override
    public Time clone() {
        try {
            Time clone = (Time) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can never happen
        }
    }
}
