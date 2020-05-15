package com.example.openpark;

import java.io.Serializable;

public class OpenParkFirestoreDocument implements Serializable {

    // document fields
    private String canPark;
    private String timeStamp;   // use library to convert to String, for easy cast back to Time
    private String daysOfWeek;
    private String location;    // use library to convert to String, for easy cast back to Location
    private String sectors;
    private String timeLimit;

    // TODO: add conversion to Time for filtering signs based on phone's time and date settings
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

    public String mapInformation() {
        String display = "Park here?\n";

        if (!(this.canPark.equals("None"))) {
            if (this.canPark.equals("yes")) {
                display += "- You can park here under conditions:\n";
            } else if (this.canPark.equals("no")) {
                display += "- You can't park here under conditions:\n";
            } else if (this.canPark.equals("stop")) {
                display += "- You can't stop here under conditions:\n";
            }
        } else {
            display += "- None found\n";
        }

        // conditions
        display += "\nConditions:\n";
        boolean conditionsFound = false;       // one-time flag, otherwise 'None found'

        // cycle through all conditions
        if (!(this.timeOfYear.equals("None"))) {
            conditionsFound = true;
            display += "- During this time of year: " + this.timeOfYear + "\n";
        }

        if (!(this.daysOfWeek.equals("None"))) {
            conditionsFound = true;
            display += "- On the day(s): " + this.daysOfWeek + "\n";
        }

        if (!(this.timeOfDay.equals("None"))) {
            conditionsFound = true;
            display += "- In this time range of day: " + this.timeOfDay + "\n";
        }

        if (!(this.timeLimit.equals("None"))) {
            conditionsFound = true;
            display += "- For this amount of time: " + this.timeLimit + "\n";
        }

        if (!(this.sectors.equals("None"))) {
            conditionsFound = true;
            display += "- Above don't apply if vehicle has permit(s): " + this.sectors + "\n";
        }

        if (!(conditionsFound)) {
            display += "None found";
        }

        if (!(this.extraInfo.equals("None"))) {
            display += "\nExtra Information found:\n" + this.extraInfo;
        }

        if (!(this.location.equals("None"))) {
            display += "\nLocation: " + this.location;
        }

        return display;
    }
}
