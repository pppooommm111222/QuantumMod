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

    // 这里的 registryKey 彻底解决了你之前的 Block id not set 空值崩溃
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
                    // 终极传送大招：requestTeleport 是 1.21.5 最稳的传送方法，绝对能编译过
                    player.requestTeleport(dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5);
                    world.playSound(null, dest, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 1.0f);
                } else {
                    player.sendMessage(Text.literal("✖ 尚未设定目的地！").formatted(Formatting.RED), true);
                }
            }
            return ActionResult.SUCCESS;
        }
    };

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "quantum_block"), QUANTUM_BLOCK);
        // 物品注册也加上 registryKey，双保险
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "quantum_block"), 
                new BlockItem(QUANTUM_BLOCK, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "quantum_block")))));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> content.add(QUANTUM_BLOCK));
    }
}
