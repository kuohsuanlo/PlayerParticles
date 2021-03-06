package dev.esophose.playerparticles.particles;

import com.google.common.collect.ObjectArrays;
import dev.esophose.playerparticles.PlayerParticles;
import dev.esophose.playerparticles.config.CommentedFileConfiguration;
import dev.esophose.playerparticles.manager.ConfigurationManager.Setting;
import dev.esophose.playerparticles.manager.ParticleManager;
import dev.esophose.playerparticles.util.NMSUtil;
import dev.esophose.playerparticles.util.ParticleUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;

@SuppressWarnings("deprecation")
public enum ParticleEffect {

    // Ordered and named by their Minecraft 1.13+ internal names
    AMBIENT_ENTITY_EFFECT("SPELL_MOB_AMBIENT", ParticleProperty.COLORABLE),
    ANGRY_VILLAGER("VILLAGER_ANGRY"),
    BARRIER("BARRIER"),
    BLOCK("BLOCK_CRACK", ParticleProperty.REQUIRES_MATERIAL_DATA),
    BUBBLE("WATER_BUBBLE"),
    BUBBLE_COLUMN_UP("BUBBLE_COLUMN_UP"),
    BUBBLE_POP("BUBBLE_POP"),
    CAMPFIRE_COSY_SMOKE("CAMPFIRE_COSY_SMOKE"),
    CAMPFIRE_SIGNAL_SMOKE("CAMPFIRE_SIGNAL_SMOKE"),
    CLOUD("CLOUD"),
    COMPOSTER("COMPOSTER"),
    CRIT("CRIT"),
    CURRENT_DOWN("CURRENT_DOWN"),
    DAMAGE_INDICATOR("DAMAGE_INDICATOR"),
    DOLPHIN("DOLPHIN"),
    DRAGON_BREATH("DRAGON_BREATH"),
    DRIPPING_HONEY("DRIPPING_HONEY"),
    DRIPPING_LAVA("DRIP_LAVA"),
    DRIPPING_WATER("DRIP_WATER"),
    DUST("REDSTONE", ParticleProperty.COLORABLE),
    ELDER_GUARDIAN("MOB_APPEARANCE", false), // No thank you
    ENCHANT("ENCHANTMENT_TABLE"),
    ENCHANTED_HIT("CRIT_MAGIC"),
    END_ROD("END_ROD"),
    ENTITY_EFFECT("SPELL_MOB", ParticleProperty.COLORABLE),
    EXPLOSION("EXPLOSION_LARGE"),
    EXPLOSION_EMITTER("EXPLOSION_HUGE"),
    FALLING_DUST("FALLING_DUST", ParticleProperty.REQUIRES_MATERIAL_DATA),
    FALLING_HONEY("FALLING_HONEY"),
    FALLING_LAVA("FALLING_LAVA"),
    FALLING_NECTAR("FALLING_NECTAR"),
    FALLING_WATER("FALLING_WATER"),
    FIREWORK("FIREWORKS_SPARK"),
    FISHING("WATER_WAKE"),
    FLAME("FLAME"),
    FLASH("FLASH", false), // Also no thank you
    FOOTSTEP("FOOTSTEP"), // Removed in Minecraft 1.13 :(
    HAPPY_VILLAGER("VILLAGER_HAPPY"),
    HEART("HEART"),
    INSTANT_EFFECT("SPELL_INSTANT"),
    ITEM("ITEM_CRACK", ParticleProperty.REQUIRES_MATERIAL_DATA),
    ITEM_SLIME("SLIME"),
    ITEM_SNOWBALL("SNOWBALL"),
    LANDING_HONEY("LANDING_HONEY"),
    LANDING_LAVA("LANDING_LAVA"),
    LARGE_SMOKE("SMOKE_LARGE"),
    LAVA("LAVA"),
    MYCELIUM("TOWN_AURA"),
    NAUTILUS("NAUTILUS"),
    NOTE("NOTE", ParticleProperty.COLORABLE),
    POOF("EXPLOSION_NORMAL"), // The 1.13 combination of explode and showshovel
    PORTAL("PORTAL"),
    RAIN("WATER_DROP"),
    SMOKE("SMOKE_NORMAL"),
    SNEEZE("SNEEZE"),
    SPELL("SPELL"), // The Minecraft internal name for this is actually "effect", but that's the command name, so it's SPELL for the plugin instead
    SPIT("SPIT"),
    SPLASH("WATER_SPLASH"),
    SQUID_INK("SQUID_INK"),
    SWEEP_ATTACK("SWEEP_ATTACK"),
    TOTEM_OF_UNDYING("TOTEM"),
    UNDERWATER("SUSPENDED_DEPTH"),
    WITCH("SPELL_WITCH");

