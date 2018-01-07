package tc.oc.pgm.polls;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import tc.oc.commons.core.plugin.PluginFacet;
import tc.oc.pgm.Config;
import tc.oc.pgm.PGM;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class PollBlacklist implements PluginFacet {

    private List<PGMMap> blacklistedMaps = new ArrayList<>();

    private final MapLibrary mapLibrary;

    @Inject PollBlacklist(MapLibrary mapLibrary) {
        this.mapLibrary = mapLibrary;
    }

    @Override
    public void enable() {
        loadPollBlacklist();
    }

    public void loadPollBlacklist() {
        Path filepath = Config.Poll.getPollAbleMapPath();
        if (filepath == null) return;
        List<String> lines = null;
        try {
            lines = Files.readAllLines(filepath, Charsets.UTF_8);
        } catch (IOException e) {
            PGM.get().getLogger().severe("Error in reading poll blacklist from file!");
        }
        if (lines == null) return;
        ImmutableList.Builder<PGMMap> maps = ImmutableList.builder();
        for(String line : lines) {
            line = line.trim();
            if(line.isEmpty()) {
                continue;
            }

            Optional<PGMMap> map = mapLibrary.getMapByNameOrId(line);
            if(map.isPresent()) {
                maps.add(map.get());
            } else {
                mapLibrary.getLogger().severe("Unknown map '" + line
                        + "' when parsing " + filepath.toString());
            }
        }
        this.blacklistedMaps = maps.build();
    }

    public boolean isBlacklisted(PGMMap map) {
        return blacklistedMaps.contains(map);
    }
}
