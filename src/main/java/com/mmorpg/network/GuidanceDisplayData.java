package com.mmorpg.network;

import net.minecraft.nbt.CompoundTag;
import com.mojang.math.Transformation;
import org.joml.*;

/** Complete vanilla display transformation: the codec requires all four components. */
public final class GuidanceDisplayData {
    public static CompoundTag create(float size){
        var data=new CompoundTag();data.putString("billboard","fixed");data.putString("item_display","fixed");data.putFloat("view_range",4);data.putFloat("width",1.2f);data.putFloat("height",.5f);data.putInt("teleport_duration",0);data.putInt("interpolation_duration",0);
        var light=new CompoundTag();light.putInt("block",15);light.putInt("sky",15);data.put("brightness",light);
        data.store("transformation",Transformation.EXTENDED_CODEC,new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(size),new Quaternionf()));return data;
    }
}
