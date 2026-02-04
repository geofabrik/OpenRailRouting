package de.geofabrik.railway_routing.util;

import static com.graphhopper.json.Statement.Else;
import static com.graphhopper.json.Statement.ElseIf;
import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;

import java.util.Arrays;
import java.util.List;

import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;

public final class RailwayProfiles {
    private RailwayProfiles() {}
    
    public static Profile create(String name, boolean electric, int maxSpeed, boolean noYards) {
        Profile profile = new Profile(name);
        String speed = Integer.toString(maxSpeed);
        CustomModel customModel = new CustomModel().
            addToPriority(If("!rail_access || railway_class != RAIL", MULTIPLY, "0")).
            addToPriority(If("!preferred_direction", MULTIPLY, "0.7")).
            addToSpeed(If("true", LIMIT, "rail_average_speed")).
            addToSpeed(If("true", LIMIT, speed));
        if (electric) {
            if (noYards) {
                customModel.addToSpeed(If("railway_service == YARD || railway_service == SPUR", MULTIPLY, "0.0")).
                    addToSpeed(ElseIf("!(electrified == CONTACT_LINE || electrified == UNSET)", MULTIPLY, "0.0"));
            } else {
                customModel.addToSpeed(If("!(electrified == CONTACT_LINE || electrified == UNSET)", MULTIPLY, "0.0"));
            }
            customModel.addToSpeed(ElseIf("voltage >= 14000.0 && voltage <= 16000.0 && frequency >= 15.0 && frequency <= 17.5", MULTIPLY, "1.0")).
                addToSpeed(ElseIf("voltage == 0.0 && frequency == 0.0", MULTIPLY, "1.0")).
                addToSpeed(Else(MULTIPLY, "0.0"));
        } else if (noYards) {
            customModel.addToSpeed(If("railway_service == YARD || railway_service == SPUR", MULTIPLY, "0.0"));
        }
        profile.setCustomModel(customModel);
        return profile;
    }
    
    public static List<Profile> mapMatchingcreateProfiles() {
        Profile ice = RailwayProfiles.create("ice", true, 300, true);
        Profile intercity = RailwayProfiles.create("intercity", true, 160, true);
        return Arrays.asList(ice, intercity);
    }
}
