package awa.Aether_254.create_wrench_guard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;

public final class WrenchGuardConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("create_wrench_guard.json");
    private static Data data = defaults();

    private WrenchGuardConfig() {
    }

    public static Data get() {
        return data;
    }

    public static void load() {
        try {
            if (Files.isRegularFile(PATH)) {
                Data loaded = GSON.fromJson(Files.readString(PATH), Data.class);
                data = loaded == null ? defaults() : loaded;
            }
        } catch (IOException | RuntimeException ignored) {
            data = defaults();
        }
        save();
    }

    public static void save() {
        if (data.protectedBlocks == null)
            data.protectedBlocks = new ArrayList<>();
        if (data.whitelist == null)
            data.whitelist = new ArrayList<>();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(data));
        } catch (IOException ignored) {
        }
    }

    public static Mode modeFor(BlockState state) {
        if (!data.enabled)
            return Mode.ALLOW;
        if (data.whitelist.stream().anyMatch(entry -> matches(state, entry)))
            return Mode.ALLOW;
        for (Rule rule : data.protectedBlocks) {
            if (rule != null && matches(state, rule.block))
                return Mode.parse(rule.doBreak);
        }
        return Mode.ALLOW;
    }

    public static List<String> rulesAsStrings() {
        return data.protectedBlocks.stream()
            .map(rule -> rule.block + "=" + rule.doBreak)
            .toList();
    }

    public static void setRulesFromStrings(List<String> values) {
        data.protectedBlocks = new ArrayList<>();
        for (String value : values) {
            String[] parts = value.split("=", 2);
            if (parts.length == 2 && !parts[0].isBlank())
                data.protectedBlocks.add(new Rule(parts[0].trim(), Mode.parse(parts[1]).name().toLowerCase(Locale.ROOT)));
        }
    }

    private static boolean matches(BlockState state, String entry) {
        if (entry == null || entry.isBlank())
            return false;
        if (entry.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(1));
            return id != null && state.is(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, id));
        }
        ResourceLocation id = ResourceLocation.tryParse(entry);
        return id != null && BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(id);
    }

    private static Data defaults() {
        Data result = new Data();
        for (String block : List.of(
            "create:andesite_bars", "create:brass_bars", "create:copper_bars",
            "create:industrial_iron_block", "create:weathered_iron_block",
            "minecraft:redstone_wire", "minecraft:redstone_torch", "minecraft:repeater",
            "minecraft:lever", "minecraft:redstone_lamp", "minecraft:comparator",
            "minecraft:observer", "minecraft:redstone_wall_torch", "minecraft:piston",
            "minecraft:sticky_piston", "minecraft:tripwire", "minecraft:tripwire_hook",
            "minecraft:daylight_detector", "minecraft:target", "minecraft:hopper",
            "#minecraft:buttons", "#minecraft:pressure_plates", "#minecraft:rails"
        ))
            result.protectedBlocks.add(new Rule(block, "allow"));
        return result;
    }

    public enum Mode {
        ALLOW, WARN, DENY;

        static Mode parse(String value) {
            try {
                return value == null ? ALLOW : valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return ALLOW;
            }
        }
    }

    public static final class Rule {
        public String block;
        @SerializedName("do_break")
        public String doBreak;

        public Rule(String block, String doBreak) {
            this.block = block;
            this.doBreak = doBreak;
        }
    }

    public static final class Data {
        public boolean enabled = true;
        @SerializedName("prevent_when_inventory_full")
        public boolean preventWhenInventoryFull = true;
        @SerializedName("protected")
        public List<Rule> protectedBlocks = new ArrayList<>();
        public List<String> whitelist = new ArrayList<>();
    }
}
