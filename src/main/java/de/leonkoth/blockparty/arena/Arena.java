package de.leonkoth.blockparty.arena;

import de.leonkoth.blockparty.BlockParty;
import de.leonkoth.blockparty.arena.ArenaDataManager.ArenaDataSet;
import de.leonkoth.blockparty.event.PlayerEliminateEvent;
import de.leonkoth.blockparty.event.PlayerJoinArenaEvent;
import de.leonkoth.blockparty.event.PlayerLeaveArenaEvent;
import de.leonkoth.blockparty.floor.Floor;
import de.leonkoth.blockparty.floor.FloorPattern;
import de.leonkoth.blockparty.phase.PhaseHandler;
import de.leonkoth.blockparty.player.PlayerInfo;
import de.leonkoth.blockparty.player.PlayerState;
import de.leonkoth.blockparty.song.SongManager;
import de.pauhull.utils.locale.storage.LocaleString;
import de.pauhull.utils.particle.ParticlePlayer;
import de.pauhull.utils.particle.v1_13.Particles;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static de.leonkoth.blockparty.arena.ArenaState.LOBBY;

/**
 * Created by Leon on 14.03.2018.
 * Project Blockparty2
 * © 2016 - Leon Koth
 */
public class Arena {

    public static final double TIME_REDUCTION_PER_LEVEL = 0.5;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 15;
    public static final int LOBBY_COUNTDOWN = 30;
    public static final int DISTANCE_TO_OUT_AREA = 5;
    public static final int TIME_TO_SEARCH = 8;
    public static final int AMOUNT_OF_LEVELS = 15;
    public static final int BOOST_DURATION = 10;

    private BlockParty blockParty;

    @Getter
    private ArenaDataSet data;

    @Setter
    @Getter
    private ArenaDataManager arenaDataManager;

    @Setter
    @Getter
    private ArenaState arenaState;

    @Setter
    @Getter
    private GameState gameState;

    @Setter
    @Getter
    private List<PlayerInfo> playersInArena;

    @Setter
    @Getter
    private PhaseHandler phaseHandler;

    @Setter
    @Getter
    private ParticlePlayer particlePlayer;

    public Arena(String name, BlockParty blockParty, boolean save) {
        this.blockParty = blockParty;
        this.arenaState = LOBBY;
        this.gameState = GameState.WAIT;
        this.data = new ArenaDataSet();
        this.arenaDataManager = new ArenaDataManager(this);
        this.phaseHandler = new PhaseHandler(blockParty, this);
        this.particlePlayer = new ParticlePlayer(Particles.CLOUD);
        this.playersInArena = new ArrayList<>();

        this.data.setName(name);
        this.data.setMinPlayers(MIN_PLAYERS);
        this.data.setMaxPlayers(MAX_PLAYERS);
        this.data.setLobbyCountdown(LOBBY_COUNTDOWN);
        this.data.setDistanceToOutArea(DISTANCE_TO_OUT_AREA);
        this.data.setTimeToSearch(TIME_TO_SEARCH);
        this.data.setTimeReductionPerLevel(TIME_REDUCTION_PER_LEVEL);
        this.data.setLevelAmount(AMOUNT_OF_LEVELS);
        this.data.setBoostDuration(BOOST_DURATION);
        this.data.setEnabled(false);
        this.data.setEnableLightnings(true);
        this.data.setEnableParticles(true);
        this.data.setAutoRestart(true);
        this.data.setEnableBoosts(true);
        this.data.setAllowJoinDuringGame(true);
        this.data.setEnableFireworksOnWin(true);
        this.data.setUseAutoGeneratedFloors(true);
        this.data.setUsePatternFloors(true);
        this.data.setEnableActionbarInfo(true);
        this.data.setEnableActionbarInfo(true);
        this.data.setUseNoteBlockSongs(false);
        this.data.setUseWebSongs(true);
        this.data.setTimerResetOnPlayerJoin(false);
        this.data.setEnableFallingBlocks(false);
        this.data.setAutoKick(false);
        this.data.setSongManager(new SongManager(this, new ArrayList<>()));
        this.data.setSigns(new SignList());

        if (save)
            this.saveData();
    }

    public static boolean create(String name) {
        if (isLoaded(name)) {
            return false;
        }

        BlockParty blockParty = BlockParty.getInstance();
        Arena arena = new Arena(name, blockParty, true);
        blockParty.getArenas().add(arena);

        return true;
    }

    public static Arena loadFromFile(String name) {
        BlockParty blockParty = BlockParty.getInstance();
        Arena arena;

        if (Arena.isLoaded(name)) {
            arena = Arena.getByName(name);
        } else {
            arena = new Arena(name, blockParty, false);
            arena.arenaDataManager.loadData();
        }

        return arena;
    }

