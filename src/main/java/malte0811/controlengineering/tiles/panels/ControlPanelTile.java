package malte0811.controlengineering.tiles.panels;

import malte0811.controlengineering.blocks.panels.PanelBlock;
import malte0811.controlengineering.blocks.panels.PanelOrientation;
import malte0811.controlengineering.blocks.shapes.SelectionShapeOwner;
import malte0811.controlengineering.blocks.shapes.SelectionShapes;
import malte0811.controlengineering.blocks.shapes.SingleShape;
import malte0811.controlengineering.bus.BusEmitterCombiner;
import malte0811.controlengineering.bus.BusState;
import malte0811.controlengineering.bus.IBusInterface;
import malte0811.controlengineering.bus.MarkDirtyHandler;
import malte0811.controlengineering.controlpanels.PanelData;
import malte0811.controlengineering.controlpanels.PanelSelectionShapes;
import malte0811.controlengineering.controlpanels.PanelTransform;
import malte0811.controlengineering.controlpanels.PlacedComponent;
import malte0811.controlengineering.tiles.base.CETileEntity;
import malte0811.controlengineering.tiles.base.IHasMaster;
import malte0811.controlengineering.util.Clearable;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ControlPanelTile extends CETileEntity implements IBusInterface, SelectionShapeOwner, IHasMaster {
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

    public ControlPanelTile(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
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

    @Override
    protected CompoundNBT writeSyncedData(CompoundNBT out) {
        out.merge(new PanelData(this).toNBT());
        return out;
    }

    @Override
    protected void readSyncedData(CompoundNBT in) {
        readComponentsAndTransform(in, getBlockState().get(PanelOrientation.PROPERTY));
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compound) {
        return writeSyncedData(compound);
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

    public PanelData getData() {
        return new PanelData(this);
    }

    @Nullable
    @Override
    public ControlPanelTile computeMasterTile(BlockState stateHere) {
        return PanelBlock.getBase(world, stateHere, pos);
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
