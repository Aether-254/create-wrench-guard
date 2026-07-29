package awa.Aether_254.create_wrench_guard.client;

import awa.Aether_254.create_wrench_guard.WrenchGuardConfig;
import java.util.ArrayList;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class WrenchGuardConfigScreen {
    private WrenchGuardConfigScreen() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> create(parent));
    }

    private static Screen create(Screen parent) {
        WrenchGuardConfig.Data config = WrenchGuardConfig.get();
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
            .setTitle(Component.literal("Create: Wrench Guard"));
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("Removal protection"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        category.addEntry(entries.startBooleanToggle(Component.literal("Enable guard"), config.enabled)
            .setDefaultValue(true).setSaveConsumer(value -> config.enabled = value).build());
        category.addEntry(entries.startBooleanToggle(Component.literal("Prevent removal when inventory is full"),
                config.preventWhenInventoryFull)
            .setDefaultValue(true).setSaveConsumer(value -> config.preventWhenInventoryFull = value).build());
        category.addEntry(entries.startStrList(Component.literal("Protected rules (block=allow|warn|deny)"),
                new ArrayList<>(WrenchGuardConfig.rulesAsStrings()))
            .setSaveConsumer(WrenchGuardConfig::setRulesFromStrings).build());
        category.addEntry(entries.startStrList(Component.literal("Whitelist blocks or #tags"),
                new ArrayList<>(config.whitelist))
            .setSaveConsumer(value -> config.whitelist = new ArrayList<>(value)).build());
        builder.setSavingRunnable(WrenchGuardConfig::save);
        return builder.build();
    }
}
