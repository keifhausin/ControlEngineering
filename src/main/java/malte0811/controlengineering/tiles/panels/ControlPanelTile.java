package malte0811.controlengineering.tiles.panels;

import malte0811.controlengineering.blocks.panels.PanelBlock;
import malte0811.controlengineering.blocks.panels.PanelOrientation;
import malte0811.controlengineering.blocks.shapes.SelectionShapeOwner;
import malte0811.controlengineering.blocks.shapes.SelectionShapes;
import malte0811.controlengineering.blocks.shapes.SingleShape;
import malte0811.controlengineering.bus.*;
import malte0811.controlengineering.controlpanels.*;
import malte0811.controlengineering.controlpanels.components.ColorAndSignal;
import malte0811.controlengineering.tiles.CETileEntities;
import malte0811.controlengineering.util.Clearable;
import malte0811.controlengineering.util.RaytraceUtils;
import malte0811.controlengineering.util.Vec2d;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ControlPanelTile extends TileEntity implements IBusInterface, SelectionShapeOwner {
    private final MarkDirtyHandler markBusDirty = new MarkDirtyHandler();
    private List<PlacedComponent> components = new ArrayList<>();
    private PanelTransform transform = new PanelTransform(
            0.25F,
            (float) Math.toDegrees(Math.atan(0.5)),
            PanelOrientation.DOWN_NORTH
    );
    private BusState inputState = BusState.EMPTY;
    private final BusEmitterCombiner<Integer> stateHandler = new BusEmitterCombiner<>(
            i -> components.get(i).getComponent().getEmittedState(),
            i -> components.get(i).getComponent().updateTotalState(getTotalState())
    );

    public ControlPanelTile() {
        super(CETileEntities.CONTROL_PANEL.get());
        components.add(new PlacedComponent(
                PanelComponents.BUTTON.newInstance(
                        new ColorAndSignal(0xff0000, new BusSignalRef(0, 0))
                ), new Vec2d(5, 6)
        ));
        components.add(new PlacedComponent(
                PanelComponents.BUTTON.newInstance(
                        new ColorAndSignal(0xff00, new BusSignalRef(0, 1))
                ), new Vec2d(5, 7)
        ));
        components.add(new PlacedComponent(
                PanelComponents.BUTTON.newInstance(
                        new ColorAndSignal(0xff, new BusSignalRef(0, 2))
                ), new Vec2d(5, 8)
        ));
        components.add(new PlacedComponent(
                PanelComponents.INDICATOR.newInstance(
                        new ColorAndSignal(0xff00ff, new BusSignalRef(0, 3))
                ), new Vec2d(6, 6.5)
        ));
        components.add(new PlacedComponent(
                PanelComponents.INDICATOR.newInstance(
                        new ColorAndSignal(0xffff00, new BusSignalRef(0, 4))
                ), new Vec2d(6, 7.5)
        ));
        resetStateHandler();
    }

    private void resetStateHandler() {
        stateHandler.clear();
        for (int i = 0; i < components.size(); ++i) {
            stateHandler.addEmitter(i);
        }
        updateBusState(SyncType.NEVER);
    }

    public void updateBusState(SyncType sync) {
        BusState oldState = stateHandler.getTotalEmittedState();
        stateHandler.updateState(this.inputState);
        boolean changed = !oldState.equals(stateHandler.getTotalEmittedState());
        if (changed) {
            markBusDirty.run();
        }
        if (sync.shouldSync(changed) && world != null) {
            BlockState state = getBlockState();
            world.notifyBlockUpdate(pos, state, state, 0);
        }
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);
        readComponentsAndTransform(nbt, state.get(PanelOrientation.PROPERTY));
    }

    public void readComponentsAndTransform(CompoundNBT nbt, PanelOrientation orientation) {
        final PanelData data = new PanelData(nbt, orientation);
        this.transform = data.getTransform();
        this.components = data.getComponents();
        if (world != null && !world.isRemote) {
            resetStateHandler();
        }
    }

    @Override
    public void setWorldAndPos(@Nonnull World world, @Nonnull BlockPos pos) {
        super.setWorldAndPos(world, pos);
        if (!world.isRemote) {
            resetStateHandler();
        }
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compound) {
        CompoundNBT encoded = super.write(compound);
        encoded.merge(new PanelData(this).toNBT());
        return encoded;
    }

    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT nbttagcompound = new CompoundNBT();
        write(nbttagcompound);
        return new SUpdateTileEntityPacket(this.pos, 3, nbttagcompound);
    }

    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        read(getBlockState(), pkt.getNbtCompound());
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        return write(new CompoundNBT());
    }

    public Optional<PlacedComponent> getTargetedComponent(RayTraceContext ctx) {
        BlockPos topPos = pos.offset(getBlockState().get(PanelOrientation.PROPERTY).top);
        RayTraceContext topCtx = getTransform().toPanelRay(ctx.getStartVec(), ctx.getEndVec(), topPos);
        Optional<PlacedComponent> closest = Optional.empty();
        double minDistanceSq = Double.POSITIVE_INFINITY;
        for (PlacedComponent comp : getComponents()) {
            final AxisAlignedBB selectionShape = comp.getSelectionShape();
            if (selectionShape != null) {
                final Optional<Vector3d> result = selectionShape.rayTrace(topCtx.getStartVec(), topCtx.getEndVec());
                if (result.isPresent()) {
                    final double distanceSq = result.get().squareDistanceTo(topCtx.getStartVec());
                    if (distanceSq < minDistanceSq) {
                        minDistanceSq = distanceSq;
                        closest = Optional.of(comp);
                    }
                }
            }
        }
        return closest;
    }

    public List<PlacedComponent> getComponents() {
        return components;
    }

    public PanelTransform getTransform() {
        return transform;
    }

    private BusState getTotalState() {
        return stateHandler.getTotalState();
    }

    public ActionResultType onRightClick(PlayerEntity player, BlockState state) {
        RayTraceContext raytraceCtx = RaytraceUtils.create(player, 0);
        Optional<PlacedComponent> targeted = getTargetedComponent(raytraceCtx);
        if (targeted.isPresent()) {
            ActionResultType result = targeted.get().onClick();
            if (!world.isRemote) {
            }
            return result;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void onBusUpdated(BusState newState) {
        if (!newState.equals(inputState)) {
            this.inputState = newState;
            this.updateBusState(SyncType.IF_CHANGED);
        }
    }

    @Override
    public BusState getEmittedState() {
        return this.stateHandler.getTotalEmittedState();
    }

    @Override
    public boolean canConnect(Direction fromSide) {
        //TODO? At least forbid for panel top?
        return true;
    }

    @Override
    public void addMarkDirtyCallback(Clearable<Runnable> markDirty) {
        this.markBusDirty.addCallback(markDirty);
    }

    @Override
    public void remove() {
        super.remove();
        this.markBusDirty.run();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.markBusDirty.run();
    }

    @Override
    public SelectionShapes getShape() {
        BlockState state = getBlockState();
        if (!state.hasProperty(PanelBlock.IS_BASE) || state.get(PanelBlock.IS_BASE)) {
            return SingleShape.FULL_BLOCK;
        }
        ControlPanelTile base = PanelBlock.getBase(world, state, pos);
        if (base == null) {
            return SingleShape.FULL_BLOCK;
        }
        return new PanelSelectionShapes(base);
    }

    public enum SyncType {
        NEVER,
        IF_CHANGED,
        ALWAYS;

        public boolean shouldSync(boolean changed) {
            switch (this) {
                case NEVER:
                    return false;
                case IF_CHANGED:
                    return changed;
                case ALWAYS:
                    return true;
            }
            throw new UnsupportedOperationException("Unknown sync type " + name());
        }
    }
}
