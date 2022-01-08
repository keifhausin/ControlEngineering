package malte0811.controlengineering.blocks.tape;

import malte0811.controlengineering.blockentity.CEBlockEntities;
import malte0811.controlengineering.blockentity.tape.SequencerBlockEntity;
import malte0811.controlengineering.blocks.CEBlock;
import malte0811.controlengineering.blocks.placement.BlockPropertyPlacement;
import malte0811.controlengineering.blocks.shapes.FromBlockFunction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SequencerBlock extends CEBlock<Direction> {
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SequencerBlock() {
        super(
                defaultPropertiesNotSolid(),
                BlockPropertyPlacement.horizontal(FACING),
                //TODO?
                FromBlockFunction.constant(Shapes.block()),
                CEBlockEntities.SEQUENCER
        );
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @Nonnull Level pLevel, @Nonnull BlockState pState, @Nonnull BlockEntityType<T> pBlockEntityType
    ) {
        if (!pLevel.isClientSide()) {
            return createTickerHelper(pBlockEntityType, CEBlockEntities.SEQUENCER, SequencerBlockEntity::tick);
        }
        return super.getTicker(pLevel, pState, pBlockEntityType);
    }
}
