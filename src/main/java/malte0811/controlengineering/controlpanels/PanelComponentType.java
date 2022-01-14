package malte0811.controlengineering.controlpanels;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import malte0811.controlengineering.bus.BusState;
import malte0811.controlengineering.util.FastDataResult;
import malte0811.controlengineering.util.math.Vec2d;
import malte0811.controlengineering.util.serialization.Codecs;
import malte0811.controlengineering.util.serialization.serial.SerialCodecParser;
import malte0811.controlengineering.util.typereg.TypedRegistryEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public abstract class PanelComponentType<Config, State>
        extends TypedRegistryEntry<Pair<Config, State>, PanelComponentInstance<Config, State>> {
    @Nullable
    // Null = dynamic size
    private final Vec2d size;
    private final SerialCodecParser<Config> configParser;
    private final String translationKey;
    private final AABB defaultSelectionShape;


    protected PanelComponentType(
            Config defaultConfig, State intitialState,
            Codec<Config> codecConfig, Codec<State> codecState,
            @Nullable Vec2d size, double selectionHeight,
            String translationKey
    ) {
        super(Pair.of(defaultConfig, intitialState), Codecs.safePair(codecConfig, codecState));
        this.size = size;
        this.configParser = SerialCodecParser.getParser(codecConfig);
        this.translationKey = translationKey;

        if (selectionHeight >= 0 && size != null) {
            this.defaultSelectionShape = new AABB(0, 0, 0, size.x(), selectionHeight, size.y());
        } else {
            this.defaultSelectionShape = null;
        }
    }

    @Override
    public PanelComponentInstance<Config, State> newInstance(Pair<Config, State> state) {
        return new PanelComponentInstance<>(this, state);
    }

    public PanelComponentInstance<Config, State> newInstanceFromCfg(Config config) {
        return new PanelComponentInstance<>(this, Pair.of(config, getInitialState().getSecond()));
    }

    @Nullable
    public PanelComponentInstance<Config, State> newInstance(FriendlyByteBuf from) {
        return configParser.parse(from).map(this::newInstanceFromCfg).orElse(null);
    }

    public FastDataResult<PanelComponentInstance<Config, State>> newInstance(List<String> data) {
        return configParser.parse(data).map(this::newInstanceFromCfg);
    }

    public SerialCodecParser<Config> getConfigParser() {
        return configParser;
    }

    public List<String> toCNCStrings(Config config) {
        return configParser.stringify(config);
    }

    public abstract BusState getEmittedState(Config config, State state);

    public abstract State updateTotalState(Config config, State oldState, BusState busState);

    public abstract State tick(Config config, State oldState);

    public abstract Pair<InteractionResult, State> click(Config config, State oldState, boolean sneaking);

    @Nullable
    public AABB getSelectionShape() {
        return defaultSelectionShape;
    }

    public Vec2d getSize(Config config) {
        return Objects.requireNonNull(size);
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public final List<IngredientWithSize> getCost() {
        return Objects.requireNonNullElseGet(
                ComponentCostReloadListener.COMPONENT_COSTS.get(getRegistryName()),
                () -> List.of(new IngredientWithSize(Ingredient.of(Items.BEDROCK)))
        );
    }
}
