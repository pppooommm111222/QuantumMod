#!/bin/bash

echo "1. 创建目录结构..."
mkdir -p src/main/java/com/quantummod
mkdir -p src/main/resources/data/quantummod/recipe
mkdir -p src/main/resources/assets/quantummod/blockstates
mkdir -p src/main/resources/assets/quantummod/models/block
mkdir -p src/main/resources/assets/quantummod/models/item
mkdir -p .github/workflows

echo "2. 写入 Java 主类..."
cat << 'EOF' > src/main/java/com/quantummod/QuantumMod.java
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuantumMod implements ModInitializer {
    public static final String MOD_ID = "quantummod";
    public static final Map<UUID, BlockPos> DESTINATIONS = new HashMap<>();

    public static final Block QUANTUM_BLOCK = new Block(AbstractBlock.Settings.create()
            .strength(3.0f)
            .requiresTool()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "quantum_block")))) {
        
        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
            if (world.isClient) return ActionResult.SUCCESS;
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            UUID playerId = player.getUuid();

            if (player.isSneaking()) {
                DESTINATIONS.put(playerId, pos);
                player.sendMessage(Text.literal("✔ 传送锚点已设定！").formatted(Formatting.GREEN), true);
                world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            } else {
                BlockPos dest = DESTINATIONS.get(playerId);
                if (dest != null) {
                    serverPlayer.teleport(serverPlayer.getServerWorld(), dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5, serverPlayer.getYaw(), serverPlayer.getPitch());
                    world.playSound(null, dest, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    serverPlayer.getServerWorld().spawnParticles(ParticleTypes.PORTAL, dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5, 50, 0.5, 0.5, 0.5, 0.1);
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
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "quantum_block"), new BlockItem(QUANTUM_BLOCK, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "quantum_block")))));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> content.add(QUANTUM_BLOCK));
    }
}
EOF

echo "3. 写入配方 JSON..."
cat << 'EOF' > src/main/resources/data/quantummod/recipe/quantum_block.json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", "IRI", "III"],
  "key": {
    "I": { "item": "minecraft:iron_ingot" },
    "R": { "item": "minecraft:redstone_block" }
  },
  "result": { "id": "quantummod:quantum_block", "count": 1 }
}
EOF

echo "4. 写入模型和状态文件..."
echo '{ "variants": { "": { "model": "quantummod:block/quantum_block" } } }' > src/main/resources/assets/quantummod/blockstates/quantum_block.json
echo '{ "parent": "minecraft:block/cube_all", "textures": { "all": "quantummod:block/quantum_block" } }' > src/main/resources/assets/quantummod/models/block/quantum_block.json
echo '{ "parent": "quantummod:block/quantum_block" }' > src/main/resources/assets/quantummod/models/item/quantum_block.json

echo "5. 写入 fabric.mod.json..."
cat << 'EOF' > src/main/resources/fabric.mod.json
{
  "schemaVersion": 1,
  "id": "quantummod",
  "version": "1.0.0",
  "name": "Quantum Mod",
  "environment": "*",
  "entrypoints": { "main": [ "com.quantummod.QuantumMod" ] },
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~1.21.5",
    "java": ">=21",
    "fabric-api": "*"
  }
}
EOF

echo "6. 写入 build.gradle 和 settings.gradle..."
cat << 'EOF' > build.gradle
plugins {
    id 'fabric-loom' version '1.10-SNAPSHOT'
    id 'maven-publish'
}
version = "1.0.0"
group = "com.quantummod"
repositories { maven { url = "https://maven.fabricmc.net/" } }
dependencies {
    minecraft "com.mojang:minecraft:1.21.5"
    mappings "net.fabricmc:yarn:1.21.5+build.1:v2"
    modImplementation "net.fabricmc:fabric-loader:0.16.10"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.128.2+1.21.5"
}
processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") { expand "version": project.version }
}
tasks.withType(JavaCompile).configureEach { it.options.release = 21 }
java { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
EOF

cat << 'EOF' > settings.gradle
pluginManagement {
    repositories {
        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
        gradlePluginPortal()
    }
}
EOF

echo "7. 写入 GitHub Actions..."
cat << 'EOF' > .github/workflows/build.yml
name: Build Mod
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      - name: Build with Gradle
        run: gradle build
      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: mod-jar
          path: build/libs/*.jar
EOF

echo "8. 初始化 Git 并绑定仓库..."
git init
git add .
git commit -m "Create Quantum Mod by Termux"

# 强制设置远程仓库（防止你之前配错或重复添加报错）
git remote remove origin 2>/dev/null
git remote add origin https://github.com/pppooommm111222/QuantumMod.git
git branch -M main

echo "====================================================="
echo "准备推送代码！"
echo "Username 请输入: pppooommm111222"
echo "Password 请粘贴你的完整 ghp_ Token（粘贴后没反应是正常的，直接回车）"
echo "====================================================="

git push -u origin main

