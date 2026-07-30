package awa.Aether_254.create_wrench_guard.mixin;

import awa.Aether_254.create_wrench_guard.PendingConfirmation;
import awa.Aether_254.create_wrench_guard.WrenchGuardConfig;
import awa.Aether_254.create_wrench_guard.WrenchGuardConfig.Mode;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WrenchItem.class)
abstract class WrenchItemMixin {
    @Unique
    private static final Map<UUID, PendingConfirmation> wrenchGuard$pending = new HashMap<>();
    @Unique
    private static final Map<UUID, BlockPos> wrenchGuard$confirmed = new HashMap<>();

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void wrenchGuard$beforeRemoval(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown())
            return;
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof IWrenchable)
            && !AllTags.AllBlockTags.WRENCH_PICKUP.matches(state))
            return;

        Mode mode = WrenchGuardConfig.modeFor(state);
        if (context.getLevel().isClientSide) {
            if (mode != Mode.ALLOW)
                cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (mode == Mode.DENY) {
            actionbar(player, state.getBlock().getName().getString() + " 已被保护，无法拆除。");
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (mode == Mode.WARN && !consumeConfirmation(player, context)) {
            wrenchGuard$pending.put(player.getUUID(), new PendingConfirmation(
                context.getClickedPos().immutable(), context.getLevel().dimension().location().toString(),
                context.getLevel().getGameTime() + 100));
            actionbar(player, "确定要拆除 " + state.getBlock().getName().getString()
                + " 吗？再次按 Shift+右键 确认。");
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (WrenchGuardConfig.get().preventWhenInventoryFull && !player.isCreative()
            && context.getLevel() instanceof ServerLevel serverLevel
            && !canFitDrops(player, serverLevel, context.getClickedPos(), state, context.getItemInHand())) {
            actionbar(player, "背包已满，无法拆除");
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (mode == Mode.WARN)
            wrenchGuard$confirmed.put(player.getUUID(), context.getClickedPos().immutable());
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void wrenchGuard$afterRemoval(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || context.getLevel().isClientSide)
            return;
        BlockPos confirmed = wrenchGuard$confirmed.remove(player.getUUID());
        if (confirmed != null && confirmed.equals(context.getClickedPos())
            && context.getLevel().getBlockState(confirmed).isAir())
            actionbar(player, "已拆除方块。");
    }

    @Unique
    private static boolean consumeConfirmation(Player player, UseOnContext context) {
        PendingConfirmation pending = wrenchGuard$pending.remove(player.getUUID());
        return pending != null
            && pending.pos().equals(context.getClickedPos())
            && pending.dimension().equals(context.getLevel().dimension().location().toString())
            && pending.expiresAt() >= context.getLevel().getGameTime();
    }

    @Unique
    private static boolean canFitDrops(
        Player player, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool
    ) {
        ItemStackHandler simulated = new ItemStackHandler(36);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++)
            simulated.setStackInSlot(slot, inventory.getItem(slot).copy());
        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool)) {
            if (!ItemHandlerHelper.insertItemStacked(simulated, drop.copy(), false).isEmpty())
                return false;
        }
        return true;
    }

    @Unique
    private static void actionbar(Player player, String message) {
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GOLD), true);
    }
}
