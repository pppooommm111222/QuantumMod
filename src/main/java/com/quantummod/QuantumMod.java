package com.quantummod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuantumMod implements ModInitializer {
    public static final String MOD_ID = "quantummod";
    public static final Map<UUID, BlockPos> DESTINATIONS = new HashMap<>();

    // 修复点1：在 AbstractBlock.Settings 中加入 registryKey
    public static final Block QUANTUM_BLOCK = new Block(AbstractBlock.Settings.create()
            .strength(3.0f)
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "quantum_block")))) {
        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
            if (world.isClient) return ActionResult.SUCCESS;
            UUID playerId = player.getUuid();

            if (player.isSneaking()) {
                DESTINATIONS.put(playerId, pos);
                player.sendMessage(Text.literal("✔ 传送锚点已设定！").formatted(Formatting.GREEN), true);
                world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            } else {
                BlockPos dest = DESTINATIONS.get(playerId);
                if (dest != null) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        String cmd = String.format("tp @s %d %d %d", dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5);
                        serverPlayer.getServer().getCommandManager().executeWithPrefix(serverPlayer.getCommandSource(), cmd);
                    }
                    world.playSound(null, dest, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 1.0f);
                } else {
                    player.sendMessage(Text.literal("✖ 尚未设定目的地！").formatting(Formatting.RED), true);
                }
            }
            return ActionResult.SUCCESS;
        }
    };

    @Override
    public void onInitialize() {
        // 修复点2：注册时直接使用已经带有 registryKey 的实例
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "quantum_block"), QUANTUM_BLOCK);
        
        // 修复点3：BlockItem 也必须在 Settings 中加 registryKey
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "quantum_block"), 
                new BlockItem(QUANTUM_BLOCK, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "quantum_block")))));
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> content.add(QUANTUM_BLOCK));
    }
}
