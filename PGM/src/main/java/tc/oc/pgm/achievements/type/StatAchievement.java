package tc.oc.pgm.achievements.type;

import tc.oc.pgm.achievements.Achievement;
import tc.oc.pgm.achievements.type.stat.KillAchievements;

import java.util.ArrayList;

public class StatAchievement extends Achievement {

    private ArrayList<Integer> amounts;

    public KillAchievements killAchievements;

    public StatAchievement(String name, String displayName, String description, ArrayList<Integer> amounts) {
        super(name, displayName, description);
        this.amounts = amounts;
    }

    public ArrayList getAmounts() {
        return amounts;
    }
}
