package malte0811.controlengineering.gui;

import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.network.SimplePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = ControlEngineering.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public abstract class CEContainerMenu<PacketType> extends AbstractContainerMenu {
    private final List<ServerPlayer> listeners = new ArrayList<>();
    private final Predicate<Player> isValid;
    private final Runnable setChanged;

    protected CEContainerMenu(@Nullable MenuType<?> type, int id, Predicate<Player> isValid, Runnable setChanged) {
        super(type, id);
        this.isValid = isValid;
        this.setChanged = setChanged;
    }

    protected CEContainerMenu(@Nullable MenuType<?> type, int id) {
        this(type, id, $ -> true, () -> {});
    }

    @Override
    public boolean stillValid(@Nonnull Player playerIn) {
        return isValid.test(playerIn);
    }

    public void sendToListeningPlayers(PacketType data) {
        sendToListeningPlayersExcept(null, data);
    }

    public void sendToListeningPlayersExcept(@Nullable ServerPlayer excluded, PacketType data) {
        for (var player : listeners) {
            if (player != excluded) {
                sendTo(player, data);
            }
        }
    }

    protected final void sendTo(ServerPlayer listener, PacketType packet) {
        ControlEngineering.NETWORK.sendTo(
                makePacket(packet), listener.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT
        );
    }

    protected abstract SimplePacket makePacket(PacketType type);

    protected abstract PacketType getInitialSync();

    public void markDirty() {
        setChanged.run();
    }

    private void addListener(ServerPlayer serverPlayer) {
        if (!listeners.contains(serverPlayer)) {
            if (listeners.isEmpty()) {
                onFirstOpened();
            }
            listeners.add(serverPlayer);
            sendTo(serverPlayer, getInitialSync());
        }
    }

    protected void onFirstOpened() {}

    protected void onLastClosed() {}

    @SubscribeEvent
    public static void openContainer(PlayerContainerEvent.Open ev) {
        if (!(ev.getContainer() instanceof CEContainerMenu<?> ceContainer)) {
            return;
        }
        if (!(ev.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ceContainer.addListener(serverPlayer);
    }

    @SubscribeEvent
    public static void closeContainer(PlayerContainerEvent.Close ev) {
        if (!(ev.getContainer() instanceof CEContainerMenu<?> ceContainer)) {
            return;
        }
        if (!(ev.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ceContainer.listeners.remove(serverPlayer);
        if (ceContainer.listeners.isEmpty()) {
            ceContainer.onLastClosed();
        }
    }

    public static Predicate<Player> isValidFor(BlockEntity menuBE) {
        return p -> !menuBE.isRemoved() && p.distanceToSqr(Vec3.atCenterOf(menuBE.getBlockPos())) <= 64.0D;
    }
}
