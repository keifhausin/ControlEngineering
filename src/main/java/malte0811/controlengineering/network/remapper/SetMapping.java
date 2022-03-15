package malte0811.controlengineering.network.remapper;

import malte0811.controlengineering.blockentity.bus.RSRemapperBlockEntity;
import net.minecraft.network.FriendlyByteBuf;

public class SetMapping extends RSRemapperSubPacket {
    private final int colorIndex;
    private final int grayIndex;

    public SetMapping(int colorIndex, int grayIndex) {
        this.colorIndex = colorIndex;
        this.grayIndex = grayIndex;
    }

    public SetMapping(FriendlyByteBuf in) {
        this(in.readVarInt(), in.readVarInt());
    }

    @Override
    protected void write(FriendlyByteBuf out) {
        out.writeVarInt(colorIndex);
        out.writeVarInt(grayIndex);
    }

    @Override
    protected int[] process(int[] colorToGray) {
        for (int i = 0; i < colorToGray.length; ++i) {
            if (colorToGray[i] == grayIndex) {
                colorToGray[i] = RSRemapperBlockEntity.NOT_MAPPED;
            }
        }
        colorToGray[colorIndex] = grayIndex;
        return colorToGray;
    }
}