    private Particle internalEnum;
    private List<ParticleProperty> properties;

    private CommentedFileConfiguration config;
    private boolean enabledByDefault;

    private String effectName;
    private boolean enabled;

    /**
     * Construct a new particle effect
     * 
     * @param enumName Name of the Particle Enum when the server version is greater than or equal to 1.13
     * @param properties Properties of this particle effect
     */
    ParticleEffect(String enumName, ParticleProperty... properties) {
        this(enumName, true, properties);
    }

    /**
     * Construct a new particle effect
     *
     * @param enumName Name of the Particle Enum when the server version is greater than or equal to 1.13
     * @param enabledByDefault If the particle type is enabled by default
     * @param properties Properties of this particle effect
     */
    ParticleEffect(String enumName, boolean enabledByDefault, ParticleProperty... properties) {
        this.enabledByDefault = enabledByDefault;
        this.properties = Arrays.asList(properties);

        // Will be null if this server's version doesn't support this particle type
        this.internalEnum = Stream.of(Particle.values()).filter(x -> x.name().equals(enumName)).findFirst().orElse(null);

        this.setDefaultSettings();
        this.loadSettings(false);
    }

    /**
     * Sets the default settings for the particle type
     */
    private void setDefaultSettings() {
        if (!this.isSupported())
            return;

        File directory = new File(PlayerParticles.getInstance().getDataFolder(), "effects");
        directory.mkdirs();

        File file = new File(directory, this.getInternalName() + ".yml");
        this.config = CommentedFileConfiguration.loadConfiguration(PlayerParticles.getInstance(), file);

        boolean changed = this.setIfNotExists("effect-name", this.getInternalName(), "The name the effect will display as");
        changed |= this.setIfNotExists("enabled", this.enabledByDefault, "If the effect is enabled or not");

        if (changed)
            this.config.save();
    }

    /**
     * Loads the settings shared for each style then calls loadSettings(CommentedFileConfiguration)
     *
     * @param reloadConfig If the settings should be reloaded or not
     */
    public void loadSettings(boolean reloadConfig) {
        if (!this.isSupported())
            return;

        if (reloadConfig)
            this.setDefaultSettings();

        this.effectName = this.config.getString("effect-name");
        this.enabled = this.config.getBoolean("enabled");
    }

    /**
     * Sets a value to the config if it doesn't already exist
     *
     * @param setting The setting name
     * @param value The setting value
     * @param comments Comments for the setting
     * @return true if changes were made
     */
    private boolean setIfNotExists(String setting, Object value, String... comments) {
        if (this.config.get(setting) != null)
            return false;

        String defaultMessage = "Default: ";
        if (value instanceof String && ParticleUtils.containsConfigSpecialCharacters((String) value)) {
            defaultMessage += "'" + value + "'";
        } else {
            defaultMessage += value;
        }

        this.config.set(setting, value, ObjectArrays.concat(comments, new String[] { defaultMessage }, String.class));
        return true;
    }

    /**
     * Reloads the settings for all ParticleEffects
     */
    public static void reloadSettings() {
        for (ParticleEffect effect : values())
            effect.loadSettings(true);
    }

