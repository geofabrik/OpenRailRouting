package de.geofabrik.railway_routing.util;

import java.util.ArrayList;

/**
 * This class provides some methods to check if a OSM tag value contains a specific value. The
 * value might be a list separated by semicolons.
 *  
 * @author Michael Reichert
 */
public class MultiValueChecker {

    /**
     * Check if a value of an OSM tag contains at least one "permitted" value.
     * 
     * @param tagValue OSM tag value
     * @param acceptedValues If one of these values is found, the method should return true.
     * @param resultIfNull This will be returned if the tag is not set at all or acceptedValues is empty. 
     */
    public static boolean tagContainsInt(String tagValue, ArrayList<Integer> acceptedValues, boolean resultIfNull) {
        if (tagValue == null || acceptedValues.isEmpty() || tagValue.equals("")) {
            return resultIfNull;
        }
        // check if string contains ;
        if (!tagValue.contains(";")) {
            try {
                return acceptedValues.contains(Integer.parseInt(tagValue));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // split at ;
        String[] tokens = tagValue.split(";");
        for (String t : tokens) {
            try {
                if (acceptedValues.contains(Integer.parseInt(t))) {
                    return true;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Check if a value of an OSM tag contains at least one "permitted" value.
     * 
     * @param tagValue OSM tag value
     * @param acceptedValues If one of these values is found, the method should return true.
     * @param resultIfNull This will be returned if the tag is not set at all or acceptedValues is empty. 
     */
    public static boolean tagContainsDouble(String tagValue, ArrayList<Double> acceptedValues, boolean resultIfNull) {
        if (tagValue == null || acceptedValues.isEmpty() || tagValue.equals("")) {
            return resultIfNull;
        }
        // check if string contains ;
        if (!tagValue.contains(";")) {
            try {
                return acceptedValues.contains(Double.parseDouble(tagValue));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // split at ;
        String[] tokens = tagValue.split(";");
        for (String t : tokens) {
            try {
                if (acceptedValues.contains(Double.parseDouble(t))) {
                    return true;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
