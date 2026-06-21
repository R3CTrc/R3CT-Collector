package com.r3ct.collection.block;

import com.r3ct.collection.Constants;
import com.r3ct.collection.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModBlocks {

    public static final Map<String, Block> TROPHIES = new LinkedHashMap<>();

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":" + name));
    }

    private static BlockBehaviour.Properties trophyProps(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of()
                .setId(key)
                .noOcclusion()
                .isValidSpawn((state, getter, pos, entityType) -> false)
                .isViewBlocking((state, getter, pos) -> false)
                .strength(1.0f)
                .sound(SoundType.GLASS);
    }

    private static Block registerTrophy(String name) {
        Block block = new TrophyBlock(trophyProps(key(name)));
        TROPHIES.put(name, block);
        return block;
    }

    public static final Block TROPHY_BUILDING = registerTrophy("trophy_building");
    public static final Block TROPHY_COMBAT = registerTrophy("trophy_combat");
    public static final Block TROPHY_TOOLS = registerTrophy("trophy_tools");
    public static final Block TROPHY_FOOD = registerTrophy("trophy_food");
    public static final Block TROPHY_REDSTONE = registerTrophy("trophy_redstone");
    public static final Block TROPHY_INGREDIENTS = registerTrophy("trophy_ingredients");
    public static final Block TROPHY_NATURAL = registerTrophy("trophy_natural");
    public static final Block TROPHY_COLORED = registerTrophy("trophy_colored");
    public static final Block TROPHY_EGG = registerTrophy("trophy_egg");
    public static final Block TROPHY_FUNCTIONAL = registerTrophy("trophy_functional");
    public static final Block TROPHY_MOD = registerTrophy("trophy_mod");

    public static final ResourceKey<BlockEntityType<?>> TROPHY_BE_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.parse(Constants.MOD_ID + ":trophy_building_be")
    );

    public static final BlockEntityType<TrophyBlockEntity> TROPHY_BE_TYPE = Services.PLATFORM.createBlockEntityType(
            (pos, state) -> new TrophyBlockEntity(ModBlocks.TROPHY_BE_TYPE, pos, state),
            TROPHIES.values().toArray(new Block[0])
    );
}