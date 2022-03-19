package malte0811.controlengineering.logic.schematic.symbol;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.bus.BusSignalRef;
import malte0811.controlengineering.gui.misc.BusSignalSelector;
import malte0811.controlengineering.gui.misc.DataProviderScreen;
import malte0811.controlengineering.util.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class IOSymbol extends SchematicSymbol<BusSignalRef> {
    public static final String INPUT_KEY = ControlEngineering.MODID + ".gui.inputPin";
    public static final String OUTPUT_KEY = ControlEngineering.MODID + ".gui.outputPin";
    public static final String SIGNAL_KEY = ControlEngineering.MODID + ".gui.busRef";
    public static final String VANILLA_COLOR_PREFIX = "item.minecraft.firework_star.";

    private static final List<SymbolPin> OUTPUT_PIN = ImmutableList.of(SymbolPin.analogOut(5, 1, "out"));
    private static final List<SymbolPin> INPUT_PIN = ImmutableList.of(SymbolPin.analogIn(0, 1, "in"));
    private final boolean isInput;

    public IOSymbol(boolean isInput) {
        super(BusSignalRef.DEFAULT, BusSignalRef.CODEC);
        this.isInput = isInput;
    }

    @Override
    public void renderCustom(PoseStack transform, int x, int y, @Nullable BusSignalRef state) {
        int color;
        if (state != null) {
            color = DyeColor.byId(state.color()).getTextColor();
        } else {
            color = 0;
        }
        color |= 0xff000000;
        if (isInput) {
            GuiComponent.fill(transform, x + 3, y + 1, x + 4, y + 2, color);
        } else {
            GuiComponent.fill(transform, x + 2, y + 1, x + 3, y + 2, color);
        }
        final String text = state != null ? Integer.toString(state.line()) : "";
        final int blockX = x + (isInput ? 0 : 3);
        TextUtil.renderBoxWithText(transform, color, text, 4, blockX, y, 3, 3);
    }

    @Override
    public int getXSize() {
        return 6;
    }

    @Override
    public int getYSize() {
        return 3;
    }

    @Override
    public List<SymbolPin> getPins(@Nullable BusSignalRef state) {
        if (isInput) {
            return OUTPUT_PIN;
        } else {
            return INPUT_PIN;
        }
    }

    @Override
    public void createInstanceWithUI(
            Consumer<? super SymbolInstance<BusSignalRef>> onDone,
            BusSignalRef initialState
    ) {
        Minecraft.getInstance().setScreen(new DataProviderScreen<>(
                TextComponent.EMPTY, BusSignalSelector::new, initialState,
                ref -> {
                    SymbolInstance<BusSignalRef> instance = new SymbolInstance<>(this, ref);
                    onDone.accept(instance);
                }
        ));
    }

    @Override
    public Component getName() {
        return new TranslatableComponent(isInput ? INPUT_KEY : OUTPUT_KEY);
    }

    @Override
    public List<MutableComponent> getExtraDescription(BusSignalRef state) {
        final DyeColor color = DyeColor.byId(state.color());
        final String colorName = I18n.get(VANILLA_COLOR_PREFIX + color);
        return ImmutableList.of(new TranslatableComponent(SIGNAL_KEY, colorName, state.line()));
    }

    public boolean isInput() {
        return isInput;
    }
}
