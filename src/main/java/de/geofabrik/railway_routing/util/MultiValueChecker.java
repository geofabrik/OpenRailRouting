package de.geofabrik.railway_routing.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * This class provides some methods to check if a OSM tag value contains a specific value. The
 * value might be a list separated by semicolons.
 *  
 * @author Michael Reichert
 */
public class MultiValueChecker {

    /**
     * Read numeric values from a OSM tag value separated by semicolons.
     * @param <T> Numeric type, e.g. Integer or Double.
     * @param tagValue OSM tag value
     * @param parseFunction Function to parse a String to the provided type T, e.g. Double::parseDouble or Integer::parseInt.
     * @return List of read values or an empty list if nothing could be read.
     */
    public static <T> List<T> getNumbersFromTagValue(String tagValue, Function<String, T> parseFunction) {
        List<T> l;
        if (tagValue == null) {
            l = new ArrayList<T>(1);
            l.add(null);
            return l;
        }
        if (!tagValue.contains(";")) {
            l = new ArrayList<T>(1);
            try {
                l.add(parseFunction.apply(tagValue));
            } catch (NumberFormatException e) {
            }
            return l;
        }
        l = new ArrayList<T>(5);
        // split at ;
        String[] tokens = tagValue.split(";");
        for (String t : tokens) {
            try {
                T val = parseFunction.apply(t);
                l.add(val);
            } catch (NumberFormatException e) {
                return new ArrayList<T>();
            }
        }
        return l;
    }

    /**
     * Read multiple string values from a OSM tag value separated by semicolons.
     * @param tagValue OSM tag value
     * @return List of read values or an empty list if nothing could be read.
     */
    public static List<String> tagValueToList(String tagValue) {
        List<String> l;
        if (tagValue == null) {
            l = new ArrayList<String>(1);
            l.add(null);
            return l;
        }
        return Arrays.asList(tagValue.split(";"));
    }

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
