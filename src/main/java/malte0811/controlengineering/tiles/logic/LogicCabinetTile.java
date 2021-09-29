package malte0811.controlengineering.tiles.logic;

import blusunrize.immersiveengineering.api.utils.client.SinglePropertyModelData;
import com.mojang.datafixers.util.Pair;
import malte0811.controlengineering.blocks.logic.LogicCabinetBlock;
import malte0811.controlengineering.blocks.shapes.*;
import malte0811.controlengineering.bus.BusState;
import malte0811.controlengineering.bus.IBusInterface;
import malte0811.controlengineering.bus.MarkDirtyHandler;
import malte0811.controlengineering.gui.logic.LogicDesignContainer;
import malte0811.controlengineering.items.CEItems;
import malte0811.controlengineering.items.PCBStackItem;
import malte0811.controlengineering.logic.circuit.BusConnectedCircuit;
import malte0811.controlengineering.logic.clock.ClockGenerator;
import malte0811.controlengineering.logic.clock.ClockGenerator.ClockInstance;
import malte0811.controlengineering.logic.clock.ClockTypes;
import malte0811.controlengineering.logic.model.DynamicLogicModel;
import malte0811.controlengineering.logic.schematic.Schematic;
import malte0811.controlengineering.logic.schematic.SchematicCircuitConverter;
import malte0811.controlengineering.tiles.base.CETileEntity;
import malte0811.controlengineering.tiles.base.IExtraDropTile;
import malte0811.controlengineering.tiles.base.IHasMaster;
import malte0811.controlengineering.util.*;
import malte0811.controlengineering.util.energy.CEEnergyStorage;
import malte0811.controlengineering.util.math.Matrix4;
import malte0811.controlengineering.util.serialization.Codecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fmllegacy.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class LogicCabinetTile extends CETileEntity implements SelectionShapeOwner, IBusInterface,
        ISchematicTile, IExtraDropTile, IHasMaster {
    public static final int MAX_NUM_BOARDS = 4;
    public static final int NUM_TUBES_PER_BOARD = 16;

    private final CEEnergyStorage energy = new CEEnergyStorage(2048, 2 * 128, 128);
    @Nullable
    private Pair<Schematic, BusConnectedCircuit> circuit;
    @Nonnull
    private ClockInstance<?> clock = ClockTypes.NEVER.newInstance();
    private final MarkDirtyHandler markBusDirty = new MarkDirtyHandler();
    private int numTubes;
    private BusState currentBusState = BusState.EMPTY;

    public LogicCabinetTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    public static int getNumBoardsFor(int numTubes) {
        return Mth.ceil(numTubes / (double) NUM_TUBES_PER_BOARD);
    }

    public void tick() {
        //TODO less? config?
        if (circuit == null || energy.extractOrTrue(128) || level.getGameTime() % 2 != 0) {
            return;
        }
        final Direction facing = getFacing(getBlockState());
        final Direction clockFace = facing.getCounterClockWise();
        boolean rsIn = level.getSignal(worldPosition.relative(clockFace), clockFace.getOpposite()) > 0;
        if (!clock.tick(rsIn)) {
            return;
        }
        // Inputs are updated in onBusUpdated
        if (circuit.getSecond().tick()) {
            markBusDirty.run();
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        clock = Codecs.readOrNull(ClockInstance.CODEC, nbt.getCompound("clock"));
        if (clock == null) {
            clock = ClockTypes.NEVER.newInstance();
        }
        if (nbt.contains("circuit")) {
            setCircuit(Codecs.readOrNull(Schematic.CODEC, nbt.get("circuit")));
        } else {
            setCircuit(null);
        }
        energy.readNBT(nbt.get("energy"));
    }

    @Nonnull
    @Override
    public CompoundTag save(@Nonnull CompoundTag compound) {
        compound = super.save(compound);
        compound.put("clock", Codecs.encode(ClockInstance.CODEC, clock));
        if (circuit != null) {
            compound.put("circuit", Codecs.encode(Schematic.CODEC, circuit.getFirst()));
        }
        compound.put("energy", energy.writeNBT());
        return compound;
    }

    @Override
    protected CompoundTag writeSyncedData(CompoundTag result) {
        result.putBoolean("hasClock", clock.getType().isActiveClock());
        result.putInt("numTubes", numTubes);
        return result;
    }

    @Override
    protected void readSyncedData(CompoundTag tag) {
        if (tag.getBoolean("hasClock"))
            clock = ClockTypes.ALWAYS_ON.newInstance();
        else
            clock = ClockTypes.NEVER.newInstance();
        numTubes = tag.getInt("numTubes");
        requestModelDataUpdate();
        level.sendBlockUpdated(
                worldPosition, getBlockState(), getBlockState(), Constants.BlockFlags.DEFAULT
        );
    }

    @Nonnull
    @Override
    public IModelData getModelData() {
        return new SinglePropertyModelData<>(
                new DynamicLogicModel.ModelData(numTubes, clock.getType().isActiveClock()), DynamicLogicModel.DATA
        );
    }

    @Override
    public void onBusUpdated(BusState newState) {
        if (circuit != null) {
            circuit.getSecond().updateInputs(newState);
        }
        this.currentBusState = newState;
    }

    @Override
    public BusState getEmittedState() {
        if (circuit != null) {
            return circuit.getSecond().getOutputState();
        } else {
            return BusState.EMPTY;
        }
    }

    @Override
    public boolean canConnect(Direction fromSide) {
        return fromSide == getFacing(getBlockState()).getClockWise();
    }

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(energy::insertOnlyView);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(
            @Nonnull Capability<T> cap, @Nullable Direction side
    ) {
        if (cap == CapabilityEnergy.ENERGY && side == getFacing(getBlockState())) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void addMarkDirtyCallback(Clearable<Runnable> markDirty) {
        this.markBusDirty.addCallback(markDirty);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.markBusDirty.run();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.markBusDirty.run();
    }

    public void setCircuit(@Nullable Schematic schematic) {
        this.circuit = null;
        if (schematic != null) {
            Optional<BusConnectedCircuit> busCircuit = SchematicCircuitConverter.toCircuit(schematic);
            if (busCircuit.isPresent()) {
                this.circuit = Pair.of(schematic, busCircuit.get());
            }
        }
        if (this.circuit != null) {
            this.numTubes = this.circuit.getSecond().getNumTubes();
            this.circuit.getSecond().updateInputs(currentBusState);
        } else {
            this.numTubes = 0;
        }
    }

    private static Direction getFacing(BlockState state) {
        return state.getValue(LogicCabinetBlock.FACING);
    }

    private static boolean isUpper(BlockState state) {
        return state.getValue(LogicCabinetBlock.HEIGHT) != 0;
    }

    private final CachedValue<BlockState, SelectionShapes> selectionShapes = new CachedValue<>(
            this::getBlockState,
            state -> createSelectionShapes(getFacing(state), computeMasterTile(state), isUpper(state))
    );

    @Override
    public SelectionShapes getShape() {
        return selectionShapes.get();
    }

    private static SelectionShapes createSelectionShapes(Direction d, LogicCabinetTile tile, boolean upper) {
        List<SelectionShapes> subshapes = new ArrayList<>(1);
        DirectionalShapeProvider baseShape = upper ? LogicCabinetBlock.TOP_SHAPE : LogicCabinetBlock.BOTTOM_SHAPE;
        if (!upper) {
            subshapes.add(makeClockInteraction(tile));
        } else {
            subshapes.add(makeViewDesignInteraction(tile));
        }
        subshapes.add(makeBoardInteraction(tile, upper));
        return new ListShapes(
                baseShape.apply(d), Matrix4.inverseFacing(d), subshapes, $ -> InteractionResult.PASS
        );
    }

    private static SelectionShapes makeClockInteraction(LogicCabinetTile tile) {
        return new SingleShape(
                ShapeUtils.createPixelRelative(0, 6, 6, 5, 10, 10), ctx -> {
            if (ctx.getPlayer() == null) {
                return InteractionResult.PASS;
            }
            ClockGenerator<?> currentClock = tile.clock.getType();
            RegistryObject<Item> clockItem = CEItems.CLOCK_GENERATORS.get(currentClock.getRegistryName());
            if (!ctx.getLevel().isClientSide) {
                if (clockItem != null) {
                    ItemUtil.giveOrDrop(ctx.getPlayer(), new ItemStack(clockItem.get()));
                    tile.clock = ClockTypes.NEVER.newInstance();
                    TileUtil.markDirtyAndSync(tile);
                } else {
                    ItemStack item = ctx.getItemInHand();
                    ClockGenerator<?> newClock = ClockTypes.REGISTRY.get(item.getItem().getRegistryName());
                    if (newClock != null) {
                        tile.clock = newClock.newInstance();
                        item.shrink(1);
                        TileUtil.markDirtyAndSync(tile);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static SelectionShapes makeBoardInteraction(LogicCabinetTile tile, boolean upper) {
        final int yOff = upper ? -16 : 0;
        final VoxelShape fullShape = ShapeUtils.createPixelRelative(
                1, 11 + yOff, 1, 15, 31 + yOff, 15
        );
        return new SingleShape(
                fullShape, ctx -> {
            if (ctx.getPlayer() == null) {
                return InteractionResult.PASS;
            }
            if (!ctx.getLevel().isClientSide) {
                final Pair<Schematic, BusConnectedCircuit> oldSchematic = tile.circuit;
                Pair<Schematic, BusConnectedCircuit> schematic = PCBStackItem.getSchematic(ctx.getItemInHand());
                if (schematic != null) {
                    tile.setCircuit(schematic.getFirst());
                    ctx.getItemInHand().shrink(1);
                } else {
                    tile.setCircuit(null);
                    tile.markBusDirty.run();
                }
                if (oldSchematic != null) {
                    ItemUtil.giveOrDrop(ctx.getPlayer(), PCBStackItem.forSchematic(oldSchematic.getFirst()));
                }
                TileUtil.markDirtyAndSync(tile);
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static SelectionShapes makeViewDesignInteraction(LogicCabinetTile tile) {
        final VoxelShape shape = ShapeUtils.createPixelRelative(
                15, 1, 4, 16, 11, 12
        );
        return new SingleShape(
                shape, ctx -> {
            final Player player = ctx.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer && tile.circuit != null) {
                LogicDesignContainer.makeProvider(tile.level, tile.worldPosition, true)
                        .open((ServerPlayer) player);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public Schematic getSchematic() {
        if (circuit != null) {
            return circuit.getFirst();
        } else {
            // should never happen(?)
            return new Schematic();
        }
    }

    @Override
    public void getExtraDrops(Consumer<ItemStack> dropper) {
        if (circuit != null) {
            dropper.accept(PCBStackItem.forSchematic(circuit.getFirst()));
        }
        RegistryObject<Item> clockItem = CEItems.CLOCK_GENERATORS.get(clock.getType().getRegistryName());
        if (clockItem != null) {
            dropper.accept(clockItem.get().getDefaultInstance());
        }
    }

    @Nullable
    @Override
    public LogicCabinetTile computeMasterTile(BlockState stateHere) {
        if (isUpper(stateHere)) {
            BlockEntity below = level.getBlockEntity(worldPosition.below());
            if (below instanceof LogicCabinetTile) {
                return (LogicCabinetTile) below;
            } else {
                return null;
            }
        } else {
            return this;
        }
    }
}
