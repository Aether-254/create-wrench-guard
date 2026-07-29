package awa.Aether_254.create_wrench_guard;

import awa.Aether_254.create_wrench_guard.client.WrenchGuardConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CreateWrenchGuard.MOD_ID)
public final class CreateWrenchGuard {
    public static final String MOD_ID = "create_wrench_guard";

    public CreateWrenchGuard(ModContainer container) {
        WrenchGuardConfig.load();
        if (FMLEnvironment.dist == Dist.CLIENT)
            WrenchGuardConfigScreen.register(container);
    }
}
