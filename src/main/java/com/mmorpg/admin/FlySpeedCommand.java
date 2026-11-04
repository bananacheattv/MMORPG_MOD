package com.mmorpg.admin;

import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /flyspeed &lt;nombre&gt; : vitesse de vol de l'admin (1 = normale, max 10). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class FlySpeedCommand {
    private FlySpeedCommand() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("flyspeed")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("nombre", FloatArgumentType.floatArg(0F, 10F)).executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    float n = FloatArgumentType.getFloat(c, "nombre");
                    p.getAbilities().setFlyingSpeed(0.05F * n);
                    p.onUpdateAbilities();
                    c.getSource().sendSuccess(() -> Component.literal("§7Vitesse de vol : §f" + n), false);
                    return 1;
                })));
    }
}
