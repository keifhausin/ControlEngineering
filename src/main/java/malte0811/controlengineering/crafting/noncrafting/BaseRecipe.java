package malte0811.controlengineering.crafting.noncrafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class BaseRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final RecipeSerializer<?> serializer;
    private final RecipeType<?> type;

    public BaseRecipe(ResourceLocation id, Supplier<? extends RecipeSerializer<?>> serializer, RecipeType<?> type) {
        this.id = id;
        this.serializer = serializer.get();
        this.type = type;
    }

    @Override
    public boolean matches(@Nonnull Container pContainer, @Nonnull Level pLevel) {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull Container pContainer) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Nonnull
    @Override
    public RecipeType<?> getType() {
        return type;
    }

    protected static abstract class BaseSerializer<R extends BaseRecipe> extends ForgeRegistryEntry<RecipeSerializer<?>>
            implements RecipeSerializer<R> {}
}