    public static Arena getByName(String name) {
        for (Arena arena : BlockParty.getInstance().getArenas()) {
            if (arena.getName().equals(name)) {
                return arena;
            }
        }

        return null;
    }

    public static void startUpdatingSigns(int millis) {
        BlockParty.getInstance().getScheduledExecutorService().scheduleAtFixedRate(() -> {

            if (BlockParty.getInstance().isSignsEnabled()) {
                for (Arena arena : BlockParty.getInstance().getArenas()) {
                    arena.updateSigns();
                }
            }

        }, 0, millis, TimeUnit.MILLISECONDS);
    }

    public static boolean isLoaded(String name) {

        if (BlockParty.getInstance().getArenas() == null)
            return false;

        for (Arena arena : BlockParty.getInstance().getArenas()) {
            if (arena.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    public static void saveAll() {
        for (Arena arena : BlockParty.getInstance().getArenas()) {
            arena.save();
        }
    }

    public void save() {
        try {
            arenaDataManager.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData() {
        arenaDataManager.save(data, true);
    }

    public PlayerJoinArenaEvent addPlayer(Player player) {

        PlayerInfo playerInfo = PlayerInfo.getFromPlayer(player);

        if (playerInfo == null) {
            blockParty.getPlayers().add(new PlayerInfo(player.getName(), player.getUniqueId(), 0, 0));
            playerInfo = PlayerInfo.getFromPlayer(player);
        }

        PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(this, player, playerInfo);
        Bukkit.getPluginManager().callEvent(event);

        return event;
    }

    public boolean removePlayer(Player player) {

        PlayerInfo playerInfo = PlayerInfo.getFromPlayer(player);
        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(this, player, playerInfo);
        Bukkit.getPluginManager().callEvent(event);

        return event.isCancelled();
    }

    public boolean removePattern(String name) {

        if (data.floor.getPatternNames().contains(name)) {
            data.floor.getPatternNames().remove(name);

            data.floor.getFloorPatterns().removeIf(pattern -> pattern.getName().equals(name)); // Java 8

            arenaDataManager.getConfig().set("configuration.Floor.EnabledFloors", data.floor.getPatternNames());
            try {
                arenaDataManager.save();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        return false;
    }

    public boolean addPattern(FloorPattern pattern) {
        data.floor.getPatternNames().add(pattern.getName());
        data.floor.getFloorPatterns().add(pattern);
        arenaDataManager.save(data);
        return true;
    }

    public void updateSigns() {
        Iterator<Location> iterator = data.signs.getSigns().iterator();
        boolean save = false;
        while (iterator.hasNext()) {
            Location location = iterator.next();
            Block block = location.getBlock();

            if (block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) block.getState();
                String[] lines = new String[4];

                if (!isEnabled()) {
                    lines = getLines(blockParty.getConfig().getConfig(), "JoinSigns.Lines.Disabled");
                } else {
                    switch (arenaState) {
                        case LOBBY:
                            if (getPlayersInArena().size() >= getMaxPlayers()) {
                                lines = getLines(blockParty.getConfig().getConfig(), "JoinSigns.Lines.LobbyFull");
                            } else {
                                lines = getLines(blockParty.getConfig().getConfig(), "JoinSigns.Lines.Lobby");
                            }
                            break;

                        case INGAME:
                            lines = getLines(blockParty.getConfig().getConfig(), "JoinSigns.Lines.Ingame");
                            break;

                        case ENDING:
                            lines = getLines(blockParty.getConfig().getConfig(), "JoinSigns.Lines.Ending");
                            break;
                    }
                }

                for (int i = 0; i < 4; i++) {
                    sign.setLine(i, lines[i]);
                }

            } else {
                iterator.remove();
                save = true;
            }
        }

        if (save)
            arenaDataManager.save(data);
    }

    private String[] getLines(FileConfiguration config, String path) {
        String[] arr = new String[4];
        for (int i = 1; i <= 4; i++) {
            String newPath = path + ".Lines." + Integer.toString(i);

            if (!config.isString(newPath)) {
                continue;
            }

            arr[i - 1] = ChatColor.translateAlternateColorCodes('&', config.getString(newPath))
                    .replace("%ARENA%", data.name)
                    .replace("%PLAYERS%", Integer.toString(playersInArena.size()))
                    .replace("%MAX_PLAYERS%", Integer.toString(data.maxPlayers))
                    .replace("%ALIVE%", Integer.toString(getIngamePlayers()));
        }

        return arr;
    }

    public void broadcast(LocaleString prefix, LocaleString message, boolean onlyIngame, PlayerInfo exception, String... placeholders) {
        broadcast(prefix, message, onlyIngame, new PlayerInfo[]{exception}, placeholders);
    }

    public void broadcast(LocaleString prefix, LocaleString message, boolean onlyIngame, PlayerInfo[] exceptions, String... placeholders) {

        playerLoop:
        for (PlayerInfo playerInfo : playersInArena) {

            for (PlayerInfo exception : exceptions) {
                if (playerInfo.equals(exception)) {
                    continue playerLoop;
                }
            }

            if (onlyIngame && playerInfo.getPlayerState() != PlayerState.INGAME) {
                continue;
            }

            message.message(prefix, playerInfo.asPlayer(), placeholders);
        }
    }

    public void kickAllPlayers() {
        Iterator iterator = playersInArena.iterator();

        while (iterator.hasNext()) {
            PlayerInfo info = (PlayerInfo) iterator.next();
            removePlayer(info.asPlayer());
        }
    }

    public void delete() {
        for (PlayerInfo playerInfo : playersInArena) {
            removePlayer(playerInfo.asPlayer());
        }

        blockParty.getArenas().remove(this);
        arenaDataManager.delete();
    }

    public void eliminate(PlayerInfo playerInfo) {
        PlayerEliminateEvent event = new PlayerEliminateEvent(this, playerInfo.asPlayer(), playerInfo);
        Bukkit.getPluginManager().callEvent(event);
    }

    public int getIngamePlayers() {
        int ingamePlayers = 0;
        for (PlayerInfo info : playersInArena) {
            if (info.getPlayerState() == PlayerState.INGAME || info.getPlayerState() == PlayerState.WINNER)
                ingamePlayers++;
        }

        return ingamePlayers;
    }

    // Can be collapsed in IntelliJ IDEA
    // region Some getters/setters

    @Override
    public String toString() {
        return getName();
    }

    public int getDistanceToOutArea() {
        return data.distanceToOutArea;
    }

    public int getTimeToSearch() {
        return data.timeToSearch;
    }

    public int getLevelAmount() {
        return data.levelAmount;
    }

    public int getBoostDuration() {
        return data.boostDuration;
    }

    public int getMinPlayers() {
        return data.minPlayers;
    }

    public int getMaxPlayers() {
        return data.maxPlayers;
    }

    public int getLobbyCountdown() {
        return data.lobbyCountdown;
    }

    public double getTimeReductionPerLevel() {
        return data.timeReductionPerLevel;
    }

    public boolean isEnabled() {
        return data.enabled;
    }

    public void setEnabled(boolean enabled) {
        data.enabled = enabled;
    }

    public boolean isEnableParticles() {
        return data.enableParticles;
    }

    public boolean isEnableLightnings() {
        return data.enableLightnings;
    }

    public boolean isAutoRestart() {
        return data.autoRestart;
    }

    public boolean isAutoKick() {
        return data.autoKick;
    }

    public boolean isEnableBoosts() {
        return data.enableBoosts;
    }

    public boolean isEnableFallingBlocks() {
        return data.enableFallingBlocks;
    }

    public boolean isUseAutoGeneratedFloors() {
        return data.useAutoGeneratedFloors;
    }

    public boolean isUsePatternFloors() {
        return data.usePatternFloors;
    }

    public boolean isEnableActionbarInfo() {
        return data.enableActionbarInfo;
    }

    public boolean isUseNoteBlockSongs() {
        return data.useNoteBlockSongs;
    }

    public boolean isUseWebSongs() {
        return data.useWebSongs;
    }

    public boolean isEnableFireworksOnWin() {
        return data.enableFireworksOnWin;
    }

    public boolean isTimerResetOnPlayerJoin() {
        return data.timerResetOnPlayerJoin;
    }

    public boolean isAllowJoinDuringGame() {
        return data.allowJoinDuringGame;
    }

    public String getName() {
        return data.name;
    }

    public SongManager getSongManager() {
        return data.songManager;
    }

    public Floor getFloor() {
        return data.floor;
    }

    public void setFloor(Floor floor) {
        data.floor = floor;
        arenaDataManager.save(data);
    }

    public Location getGameSpawn() {
        return data.gameSpawn;
    }

    public void setGameSpawn(Location gameSpawn) {
        data.gameSpawn = gameSpawn;
        arenaDataManager.save(data);
    }

    public Location getLobbySpawn() {
        return data.lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        data.lobbySpawn = lobbySpawn;
        arenaDataManager.save(data);
    }

    public SignList getSigns() {
        return data.signs;
    }

    // endregion

}
