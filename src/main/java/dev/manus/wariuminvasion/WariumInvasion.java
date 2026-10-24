package dev.manus.wariuminvasion;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(WariumInvasion.MOD_ID)
public final class WariumInvasion {
    public static final String MOD_ID = "warium_invasion";
    private static final String TAG_PREFIX = "warium_invasion_v2.";
    private static final long TICKS_PER_DAY = 24_000L;
    private static final int MAJOR_ROBOTS = 30;
    private static final int MAJOR_TANKS = 10;
    private static final int MAJOR_PLANES = 5;
    private static final int MAJOR_DRONE_LIKE_FLYERS = 25;
    private static final int MAJOR_HELICOPTER_PLACEHOLDERS = 10;
    private static final String[] GROUND_ROBOTS = {"scout", "rifler", "striker", "breacher", "assassin", "reaper", "mortarer", "worker", "commander"};
    private static final String[] TANKS = {"decimator_hull", "eradicator_hull", "prototype_hull"};

    public WariumInvasion() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Settings.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0 || !Settings.ENABLED.get()) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level) || level.getDifficulty() == Difficulty.PEACEFUL) continue;
            long gameTime = level.getGameTime();
            long calendarTime = level.getDayTime();

            handlePendingBombs(player, level, gameTime);
            guideAndRemoveAircraft(player, level, gameTime);
            processInvasionQueue(player, level, gameTime);
            maybeSpawnRareRoamer(player, level, gameTime);

            long airInterval = TICKS_PER_DAY * Settings.AIR_INTERVAL_DAYS.get();
            if (dueAtCalendarTime(player, "next_air", calendarTime, airInterval)) {
                startAirRaid(player, level);
                setNext(player, "next_air", calendarTime, airInterval);
            }

            long repeatInvasionInterval = TICKS_PER_DAY * Settings.INVASION_INTERVAL_DAYS.get();
            long firstInvasionAt = TICKS_PER_DAY * Settings.FIRST_INVASION_DAYS.get();
            if (dueAtCalendarTime(player, "next_invasion", calendarTime, firstInvasionAt)) {
                startMajorInvasion(player, level, gameTime);
                setNext(player, "next_invasion", calendarTime, repeatInvasionInterval);
            }
        }
    }

    private static boolean due(ServerPlayer player, String key, long now, long firstDelay) {
        String fullKey = TAG_PREFIX + key;
        long next = player.getPersistentData().getLong(fullKey);
        if (next == 0L) {
            player.getPersistentData().putLong(fullKey, now + firstDelay);
            return false;
        }
        return now >= next;
    }

    private static boolean dueAtCalendarTime(ServerPlayer player, String key, long now, long firstAt) {
        String fullKey = TAG_PREFIX + key;
        long next = player.getPersistentData().getLong(fullKey);
        if (next == 0L) {
            player.getPersistentData().putLong(fullKey, firstAt);
            next = firstAt;
        }
        return now >= next;
    }

    private static void setNext(ServerPlayer player, String key, long now, long delay) {
        player.getPersistentData().putLong(TAG_PREFIX + key, now + delay);
    }

    private static void maybeSpawnRareRoamer(ServerPlayer player, ServerLevel level, long now) {
        String nextKey = TAG_PREFIX + "next_roamer_check";
        long nextCheck = player.getPersistentData().getLong(nextKey);
        if (nextCheck == 0L) {
            player.getPersistentData().putLong(nextKey, now + 3L * 60L * 20L);
            return;
        }
        if (now < nextCheck) return;
        player.getPersistentData().putLong(nextKey, now + 3L * 60L * 20L);
        if (level.random.nextFloat() < Settings.ROAMER_CHANCE.get().floatValue() && remainingSlots(player, level) > 0) {
            spawnHostile(level, player, randomGroundRobot(level), 80, 112, false);
        }
    }

    private static String randomGroundRobot(ServerLevel level) {
        return GROUND_ROBOTS[level.random.nextInt(GROUND_ROBOTS.length)];
    }

    private static String randomTank(ServerLevel level) {
        return TANKS[level.random.nextInt(TANKS.length)];
    }

    private static void startAirRaid(ServerPlayer player, ServerLevel level) {
        boolean spawned = spawnHostile(level, player, "raidscout", 96, 120, true);
        if (spawned && Settings.ENABLE_BOMBS.get()) scheduleBombRun(player, level, 3, 60L);
    }

    private static void startMajorInvasion(ServerPlayer player, ServerLevel level, long now) {
        var data = player.getPersistentData();
        if (data.getBoolean(TAG_PREFIX + "invasion_active")) return;
        data.putBoolean(TAG_PREFIX + "invasion_active", true);
        data.putInt(TAG_PREFIX + "invasion_robots_left", MAJOR_ROBOTS);
        data.putInt(TAG_PREFIX + "invasion_tanks_left", MAJOR_TANKS);
        data.putInt(TAG_PREFIX + "invasion_planes_left", MAJOR_PLANES);
        data.putInt(TAG_PREFIX + "invasion_drones_left", MAJOR_DRONE_LIKE_FLYERS);
        data.putInt(TAG_PREFIX + "invasion_helis_left", MAJOR_HELICOPTER_PLACEHOLDERS);
        data.putInt(TAG_PREFIX + "invasion_cursor", 0);
        data.putLong(TAG_PREFIX + "invasion_next_unit", now);
        if (Settings.ENABLE_BOMBS.get()) scheduleBombRun(player, level, 10, 60L);
    }

    private static void processInvasionQueue(ServerPlayer player, ServerLevel level, long now) {
        var data = player.getPersistentData();
        String activeKey = TAG_PREFIX + "invasion_active";
        if (!data.getBoolean(activeKey) || now < data.getLong(TAG_PREFIX + "invasion_next_unit")) return;
        for (int i = 0; i < 2; i++) {
            if (!spawnNextInvasionUnit(player, level)) break;
            if (invasionUnitsLeft(player) == 0) {
                data.putBoolean(activeKey, false);
                return;
            }
        }
        if (invasionUnitsLeft(player) > 0) data.putLong(TAG_PREFIX + "invasion_next_unit", now + 20L);
        else data.putBoolean(activeKey, false);
    }

    private static boolean spawnNextInvasionUnit(ServerPlayer player, ServerLevel level) {
        var data = player.getPersistentData();
        String[] counters = {
                "invasion_robots_left", "invasion_tanks_left", "invasion_planes_left",
                "invasion_drones_left", "invasion_helis_left"
        };
        int cursor = data.getInt(TAG_PREFIX + "invasion_cursor");
        for (int offset = 0; offset < counters.length; offset++) {
            int category = (cursor + offset) % counters.length;
            String counter = TAG_PREFIX + counters[category];
            if (data.getInt(counter) <= 0) continue;
            String id;
            boolean airborne;
            switch (category) {
                case 0 -> { id = randomGroundRobot(level); airborne = false; }
                case 1 -> { id = randomTank(level); airborne = false; }
                case 2, 4 -> { id = "raidscout"; airborne = true; }
                default -> { id = "hunter"; airborne = true; }
            }
            if (!spawnHostile(level, player, id, 88, 116, airborne)) return false;
            data.putInt(counter, data.getInt(counter) - 1);
            data.putInt(TAG_PREFIX + "invasion_cursor", (category + 1) % counters.length);
            return true;
        }
        return false;
    }

    private static int invasionUnitsLeft(ServerPlayer player) {
        var data = player.getPersistentData();
        return data.getInt(TAG_PREFIX + "invasion_robots_left")
                + data.getInt(TAG_PREFIX + "invasion_tanks_left")
                + data.getInt(TAG_PREFIX + "invasion_planes_left")
                + data.getInt(TAG_PREFIX + "invasion_drones_left")
                + data.getInt(TAG_PREFIX + "invasion_helis_left");
    }

    private static void scheduleBombRun(ServerPlayer player, ServerLevel level, int number, long firstDelay) {
        var data = player.getPersistentData();
        data.putInt(TAG_PREFIX + "bombs_left", number);
        data.putLong(TAG_PREFIX + "bomb_time", level.getGameTime() + firstDelay);
        chooseBombPoint(player, level);
    }

    private static void chooseBombPoint(ServerPlayer player, ServerLevel level) {
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double radius = 10.0 + level.random.nextDouble() * 16.0;
        player.getPersistentData().putDouble(TAG_PREFIX + "bomb_x", player.getX() + Math.cos(angle) * radius);
        player.getPersistentData().putDouble(TAG_PREFIX + "bomb_z", player.getZ() + Math.sin(angle) * radius);
    }

    private static void handlePendingBombs(ServerPlayer player, ServerLevel level, long now) {
        var data = player.getPersistentData();
        long bombTime = data.getLong(TAG_PREFIX + "bomb_time");
        int bombsLeft = data.getInt(TAG_PREFIX + "bombs_left");
        if (bombsLeft <= 0 || bombTime == 0 || now < bombTime) return;
        double x = data.getDouble(TAG_PREFIX + "bomb_x");
        double z = data.getDouble(TAG_PREFIX + "bomb_z");
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        Level.ExplosionInteraction interaction = Settings.BOMBS_BREAK_BLOCKS.get()
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level.explode(null, x, y, z, Settings.BOMB_STRENGTH.get().floatValue(), interaction);
        bombsLeft--;
        data.putInt(TAG_PREFIX + "bombs_left", bombsLeft);
        if (bombsLeft > 0) {
            data.putLong(TAG_PREFIX + "bomb_time", now + 20L * (7L + level.random.nextInt(5)));
            chooseBombPoint(player, level);
        } else {
            data.putLong(TAG_PREFIX + "bomb_time", 0L);
        }
    }

    private static void guideAndRemoveAircraft(ServerPlayer player, ServerLevel level, long now) {
        AABB area = player.getBoundingBox().inflate(220.0);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                candidate -> candidate.getPersistentData().getBoolean(TAG_PREFIX + "aircraft"))) {
            var data = mob.getPersistentData();
            long spawnedAt = data.getLong(TAG_PREFIX + "aircraft_spawned_at");
            long age = now - spawnedAt;
            if (age >= Settings.AIRCRAFT_STAY_SECONDS.get() * 20L) {
                mob.discard();
                continue;
            }
            double phase = age * 0.004 + data.getDouble(TAG_PREFIX + "orbit_phase");
            double radius = data.getDouble(TAG_PREFIX + "orbit_radius");
            double x = player.getX() + Math.cos(phase) * radius;
            double z = player.getZ() + Math.sin(phase) * radius;
            int terrain = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
            double y = Math.min(level.getMaxBuildHeight() - 10.0, Math.max(player.getY() + 34.0, terrain + 36.0));
            mob.setNoGravity(true);
            mob.getMoveControl().setWantedPosition(x, y, z, 1.0);
        }
    }

    private static int remainingSlots(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(220.0);
        int active = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob.getPersistentData().getBoolean(TAG_PREFIX + "attacker")).size();
        return Math.max(0, Settings.MAX_ATTACKERS.get() - active);
    }

    private static boolean spawnHostile(ServerLevel level, ServerPlayer target, String path, int minDistance, int maxDistance, boolean airborne) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("crusty_chunks", path));
        if (type == null || remainingSlots(target, level) <= 0) return false;
        BlockPos pos = findSpawnPosition(level, target, minDistance, maxDistance, airborne);
        if (pos == null) return false;
        Entity entity = type.create(level);
        if (entity == null) return false;
        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        entity.getPersistentData().putBoolean(TAG_PREFIX + "attacker", true);
        if (airborne) {
            entity.getPersistentData().putBoolean(TAG_PREFIX + "aircraft", true);
            entity.getPersistentData().putLong(TAG_PREFIX + "aircraft_spawned_at", level.getGameTime());
            entity.getPersistentData().putDouble(TAG_PREFIX + "orbit_phase", level.random.nextDouble() * Math.PI * 2.0);
            entity.getPersistentData().putDouble(TAG_PREFIX + "orbit_radius", 34.0 + level.random.nextDouble() * 18.0);
            entity.setNoGravity(true);
        }
        if (!level.addFreshEntity(entity)) return false;
        if (entity instanceof Mob mob) {
            mob.setTarget(target);
            mob.setAggressive(true);
        }
        return true;
    }

    private static BlockPos findSpawnPosition(ServerLevel level, ServerPlayer target, int minDistance, int maxDistance, boolean airborne) {
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int distance = minDistance + level.random.nextInt(Math.max(1, maxDistance - minDistance + 1));
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            int terrainY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int y = airborne ? Math.max(terrainY + 48, (int) target.getY() + 42) : terrainY;
            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 8) continue;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(pos)) continue;
            if (airborne) return pos;
            BlockPos floor = pos.below();
            if (!level.getBlockState(floor).isSolidRender(level, floor) || !level.getFluidState(floor).isEmpty()) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.above(2)).isAir()) continue;
            if (pos.closerToCenterThan(target.position(), minDistance - 4.0)) continue;
            return pos;
        }
        return null;
    }

    private static final class Settings {
        private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        private static final ForgeConfigSpec.BooleanValue ENABLED;
        private static final ForgeConfigSpec.BooleanValue ENABLE_BOMBS;
        private static final ForgeConfigSpec.BooleanValue BOMBS_BREAK_BLOCKS;
        private static final ForgeConfigSpec.IntValue AIR_INTERVAL_DAYS;
        private static final ForgeConfigSpec.IntValue FIRST_INVASION_DAYS;
        private static final ForgeConfigSpec.IntValue INVASION_INTERVAL_DAYS;
        private static final ForgeConfigSpec.IntValue AIRCRAFT_STAY_SECONDS;
        private static final ForgeConfigSpec.IntValue MAX_ATTACKERS;
        private static final ForgeConfigSpec.DoubleValue ROAMER_CHANCE;
        private static final ForgeConfigSpec.DoubleValue BOMB_STRENGTH;
        private static final ForgeConfigSpec SPEC;

        static {
            BUILDER.push("invasions");
            ENABLED = BUILDER.define("enabled", true);
            ENABLE_BOMBS = BUILDER.define("enableBombs", true);
            BOMBS_BREAK_BLOCKS = BUILDER.define("bombsBreakBlocks", false);
            AIR_INTERVAL_DAYS = BUILDER.defineInRange("airRaidIntervalDays", 20, 1, 365);
            FIRST_INVASION_DAYS = BUILDER.defineInRange("firstMajorInvasionDays", 150, 1, 1000);
            INVASION_INTERVAL_DAYS = BUILDER.defineInRange("majorInvasionIntervalDays", 30, 1, 365);
            AIRCRAFT_STAY_SECONDS = BUILDER.defineInRange("aircraftStaySeconds", 110, 30, 600);
            MAX_ATTACKERS = BUILDER.defineInRange("maxActiveAttackersNearPlayer", 100, 1, 160);
            ROAMER_CHANCE = BUILDER.defineInRange("rareRoamerChancePerCheck", 0.08, 0.0, 1.0);
            BOMB_STRENGTH = BUILDER.defineInRange("bombStrength", 3.0, 1.0, 8.0);
            BUILDER.pop();
            SPEC = BUILDER.build();
        }
    }
}
