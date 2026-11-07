package com.example.orebore;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;
import java.util.function.Supplier;

public final class ModRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OreBore.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OreBore.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OreBore.MOD_ID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().strength(4.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion().requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<Block> SUPPORT_BEAM =
            BLOCKS.register("support_beam", () -> new Block(metal()));
    public static final DeferredBlock<DrillControllerBlock> CONTROLLER_3X3 =
            BLOCKS.register("drill_controller_3x3", () -> new DrillControllerBlock(metal(), DrillTier.SMALL_3X3));
    public static final DeferredBlock<DrillControllerBlock> CONTROLLER_5X5 =
            BLOCKS.register("drill_controller_5x5", () -> new DrillControllerBlock(metal(), DrillTier.LARGE_5X5));

    /** Блок головки бура: из них игрок собирает форму 3x3 / 5x5. Сам ничего не ломает. */
    public static final DeferredBlock<Block> DRILL_HEAD =
            BLOCKS.register("drill_head", () -> new Block(metal().strength(5.0f, 8.0f)));
    /** Платформа под головку (того же размера, что и головка). */
    public static final DeferredBlock<Block> DRILL_PLATFORM =
            BLOCKS.register("drill_platform", () -> new Block(metal()));

    public static final DeferredItem<BlockItem> DRILL_HEAD_ITEM = ITEMS.registerSimpleBlockItem(DRILL_HEAD);
    public static final DeferredItem<BlockItem> DRILL_PLATFORM_ITEM = ITEMS.registerSimpleBlockItem(DRILL_PLATFORM);
    public static final DeferredItem<BlockItem> SUPPORT_BEAM_ITEM = ITEMS.registerSimpleBlockItem(SUPPORT_BEAM);
    public static final DeferredItem<BlockItem> CONTROLLER_3X3_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER_3X3);
    public static final DeferredItem<BlockItem> CONTROLLER_5X5_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER_5X5);

    public static final Supplier<BlockEntityType<DrillControllerBlockEntity>> CONTROLLER_BE =
            BLOCK_ENTITIES.register("drill_controller", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new DrillControllerBlockEntity(ModRegistries.CONTROLLER_BE.get(), pos, state),
                    CONTROLLER_3X3.get(), CONTROLLER_5X5.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    private ModRegistries() {}
}
