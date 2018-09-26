package de.leonkoth.blockparty.arena;

import de.leonkoth.blockparty.BlockParty;
import de.leonkoth.blockparty.floor.Floor;
import de.leonkoth.blockparty.song.SongManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Created by Leon on 14.03.2018.
 * Project Blockparty2
 * © 2016 - Leon Koth
 */
public class ArenaDataManager {

    @Getter
    private FileConfiguration config;

    private File file;
    private Arena arena;

    public ArenaDataManager(Arena arena) {
        this.arena = arena;

        try {
            setup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setup() throws IOException {
        this.file = new File(BlockParty.PLUGIN_FOLDER + "Arenas/" + arena.getName() + ".yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }

        config.save(file);
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveLocation(String path, Location location) {
        saveLocation(path, location, true);
    }

    public void saveLocation(String path, Location location, boolean save) {
        config.set(path + ".World", location.getWorld().getName());
        config.set(path + ".X", location.getX());
        config.set(path + ".Y", location.getY());
        config.set(path + ".Z", location.getZ());
        config.set(path + ".Yaw", location.getYaw());
        config.set(path + ".Pitch", location.getPitch());

        if (!save)
            return;

        try {
            this.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Location getLocation(String path) {
        World world = Bukkit.getWorld(config.getString(path + ".World"));
        double x = config.getDouble(path + ".X");
        double y = config.getDouble(path + ".Y");
        double z = config.getDouble(path + ".Z");
        float yaw = (float) config.getDouble(path + ".Yaw");
        float pitch = (float) config.getDouble(path + ".Pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public void save(ArenaDataSet dataSet) {
        save(dataSet, true);
    }

    public void save(ArenaDataSet dataSet, boolean save) {
        config.set("Configuration.Enabled", dataSet.isEnabled());
        config.set("Configuration.EnableParticles", dataSet.isEnableParticles());
        config.set("Configuration.EnableLightnings", dataSet.isEnableLightnings());
        config.set("Configuration.MinPlayers", dataSet.getMinPlayers());
        config.set("Configuration.MaxPlayers", dataSet.getMaxPlayers());
        config.set("Configuration.LobbyCountdown", dataSet.getLobbyCountdown());
        config.set("Configuration.TimeToSearch", dataSet.getTimeToSearch());
        config.set("Configuration.TimeReductionPerLevel", dataSet.getTimeReductionPerLevel());
        config.set("Configuration.TimerResetOnPlayerJoin", dataSet.isTimerResetOnPlayerJoin());
        config.set("Configuration.AllowJoinDuringGame", dataSet.isAllowJoinDuringGame());
        config.set("Configuration.DistanceToOutArea", dataSet.getDistanceToOutArea());
        config.set("Configuration.AmountOfLevels", dataSet.getLevelAmount());
        config.set("Configuration.BoostDuration", dataSet.getBoostDuration());
        config.set("Configuration.AutoRestart", dataSet.isAutoRestart());
        config.set("Configuration.AutoKick", dataSet.isAutoKick());
        config.set("Configuration.EnableBoosts", dataSet.isEnableBoosts());
        config.set("Configuration.EnableFallingBlocks", dataSet.isEnableFallingBlocks());
        config.set("Configuration.UseAutoGeneratedFloors", dataSet.isUseAutoGeneratedFloors());
        config.set("Configuration.UsePatternFloors", dataSet.isUsePatternFloors());
        config.set("Configuration.EnableActionbarInfo", dataSet.isEnableActionbarInfo());
        config.set("Configuration.EnableFireworksOnWin", dataSet.isEnableFireworksOnWin());
        config.set("Configuration.UseNoteblockSongs", dataSet.isUseNoteblockSongs());
        config.set("Configuration.UseWebSongs", dataSet.isUseWebSongs());

        if (dataSet.getSongManager() != null) {
            config.set("Configuration.Songs", dataSet.getSongManager().getSongNames());
        }

        if (dataSet.getFloor() != null) {
            config.set("Configuration.Floor.EnabledFloors", dataSet.getFloor().getPatternNames());
            config.set("Configuration.Floor.Length", dataSet.getFloor().getSize().getLength());
            config.set("Configuration.Floor.Width", dataSet.getFloor().getSize().getWidth());
        }

        if (!save)
            return;

        try {
            this.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        arena.setEnabled(config.getBoolean("Configuration.Enabled"));
        arena.setEnableParticles(config.getBoolean("Configuration.EnableParticles"));
        arena.setEnableLightnings(config.getBoolean("Configuration.EnableLightnings"));
        arena.setMinPlayers(config.getInt("Configuration.MinPlayers"));
        arena.setMaxPlayers(config.getInt("Configuration.MaxPlayers"));
        arena.setLobbyCountdown(config.getInt("Configuration.LobbyCountdown"));
        arena.setTimeToSearch(config.getInt("Configuration.TimeToSearch"));
        arena.setTimeReductionPerLevel(config.getDouble("Configuration.TimeReductionPerLevel"));
        arena.setTimerResetOnPlayerJoin(config.getBoolean("Configuration.TimerResetOnPlayerJoin"));
        arena.setAllowJoinDuringGame(config.getBoolean("Configuration.AllowJoinDuringGame"));
        arena.setDistanceToOutArea(config.getInt("Configuration.DistanceToOutArea"));
        arena.setLevelAmount(config.getInt("Configuration.AmountOfLevels"));
        arena.setBoostDuration(config.getInt("Configuration.BoostDuration"));
        arena.setAutoRestart(config.getBoolean("Configuration.AutoRestart"));
        arena.setAutoKick(config.getBoolean("Configuration.AutoKick"));
        arena.setEnableBoosts(config.getBoolean("Configuration.EnableBoosts"));
        arena.setEnableFallingBlocks(config.getBoolean("Configuration.EnableFallingBlocks"));
        arena.setUseAutoGeneratedFloors(config.getBoolean("Configuration.UseAutoGeneratedFloors"));
        arena.setUsePatternFloors(config.getBoolean("Configuration.UsePatternFloors"));
        arena.setEnableActionbarInfo(config.getBoolean("Configuration.EnableActionbarInfo"));
        arena.setEnableFireworksOnWin(config.getBoolean("Configuration.EnableFireworksOnWin"));
        arena.setUseNoteblockSongs(config.getBoolean("Configuration.UseNoteblockSongs"));
        arena.setUseWebSongs(config.getBoolean("Configuration.UseWebSongs"));
        arena.setSongManager(new SongManager(arena, config.getStringList("Configuration.Songs")));
    }

    public boolean delete() {
        return file.delete();
    }

    @AllArgsConstructor
    public static class ArenaDataSet {

        @Setter
        @Getter
        private int distanceToOutArea, timeToSearch, levelAmount, boostDuration, minPlayers, maxPlayers, lobbyCountdown;

        @Setter
        @Getter
        private double timeReductionPerLevel;

        @Setter
        @Getter
        private boolean enabled, enableParticles, enableLightnings, autoRestart, autoKick, enableBoosts, enableFallingBlocks, useAutoGeneratedFloors, usePatternFloors,
                enableActionbarInfo, useNoteblockSongs, useWebSongs, enableFireworksOnWin, timerResetOnPlayerJoin, allowJoinDuringGame;

        @Setter
        @Getter
        private String name;

        @Setter
        @Getter
        private SongManager songManager;

        @Setter
        @Getter
        private Floor floor;

    }

}