    /**
     * @return the internal name of this particle effect that will never change
     */
    public String getInternalName() {
        return this.name().toLowerCase();
    }

    /**
     * @return the name that the style will display to the users as
     */
    public String getName() {
        return this.effectName;
    }

    /**
     * Determine if this particle effect has a specific property
     * 
     * @param property The property to check
     * @return Whether it has the property or not
     */
    public boolean hasProperty(ParticleProperty property) {
        return this.properties.contains(property);
    }

    /**
     * Determine if this particle effect is supported by the current server version
     * 
     * @return Whether the particle effect is supported or not
     */
    public boolean isSupported() {
        return this.internalEnum != null;
    }

    /**
     * @return true if this effect is enabled, otherwise false
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Returns a ParticleEffect List of all enabled effects for the server version
     * 
     * @return Enabled effects
     */
    public static List<ParticleEffect> getEnabledEffects() {
        List<ParticleEffect> effects = new ArrayList<>();
        for (ParticleEffect pe : values())
            if (pe.isSupported() && pe.isEnabled())
                effects.add(pe);
        return effects;
    }

    /**
     * Returns the particle effect with the given name
     * 
     * @param name Name of the particle effect
     * @return The particle effect
     */
    public static ParticleEffect fromName(String name) {
        return Stream.of(values()).filter(x -> x.isSupported() && x.isEnabled() && x.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * Returns the particle effect with the given name
     *
     * @param internalName Internal name of the particle effect
     * @return The particle effect
     */
    public static ParticleEffect fromInternalName(String internalName) {
        return Stream.of(values()).filter(x -> x.isSupported() && x.isEnabled() && x.getInternalName().equalsIgnoreCase(internalName)).findFirst().orElse(null);
    }
    
    /**
     * Invokes the correct spawn method for the particle information given
     * 
     * @param particle The ParticlePair, given the effect/style/data
     * @param pparticle The particle spawn information
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     */
    public static void display(ParticlePair particle, PParticle pparticle, boolean isLongRange, Player owner) {
        ParticleEffect effect = particle.getEffect();
        int count = pparticle.isDirectional() ? 0 : 1;
        
        if (effect.hasProperty(ParticleProperty.REQUIRES_MATERIAL_DATA)) {
            effect.display(particle.getSpawnMaterial(), pparticle.getXOff(), pparticle.getYOff(), pparticle.getZOff(), pparticle.getSpeed(), 1, pparticle.getLocation(false), isLongRange, owner);
        } else if (effect.hasProperty(ParticleProperty.COLORABLE)) {
            effect.display(particle.getSpawnColor(), pparticle.getLocation(true), isLongRange, owner);
        } else {
            effect.display(pparticle.getXOff(), pparticle.getYOff(), pparticle.getZOff(), pparticle.getSpeed(), count, pparticle.getLocation(false), isLongRange, owner);
        }
    }

    /**
     * Displays a particle effect
     * 
     * @param offsetX Maximum distance particles can fly away from the center on the x-axis
     * @param offsetY Maximum distance particles can fly away from the center on the y-axis
     * @param offsetZ Maximum distance particles can fly away from the center on the z-axis
     * @param speed Display speed of the particles
     * @param amount Amount of particles
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @throws ParticleDataException If the particle effect requires additional data
     */
    public void display(double offsetX, double offsetY, double offsetZ, double speed, int amount, Location center, boolean isLongRange, Player owner) throws ParticleDataException {
        if (this.hasProperty(ParticleProperty.REQUIRES_MATERIAL_DATA))
            throw new ParticleDataException("This particle effect requires additional data");

        for (Player player : this.getPlayersInRange(center, isLongRange, owner))
            player.spawnParticle(this.internalEnum, center.getX(), center.getY(), center.getZ(), amount, offsetX, offsetY, offsetZ, speed);
    }

    /**
     * Displays a single particle which is colored
     * 
     * @param color Color of the particle
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @throws ParticleColorException If the particle effect is not colorable or the color type is incorrect
     */
    public void display(ParticleColor color, Location center, boolean isLongRange, Player owner) throws ParticleColorException {
        if (!this.hasProperty(ParticleProperty.COLORABLE))
            throw new ParticleColorException("This particle effect is not colorable");

        if (this == DUST && NMSUtil.getVersionNumber() >= 13) { // DUST uses a special data object for spawning in 1.13+
            OrdinaryColor dustColor = (OrdinaryColor) color;
            DustOptions dustOptions = new DustOptions(Color.fromRGB(dustColor.getRed(), dustColor.getGreen(), dustColor.getBlue()), Setting.DUST_SIZE.getFloat());
            for (Player player : this.getPlayersInRange(center, isLongRange, owner))
                player.spawnParticle(this.internalEnum, center.getX(), center.getY(), center.getZ(), 1, 0, 0, 0, 0, dustOptions);
        } else {
            for (Player player : this.getPlayersInRange(center, isLongRange, owner)) {
                // Minecraft clients require that you pass a non-zero value if the Red value should be zero
                player.spawnParticle(this.internalEnum, center.getX(), center.getY(), center.getZ(), 0, this == ParticleEffect.DUST && color.getValueX() == 0 ? Float.MIN_VALUE : color.getValueX(), color.getValueY(), color.getValueZ(), 1);
            }
        }
    }

    /**
     * Displays a particle effect which requires additional data and is only
     * visible for all players within a certain range in the world of @param
     * center
     * 
     * @param spawnMaterial Material of the effect
     * @param offsetX Maximum distance particles can fly away from the center on the x-axis
     * @param offsetY Maximum distance particles can fly away from the center on the y-axis
     * @param offsetZ Maximum distance particles can fly away from the center on the z-axis
     * @param speed Display speed of the particles
     * @param amount Amount of particles
     * @param center Center location of the effect
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @throws ParticleDataException If the particle effect does not require additional data or if the data type is incorrect
     */
    public void display(Material spawnMaterial, double offsetX, double offsetY, double offsetZ, double speed, int amount, Location center, boolean isLongRange, Player owner) throws ParticleDataException {
        if (!this.hasProperty(ParticleProperty.REQUIRES_MATERIAL_DATA)) {
            throw new ParticleDataException("This particle effect does not require additional data");
        }

        Object extraData = null;
        if (this.internalEnum.getDataType().getTypeName().equals("org.bukkit.block.data.BlockData")) {
            extraData = spawnMaterial.createBlockData();
        } else if (this.internalEnum.getDataType() == ItemStack.class) {
            extraData = new ItemStack(spawnMaterial);
        } else if (this.internalEnum.getDataType() == MaterialData.class) {
            extraData = new MaterialData(spawnMaterial); // Deprecated, only used in versions < 1.13
        }

        for (Player player : this.getPlayersInRange(center, isLongRange, owner))
            player.spawnParticle(this.internalEnum, center.getX(), center.getY(), center.getZ(), amount, offsetX, offsetY, offsetZ, speed, extraData);
    }

    /**
     * Gets a List of Players within the particle display range
     * 
     * @param center The center of the radius to check around
     * @param isLongRange If the particle can be viewed from long range
     * @param owner The player that owns the particles
     * @return A List of Players within the particle display range
     */
    private List<Player> getPlayersInRange(Location center, boolean isLongRange, Player owner) {
        List<Player> players = new ArrayList<>();
        int range = !isLongRange ? Setting.PARTICLE_RENDER_RANGE_PLAYER.getInt() : Setting.PARTICLE_RENDER_RANGE_FIXED_EFFECT.getInt();
        range *= range;

        for (PPlayer pplayer : PlayerParticles.getInstance().getManager(ParticleManager.class).getPPlayers()) {
            Player p = pplayer.getPlayer();
            if (!this.canSee(p, owner))
                continue;

            if (p != null && pplayer.canSeeParticles() && p.getWorld().equals(center.getWorld()) && center.distanceSquared(p.getLocation()) <= range)
                players.add(p);
        }

        return players;
    }

    /**
     * Checks if a player can see another player
     *
     * @param player The player
     * @param target The target
     * @return True if player can see target, otherwise false
     */
    private boolean canSee(Player player, Player target) {
        if (player == null || target == null)
            return true;

        for (MetadataValue meta : target.getMetadata("vanished"))
            if (meta.asBoolean())
                return false;

        return player.canSee(target);
    }

    /**
     * Represents the property of a particle effect
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     * 
     * @author DarkBlade12
     * @since 1.7
     */
    public enum ParticleProperty {
        /**
         * The particle effect requires block or item material data to be displayed
         */
        REQUIRES_MATERIAL_DATA,
        /**
         * The particle effect uses the offsets as color values
         */
        COLORABLE
    }

    /**
     * Represents the color for effects like {@link ParticleEffect#ENTITY_EFFECT},
     * {@link ParticleEffect#AMBIENT_ENTITY_EFFECT}, {@link ParticleEffect#DUST}
     * and {@link ParticleEffect#NOTE}
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     * 
     * @author DarkBlade12
     * @since 1.7
     */
    public static abstract class ParticleColor {
        /**
         * Returns the value for the offsetX field
         * 
         * @return The offsetX value
         */
        public abstract float getValueX();

        /**
         * Returns the value for the offsetY field
         * 
         * @return The offsetY value
         */
        public abstract float getValueY();

        /**
         * Returns the value for the offsetZ field
         * 
         * @return The offsetZ value
         */
        public abstract float getValueZ();
    }

    /**
     * Represents the color for effects like {@link ParticleEffect#ENTITY_EFFECT},
     * {@link ParticleEffect#AMBIENT_ENTITY_EFFECT} and {@link ParticleEffect#NOTE}
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     * 
     * @author DarkBlade12
     * @since 1.7
     */
    public static final class OrdinaryColor extends ParticleColor {
        public static final OrdinaryColor RAINBOW = new OrdinaryColor(999, 999, 999);
        public static final OrdinaryColor RANDOM = new OrdinaryColor(998, 998, 998);

        private final int red;
        private final int green;
        private final int blue;

        /**
         * Construct a new ordinary color
         * 
         * @param red Red value of the RGB format
         * @param green Green value of the RGB format
         * @param blue Blue value of the RGB format
         * @throws IllegalArgumentException If one of the values is lower than 0
         *             or higher than 255
         */
        public OrdinaryColor(int red, int green, int blue) throws IllegalArgumentException {
            if ((red == 999 && green == 999 && blue == 999) || (red == 998 && green == 998 && blue == 998)) { // Allow rainbow and random values
                this.red = red;
                this.green = green;
                this.blue = blue;
            } else {
                if (red < 0) {
                    throw new IllegalArgumentException("The red value is lower than 0");
                }
                if (red > 255) {
                    throw new IllegalArgumentException("The red value is higher than 255");
                }
                this.red = red;
                if (green < 0) {
                    throw new IllegalArgumentException("The green value is lower than 0");
                }
                if (green > 255) {
                    throw new IllegalArgumentException("The green value is higher than 255");
                }
                this.green = green;
                if (blue < 0) {
                    throw new IllegalArgumentException("The blue value is lower than 0");
                }
                if (blue > 255) {
                    throw new IllegalArgumentException("The blue value is higher than 255");
                }
                this.blue = blue;
            }
        }

        /**
         * Returns the red value of the RGB format
         * 
         * @return The red value
         */
        public int getRed() {
            return this.red;
        }

        /**
         * Returns the green value of the RGB format
         * 
         * @return The green value
         */
        public int getGreen() {
            return this.green;
        }

        /**
         * Returns the blue value of the RGB format
         * 
         * @return The blue value
         */
        public int getBlue() {
            return this.blue;
        }

        /**
         * Returns the red value divided by 255
         * 
         * @return The offsetX value
         */
        @Override
        public float getValueX() {
            if (this.equals(OrdinaryColor.RAINBOW) || this.equals(OrdinaryColor.RANDOM))
                return 0F;
            return (float) this.red / 255F;
        }

        /**
         * Returns the green value divided by 255
         * 
         * @return The offsetY value
         */
        @Override
        public float getValueY() {
            if (this.equals(OrdinaryColor.RAINBOW) || this.equals(OrdinaryColor.RANDOM))
                return 0F;
            return (float) this.green / 255F;
        }

        /**
         * Returns the blue value divided by 255
         * 
         * @return The offsetZ value
         */
        @Override
        public float getValueZ() {
            if (this.equals(OrdinaryColor.RAINBOW) || this.equals(OrdinaryColor.RANDOM))
                return 0F;
            return (float) this.blue / 255F;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof OrdinaryColor))
                return false;
            OrdinaryColor otherColor = (OrdinaryColor) other;
            return this.red == otherColor.red && this.green == otherColor.green && this.blue == otherColor.blue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.red, this.green, this.blue);
        }
    }

    /**
     * Represents the color for the {@link ParticleEffect#NOTE} effect
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     * 
     * @author DarkBlade12
     * @since 1.7
     */
    public static final class NoteColor extends ParticleColor {
        public static final NoteColor RAINBOW = new NoteColor(99);
        public static final NoteColor RANDOM = new NoteColor(98);

        private final int note;

        /**
         * Construct a new note color
         * 
         * @param note Note id which determines color
         * @throws IllegalArgumentException If the note value is lower than 0 or
         *             higher than 24
         */
        public NoteColor(int note) throws IllegalArgumentException {
            if (note == 99 || note == 98) { // Allow rainbow and random values
                this.note = note;
            } else {
                if (note < 0) {
                    throw new IllegalArgumentException("The note value is lower than 0");
                }
                if (note > 24) {
                    throw new IllegalArgumentException("The note value is higher than 24");
                }
                this.note = note;
            }
        }

        /**
         * Returns the note value
         * 
         * @return The note value
         */
        public int getNote() {
            return this.note;
        }

        /**
         * Returns the note value divided by 24
         * 
         * @return The offsetX value
         */
        @Override
        public float getValueX() {
            return (float) this.note / 24F;
        }

        /**
         * Returns zero because the offsetY value is unused
         * 
         * @return zero
         */
        @Override
        public float getValueY() {
            return 0;
        }

        /**
         * Returns zero because the offsetZ value is unused
         * 
         * @return zero
         */
        @Override
        public float getValueZ() {
            return 0;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof NoteColor))
                return false;
            NoteColor otherColor = (NoteColor) other;
            return this.note == otherColor.note;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.note);
        }

    }

    /**
     * Represents a runtime exception that is thrown either if the displayed
     * particle effect requires data and has none or vice-versa or if the data
     * type is incorrect
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     * 
     * @author DarkBlade12
     * @since 1.6
     */
    private static final class ParticleDataException extends RuntimeException {
        private static final long serialVersionUID = 3203085387160737484L;

        /**
         * Construct a new particle data exception
         * 
         * @param message Message that will be logged
         */
        public ParticleDataException(String message) {
            super(message);
        }
    }

    /**
     * Represents a runtime exception that is thrown either if the displayed
     * particle effect is not colorable or if the particle color type is
     * incorrect
     * <p>
     * This class is part of the <b>ParticleEffect Library</b> and follows the
     * same usage conditions
     * 
     * @author DarkBlade12
     * @since 1.7
     */
    private static final class ParticleColorException extends RuntimeException {
        private static final long serialVersionUID = 3203085387160737485L;

        /**
         * Construct a new particle color exception
         * 
         * @param message Message that will be logged
         */
        public ParticleColorException(String message) {
            super(message);
        }
    }

}
