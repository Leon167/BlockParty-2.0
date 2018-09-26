package de.leonkoth.blockparty.floor;

import de.leonkoth.blockparty.BlockParty;
import de.leonkoth.blockparty.arena.Arena;
import de.leonkoth.blockparty.exception.FloorLoaderException;
import de.leonkoth.blockparty.floor.generator.AreaGenerator;
import de.leonkoth.blockparty.floor.generator.FloorGenerator;
import de.leonkoth.blockparty.floor.generator.SingleBlockGenerator;
import de.leonkoth.blockparty.floor.generator.StripeGenerator;
import de.leonkoth.blockparty.player.PlayerInfo;
import de.leonkoth.blockparty.player.PlayerState;
import de.leonkoth.blockparty.util.ColorBlock;
import de.leonkoth.blockparty.util.ParticlePlayer;
import de.leonkoth.blockparty.util.Size;
import de.leonkoth.blockparty.util.Util;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Leon on 14.03.2018.
 * Project Blockparty2
 * © 2016 - Leon Koth
 */
public class Floor {

    public static final int DEFAULT_ARENA_LENGTH = 42;
    public static final int DEFAULT_ARENA_WIDTH = 42;

    private Random random;
    private Arena arena;

    @Getter
    private Block currentBlock;

    @Setter
    @Getter
    private Size size;

    @Setter
    @Getter
    private Location[] bounds;

    @Setter
    @Getter
    private List<FloorPattern> floorPatterns;

    @Setter
    @Getter
    private List<String> patternNames;

    @Setter
    @Getter
    private List<FloorGenerator> generators = new ArrayList<>();

    public Floor(List<String> patternNames, Location[] bounds, Arena arena, Size size) {
        this.random = new Random();
        this.size = size;
        this.arena = arena;
        this.bounds = bounds;
        this.patternNames = patternNames;
        this.floorPatterns = new ArrayList<>();

        for (String name : patternNames) {
            File file = new File(BlockParty.PLUGIN_FOLDER + "Floors/" + name + ".floor");
            try {
                FloorPattern pattern = FloorLoader.readFloorPattern(file);
                floorPatterns.add(pattern);
            } catch (FileNotFoundException | FloorLoaderException e) {
                e.printStackTrace();
            }
        }

        this.generators.add(new AreaGenerator());
        this.generators.add(new SingleBlockGenerator());
        this.generators.add(new StripeGenerator());
    }

    public static boolean create(Arena arena, Location[] bounds, Size size) {

        List<String> floorNames = new ArrayList<>();

        Floor floor;
        if (arena.getFloor() == null) {
            floor = new Floor(floorNames, bounds, arena, size);
        } else {
            floor = arena.getFloor();

            if (floor.getPatternNames() != null) {
                floorNames = floor.getPatternNames();
            }
        }

        floor.setSize(size);
        arena.setFloor(floor);

        arena.getArenaDataManager().getConfig().set("Configuration.Floor.EnabledFloors", floorNames);
        arena.getArenaDataManager().getConfig().set("Configuration.Floor.Width", arena.getFloor().getSize().getWidth());
        arena.getArenaDataManager().getConfig().set("Configuration.Floor.Length", arena.getFloor().getSize().getLength());

        arena.getArenaDataManager().saveLocation("Floor.A", bounds[0]);
        arena.getArenaDataManager().saveLocation("Floor.B", bounds[1]);

        try {
            arena.getArenaDataManager().save();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static Floor getFromConfig(Arena arena) {
        Location a = arena.getArenaDataManager().getLocation("Floor.A");
        Location b = arena.getArenaDataManager().getLocation("Floor.B");
        Location[] bounds = new Location[]{a, b};
        List<String> enabledFloors = arena.getArenaDataManager().getConfig().getStringList("Configuration.Floor.EnabledFloors");
        int width = arena.getArenaDataManager().getConfig().getInt("Configuration.Floor.Width");
        int length = arena.getArenaDataManager().getConfig().getInt("Configuration.Floor.Length");

        return new Floor(enabledFloors, bounds, arena, new Size(width, 1, length));
    }

    public void placeFloor() {
        if (arena.isUsePatternFloors()) {

            if (this.arena.isUseAutoGeneratedFloors()) {
                int index = random.nextInt(floorPatterns.size() + generators.size());

                if (index < floorPatterns.size()) {
                    floorPatterns.get(index).place(Util.getMinBlockPos(bounds[0], bounds[1]));
                } else {
                    generators.get(index - floorPatterns.size()).generateFloor(this);
                }
            } else {
                floorPatterns.get(random.nextInt(floorPatterns.size())).place(Util.getMinBlockPos(bounds[0], bounds[1]));
            }

        } else {
            if (!this.arena.isUseAutoGeneratedFloors()) {
                Bukkit.getLogger().severe("[BlockParty] UsePatternFloors and UseAutoGeneratedFloors disabled. Using auto generated floors now!");
            }

            generateFloor();
        }
    }

    public void setStartFloor() {
        if (this.arena.isUsePatternFloors()) {

            if (floorPatterns.isEmpty()) {
                generateFloor();
                return;
            }

            for (FloorPattern floorPattern : this.getFloorPatterns()) {
                if (floorPattern.getName().equalsIgnoreCase("start")) {
                    floorPattern.place(Util.getMinBlockPos(bounds[0], bounds[1]));
                    return;
                }
            }
            floorPatterns.get(random.nextInt(floorPatterns.size())).place(Util.getMinBlockPos(bounds[0], bounds[1]));
        } else {
            generateFloor();
        }
    }

    public void setEndFloor() {
        if (this.arena.isUsePatternFloors()) {

            if (floorPatterns.isEmpty()) {
                generateFloor();
                return;
            }

            for (FloorPattern floorPattern : this.getFloorPatterns()) {
                if (floorPattern.getName().equalsIgnoreCase("end")) {
                    floorPattern.place(Util.getMinBlockPos(bounds[0], bounds[1]));
                    return;
                }
            }
            floorPatterns.get(random.nextInt(floorPatterns.size())).place(Util.getMinBlockPos(bounds[0], bounds[1]));
        } else {
            generateFloor();
        }
    }

    private void generateFloor() {
        FloorGenerator generator = generators.get(random.nextInt(generators.size()));
        generator.generateFloor(this);
    }

    public void removeBlocks() {
        Byte data = currentBlock.getData();
        Material material = currentBlock.getType();

        for (Block block : getFloorBlocks()) {
            if (block.getData() != data || block.getType() != material) {
                block.setType(Material.AIR);
            }
        }
    }

    public List<Block> getFloorBlocks() {

        int minX = getMinX(bounds[0], bounds[1]);
        int maxX = getMaxX(bounds[0], bounds[1]);
        int minZ = getMinZ(bounds[0], bounds[1]);
        int maxZ = getMaxZ(bounds[0], bounds[1]);

        List<Block> blocks = new ArrayList<>();

        World world = bounds[0].getWorld();
        int y = bounds[0].getBlockY();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                blocks.add(world.getBlockAt(x, y, z));
            }
        }

        return blocks;
    }

