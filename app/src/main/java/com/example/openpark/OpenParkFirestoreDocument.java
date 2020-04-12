package com.example.openpark;

public class OpenParkFirestoreDocument {

    // document fields
    private String canPark;
    private String timeStamp;   // use library to convert to String, for easy casting back to Time
    private String daysOfWeek;
    private String location;    // use library to convert to String, for easy casting back to Location
    private String sectors;
    private String timeLimit;

    // in expansion, add conversion to Time for filtering signs based on phone's time and date settings
    private String timeOfDay;
    private String timeOfYear;

    // extra information
    private String extraInfo;

    // public constructor
    public OpenParkFirestoreDocument() {}

    // document constructor
    public OpenParkFirestoreDocument(
            String canPark,
            String timeStamp,
            String daysOfWeek,
            String location,
            String sectors,
            String timeLimit,
            String timeOfDay,
            String timeOfYear,
            String extraInfo

    ) {
        this.canPark = canPark;
        this.timeStamp = timeStamp;
        this.daysOfWeek = daysOfWeek;
        this.location = location;
        this.sectors = sectors;
        this.timeLimit = timeLimit;
        this.timeOfDay = timeOfDay;
        this.timeOfYear = timeOfYear;
        this.extraInfo = extraInfo;
    }


    public String getCanPark() {
        return canPark;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public String getLocation() {
        return location;
    }

    public String getSectors() {
        return sectors;
    }

    public String getTimeLimit() {
        return timeLimit;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public String getTimeOfYear() {
        return timeOfYear;
    }

    public String getExtraInfo() {
        return extraInfo;
    }
}
