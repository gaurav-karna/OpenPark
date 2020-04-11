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


    }

    // instance methods to check for parameter in the OCR results

    public boolean searchForTimeRange (FirebaseVisionText.Line line) {
        // checks if string starts with a number followed by a 'h' and contains a '-'
        return line.toString().matches("\\d.*h.*-.*");
    }

    public boolean searchForTimeAllowed (FirebaseVisionText.Line line) {
        String searchLine = line.toString();
        // checks line starts with number followed by 'min' or 'heures' with > 50% confidence
        if (searchLine.matches("\\d.*")) {
            if ((FuzzySearch.partialRatio(searchLine, "min") > 50) ||
                    (FuzzySearch.partialRatio(searchLine, "heures") > 50)) {
                return true;
            }
        }
        return false;
    }

}