    public void pickBlock() {
        currentBlock = getRandomBlock();
        updateInventories(currentBlock);
    }

    public void updateInventories(Block block) {

        ItemStack stack = new ItemStack(block.getType(), 1, block.getData());
        ItemMeta meta = stack.getItemMeta();
        String name = ColorBlock.get(block).getName();
        meta.setDisplayName("§f§l§o" + name);
        stack.setItemMeta(meta);

        for (PlayerInfo playerInfo : this.arena.getPlayersInArena()) {
            if (playerInfo.getPlayerState() == PlayerState.INGAME) {
                Player player = playerInfo.asPlayer();

                player.getInventory().setItem(4, stack);
            }
        }
    }

    public void clearInventories() {
        for (PlayerInfo playerInfo : this.arena.getPlayersInArena()) {
            if (playerInfo.getPlayerState() == PlayerState.INGAME) {
                playerInfo.asPlayer().getInventory().clear();
            }
        }
    }

    private Block getRandomBlock() {
        Block block = getRandomLocation().getBlock();
        return block.getType() == Material.AIR ? getRandomBlock() : block;
    }

    public Location getRandomLocation() {
        World world = bounds[0].getWorld();
        int maxX = getMaxX(bounds[0], bounds[1]);
        int maxZ = getMaxZ(bounds[0], bounds[1]);
        int minX = getMinX(bounds[0], bounds[1]);
        int minZ = getMinZ(bounds[0], bounds[1]);
        int x = minX + random.nextInt(maxX - minX);
        int z = minZ + random.nextInt(maxZ - minZ);

        return new Location(world, x, bounds[0].getY(), z);
    }

    public void playParticles(int amount, int offsetY, int rangeY) {
        ParticlePlayer particlePlayer = arena.getParticlePlayer();

        for (int i = 0; i < amount; i++) {
            particlePlayer.play(pickRandomLocation(offsetY, rangeY), 1);
        }
    }

    public Location pickRandomLocation(int offsetY, int rangeY) {
        int minX = getMinX(bounds[0], bounds[1]);
        int maxX = getMaxX(bounds[0], bounds[1]);
        int minY = bounds[0].getBlockY() + offsetY;
        int maxY = minY + rangeY;
        int minZ = getMinZ(bounds[0], bounds[1]);
        int maxZ = getMaxZ(bounds[0], bounds[1]);

        int x = Util.getRandomValueInBetween(minX, maxX);
        int y = Util.getRandomValueInBetween(minY, maxY);
        int z = Util.getRandomValueInBetween(minZ, maxZ);

        return new Location(bounds[0].getWorld(), x, y, z);
    }

    public int getMaxX(Location a, Location b) {
        return Math.max(a.getBlockX(), b.getBlockX());
    }

    public int getMaxZ(Location a, Location b) {
        return Math.max(a.getBlockZ(), b.getBlockZ());
    }

    public int getMinX(Location a, Location b) {
        return Math.min(a.getBlockX(), b.getBlockX());
    }

    public int getMinZ(Location a, Location b) {
        return Math.min(a.getBlockZ(), b.getBlockZ());
    }

}
