package malte0811.controlengineering.network.logic;

import malte0811.controlengineering.logic.schematic.Schematic;
import malte0811.controlengineering.logic.schematic.symbol.PlacedSymbol;
import malte0811.controlengineering.logic.schematic.symbol.SymbolInstance;
import malte0811.controlengineering.util.math.Vec2i;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;

public class AddSymbol extends LogicSubPacket {
    private final PlacedSymbol symbol;

    public AddSymbol(PlacedSymbol symbol) {
        this.symbol = symbol;
    }

    public AddSymbol(FriendlyByteBuf in) {
        this.symbol = new PlacedSymbol(new Vec2i(in), in.readWithCodec(SymbolInstance.CODEC));
    }

    @Override
    public void write(FriendlyByteBuf out) {
        symbol.getPosition().write(out);
        out.writeWithCodec(SymbolInstance.CODEC, symbol.getSymbol());
    }

    @Override
    protected void process(Schematic applyTo, Consumer<Schematic> replace) {
        if (applyTo.getChecker().canAdd(symbol)) {
            applyTo.addSymbol(symbol);
        }
    }
}
