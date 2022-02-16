package malte0811.controlengineering.datagen;

import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.api.IETags;
import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.blocks.CEBlocks;
import malte0811.controlengineering.crafting.CERecipeSerializers;
import malte0811.controlengineering.datagen.recipes.GlueTapeBuilder;
import malte0811.controlengineering.datagen.recipes.NoAdvancementShapedBuilder;
import malte0811.controlengineering.items.CEItems;
import malte0811.controlengineering.items.IEItemRefs;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class Recipes extends RecipeProvider {
    public Recipes(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void buildCraftingRecipes(@Nonnull Consumer<FinishedRecipe> consumer) {
        SpecialRecipeBuilder.special(CERecipeSerializers.PANEL_RECIPE.get())
                .save(consumer, ControlEngineering.MODID + ":panel");
        GlueTapeBuilder.customRecipe(Ingredient.of(Tags.Items.SLIMEBALLS))
                .build(consumer, "glue_tape");
        NoAdvancementShapedBuilder.shaped(CEItems.BUS_WIRE_COIL.get())
                .pattern("pcp")
                .pattern("cpc")
                .pattern("pcp")
                .define('p', IETags.getTagsFor(EnumMetals.ALUMINUM).plate)
                .define('c', IEItemRefs.REDSTONE_WIRE_COIL.get())
                .save(consumer);
        NoAdvancementShapedBuilder.shaped(CEBlocks.BUS_RELAY.get(), 4)
                .pattern("prp")
                .pattern("bbb")
                .define('p', IETags.getTagsFor(EnumMetals.ALUMINUM).plate)
                .define('b', Blocks.TERRACOTTA)
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .save(consumer);
        NoAdvancementShapedBuilder.shaped(CEBlocks.BUS_INTERFACE.get())
                .pattern("prp")
                .pattern("bcb")
                .define('p', IETags.getTagsFor(EnumMetals.ALUMINUM).plate)
                .define('b', Blocks.TERRACOTTA)
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('c', CEItems.BUS_WIRE_COIL.get())
                .save(consumer);
        NoAdvancementShapedBuilder.shaped(CEBlocks.LINE_ACCESS.get())
                .pattern("r b")
                .pattern("RcB")
                .define('r', IEItemRefs.REDSTONE_CONNECTOR.get())
                .define('R', IEItemRefs.REDSTONE_WIRE_COIL.get())
                .define('c', Items.COMPARATOR)
                .define('b', CEBlocks.BUS_RELAY.get())
                .define('B', CEItems.BUS_WIRE_COIL.get())
                .save(consumer);
    }
}
