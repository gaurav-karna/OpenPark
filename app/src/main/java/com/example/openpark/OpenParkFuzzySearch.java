package com.example.openpark;

import java.util.HashMap;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import me.xdrop.fuzzywuzzy.FuzzySearch;

public class OpenParkFuzzySearch {

    // global variables and mapping
    public HashMap<String, String> daysOfWeek = new HashMap<String, String>();
    public HashMap<String, String> monthsOfYear = new HashMap<String, String>();

    // initialize globals
    public OpenParkFuzzySearch() {
        // init days of week
        daysOfWeek.put("LUNDI", "Monday");
        daysOfWeek.put("MARDI", "Tuesday");
        daysOfWeek.put("MERCREDI", "Wednesday");
        daysOfWeek.put("JEUDI", "Thursday");
        daysOfWeek.put("VENDREDI", "Friday");
        daysOfWeek.put("SAMEDI", "Saturday");
        daysOfWeek.put("DIMANCHE", "Sunday");
        // shortened
        daysOfWeek.put("LUN", "Monday");
        daysOfWeek.put("MAR", "Tuesday");
        daysOfWeek.put("MER", "Wednesday");
        daysOfWeek.put("JEU", "Thursday");
        daysOfWeek.put("VEN", "Friday");
        daysOfWeek.put("SAM", "Saturday");
        daysOfWeek.put("DIM", "Sunday");

        // init months of year
        monthsOfYear.put("JANVIER", "January");
        monthsOfYear.put("FÉVRIER", "February");
        monthsOfYear.put("MARS", "March");
        monthsOfYear.put("AVRIL", "April");
        monthsOfYear.put("MAI", "May");
        monthsOfYear.put("JUIN", "June");
        monthsOfYear.put("JUILLET", "July");
        monthsOfYear.put("AOÛT", "August");
        monthsOfYear.put("SEPTEMBRE", "September");
        monthsOfYear.put("OCTOBRE", "October");
        monthsOfYear.put("NOVEMBRE", "November");
        monthsOfYear.put("DÉCEMBRE", "December");
        // shortened for those that apply
        monthsOfYear.put("JAN", "January");
        monthsOfYear.put("FÉV", "February");
        monthsOfYear.put("JUIL", "July");
        monthsOfYear.put("SEPT", "September");
        monthsOfYear.put("OCT", "October");
        monthsOfYear.put("NOV", "November");
        monthsOfYear.put("DÉC", "December");


    }

    // instance methods to check for parameter in the OCR results
    // time range parameter search
    public boolean searchForTimeRange (FirebaseVisionText.Line line) {
        // checks if string starts with a number followed by a 'h' and contains a '-'
        return (
                line.getText().matches("\\d.*h.*-.*\\d.*h") ||
                line.getText().matches("\\d.*h.*\\d.*h") ||
                line.getText().matches("\\d.*h.*-.*") ||
                line.getText().matches("\\d.*h.*h") ||
                line.getText().matches("\\d.*h.*") ||
                line.getText().matches(".*h.*-.*\\dh") ||
                line.getText().matches(".*h.*\\dh")
        );
    }


    // time limit parameter search
    public boolean searchForTimeAllowed (FirebaseVisionText.Line line) {
        String searchLine = line.getText();
        // checks line starts with number followed by 'min' or 'heures' with > 75% confidence
        if (searchLine.matches("\\d.*")) {
            if ((FuzzySearch.partialRatio(searchLine, "min") > 75) ||
                    (FuzzySearch.partialRatio(searchLine, "heures") > 75)) {
                return true;
            }
        }
        return false;
    }


    // days of week parameter search
    public boolean searchForDaysOfWeek (FirebaseVisionText.Line line) {
        // iterate through days of week, and check partial similarity ratio against line text
        String searchLine = line.getText();

        // filter common exceptions
        if (
                (searchLine.contains("MUNIS")) ||
                (searchLine.matches(".*D.*UN")) ||
                (searchLine.contains("EXCEPTE")) ||
                (searchLine.contains("SECTE"))
        ) {
            System.out.println("MUNIS - " + (searchLine.contains("MUNIS")));
            System.out.println("DUN - " + (searchLine.matches(".*D.*UN")));
            return false;
        }

        String[] partsOfLine = searchLine.split(" ");
        for (String day : daysOfWeek.keySet()) {
            int totalScore = 0;
            for (String part : partsOfLine) {
                if ((part.length() > day.length()) && (day.length() == 3)) {
                    continue;      // skip comparison if part is longer than 3 letter abbreviation
                }
                totalScore += FuzzySearch.ratio(part, day);
            }
            if (totalScore > 75) {           // 75% confidence threshold
                return true;
            }
        }
        return false;
    }


    // time of year parameter search
    public boolean searchForTimeOfYear (FirebaseVisionText.Line line) {

        // iterate through months of year, and check partial similarity ratio against line text
        String searchLine = line.getText();

        // filter common exceptions
        if ((searchLine.contains("MUNIS")) || (searchLine.matches(".*D.*UN"))) {
            System.out.println("MUNIS - " + (searchLine.contains("MUNIS")));
            System.out.println("DUN - " + (searchLine.matches(".*D.*UN")));
            return false;
        }

        String[] partsOfLine = searchLine.split(" ");
        for (String month : monthsOfYear.keySet()) {
            int totalScore = 0;
            for (String part : partsOfLine) {
                totalScore += FuzzySearch.ratio(part, month);
            }
            if (totalScore > 75) {       // 75% confidence threshold
                return true;
            }
        }
        return false;
    }


    // search for sector number
    public boolean searchForSector (FirebaseVisionText.Line line) {
        String searchLine = line.getText();

        if (searchLine.matches("\\d.*")) {      // check if line is just number
            try {
                Integer.parseInt(searchLine);       // just number - most likely is the sector #
                return true;
            } catch (NumberFormatException e) {
                return false;       // line did not just have number
            }
        }

        return false;
    }
}
