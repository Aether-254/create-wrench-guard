package awa.Aether_254.create_wrench_guard;

import net.minecraft.core.BlockPos;

public record PendingConfirmation(BlockPos pos, String dimension, long expiresAt) {
}
