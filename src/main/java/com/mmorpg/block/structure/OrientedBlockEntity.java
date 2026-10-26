package com.mmorpg.block.structure;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Map;

/** A fixed absolute yaw, saved and synchronized independently from cardinal block states. */
public class OrientedBlockEntity extends BlockEntity {
    public float yaw=Float.NaN;
    private Map<BlockPos,VoxelShape> shapes;
    protected OrientedBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state);}
    public void setYaw(float value) {yaw=net.minecraft.util.Mth.wrapDegrees(value);shapes=null;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public Map<BlockPos,VoxelShape> shapes() {
        if(shapes==null)shapes=FreeOrientation.shapes(getBlockState().getBlock(),yaw);
        return shapes;
    }
    @Override protected void saveAdditional(ValueOutput out) {super.saveAdditional(out);if(Float.isFinite(yaw))out.putFloat("placementYaw",yaw);}
    @Override protected void loadAdditional(ValueInput in) {super.loadAdditional(in);yaw=in.getFloatOr("placementYaw",Float.NaN);shapes=null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {return saveWithoutMetadata(registries);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
}
