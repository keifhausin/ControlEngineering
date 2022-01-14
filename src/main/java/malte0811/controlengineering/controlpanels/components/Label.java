package malte0811.controlengineering.controlpanels.components;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.bus.BusState;
import malte0811.controlengineering.controlpanels.PanelComponentType;
import malte0811.controlengineering.controlpanels.components.config.ColorAndText;
import malte0811.controlengineering.util.ServerFontWidth;
import malte0811.controlengineering.util.math.Vec2d;
import net.minecraft.world.InteractionResult;

public class Label extends PanelComponentType<ColorAndText, Unit> {
    public static final String TRANSLATION_KEY = ControlEngineering.MODID + ".component.label";
    public static final int FONT_HEIGHT = 9;
    public static final float SCALE = 1f / FONT_HEIGHT;

    public Label() {
        super(
                ColorAndText.DEFAULT, Unit.INSTANCE,
                ColorAndText.CODEC, Codec.unit(Unit.INSTANCE),
                null, 0, TRANSLATION_KEY
        );
    }

    @Override
    public BusState getEmittedState(ColorAndText s, Unit unit) {
        return BusState.EMPTY;
    }

    @Override
    public Unit updateTotalState(ColorAndText s, Unit oldState, BusState busState) {
        return oldState;
    }

    @Override
    public Unit tick(ColorAndText s, Unit oldState) {
        return oldState;
    }

    @Override
    public Pair<InteractionResult, Unit> click(ColorAndText s, Unit oldState, boolean sneaking) {
        return Pair.of(InteractionResult.PASS, oldState);
    }

    @Override
    public Vec2d getSize(ColorAndText s) {
        // TODO handle client-side on dedicated servers?
        return new Vec2d(SCALE * ServerFontWidth.getWidth(s.text()), 1);
    }
}
