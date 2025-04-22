package domain;

public class Time {
    private int month;
    private int day;
    private int hour;
    private int min;

    public Time(int month, int day, int hour, int min) {
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.min = min;
    }

    public boolean isBefore(Time other) {
        if (this.month != other.month) return this.month < other.month;
        if (this.day != other.day) return this.day < other.day;
        if (this.hour != other.hour) return this.hour < other.hour;
        return this.min < other.min;
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
        
        // Simple month handling (assuming 30 days per month)
        while (newDay > 30) {
            newDay -= 30;
            newMonth++;
        }
        
        return new Time(newMonth, newDay, newHour, newMin);
    }

    public Time subtractMinutes(int minutes) {
        return addMinutes(-minutes);
    }

    public int minutesUntil(Time other) {
        int monthsDiff = (other.month - this.month) * 30 * 24 * 60;
        int daysDiff = (other.day - this.day) * 24 * 60;
        int hoursDiff = (other.hour - this.hour) * 60;
        int minsDiff = other.min - this.min;
        
        return monthsDiff + daysDiff + hoursDiff + minsDiff;
    }
}
