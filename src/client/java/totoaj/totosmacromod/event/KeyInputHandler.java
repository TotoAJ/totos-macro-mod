package totoaj.totosmacromod.event;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import totoaj.totosmacromod.TotosMacroMod;

public class KeyInputHandler {
    private static final KeyMapping.Category MACRO_CATEGORY = KeyMapping.Category
            .register(ResourceLocation.fromNamespaceAndPath(TotosMacroMod.MOD_ID,
                    "macros"));
    private static final String KEY_TOGGLE_MACE = "key." + TotosMacroMod.MOD_ID + ".toggle_mace";
    private static final String KEY_BREACH_MACE = "key." + TotosMacroMod.MOD_ID + ".breach_mace";
    private static final String KEY_DENSITY_MACE = "key." + TotosMacroMod.MOD_ID + ".density_mace";

    private static KeyMapping maceToggleKey;
    private static KeyMapping breachMaceKey;
    private static KeyMapping densityMaceKey;

    private static TimingState state = TimingState.IDLE;

    private static boolean autoMace = true;

    private static int previousSlot;

    public static void registerKeyInputs() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            KeyMapping attackKey = client.options.keyAttack;

            if (maceToggleKey.consumeClick()) {
                autoMace = !autoMace;
                client.player.displayClientMessage(
                        Component.literal("Auto Mace: " + autoMace), true);
            }

            if (autoMace && state == TimingState.IDLE) {
                if (client.crosshairPickEntity != null) {

                    previousSlot = client.player.getInventory().getSelectedSlot();

                    int breachSlot = Integer.parseInt(
                            breachMaceKey.saveString().split("\\.")[2]) - 1;
                    int densitySlot = Integer.parseInt(
                            densityMaceKey.saveString().split("\\.")[2]) - 1;

                    if (client.player.fallDistance < 4.85) {
                        if (breachSlot >= 0 && breachSlot <= 8) {
                            client.player.getInventory().setSelectedSlot(breachSlot);
                            state = TimingState.SWING;
                        }
                    } else {
                        if (densitySlot >= 0 && densitySlot <= 8) {
                            client.player.getInventory().setSelectedSlot(densitySlot);
                            state = TimingState.SWING;
                        }
                    }
                }
            }

            if (state == TimingState.SWING) {
                client.player.getInventory().setSelectedSlot(previousSlot);
                state = TimingState.RESET;
            }

            if (state == TimingState.RESET) {
                if (!attackKey.isDown()) {
                    state = TimingState.IDLE;
                }
            }
        });
    }

    public static void register() {
        maceToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_MACE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                MACRO_CATEGORY));

        breachMaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_BREACH_MACE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_4,
                MACRO_CATEGORY));

        densityMaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_DENSITY_MACE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_3,
                MACRO_CATEGORY));
    }
}
