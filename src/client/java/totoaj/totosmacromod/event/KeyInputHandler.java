package totoaj.totosmacromod.event;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import totoaj.totosmacromod.TotosMacroMod;
import totoaj.totosmacromod.event.TimingState.State;

public class KeyInputHandler {
    private static final KeyMapping.Category MACRO_CATEGORY = KeyMapping.Category
            .register(ResourceLocation.fromNamespaceAndPath(TotosMacroMod.MOD_ID,
                    "macros"));
    private static final String KEY_TOGGLE_MACE = "key." + TotosMacroMod.MOD_ID + ".toggle_mace";
    private static final String KEY_TOGGLE_LAUNCH = "key." + TotosMacroMod.MOD_ID + ".toggle_launch";
    private static final String KEY_PEARL_LAUNCH = "key." + TotosMacroMod.MOD_ID + ".pearl_launch";
    private static final String KEY_DENSITY_MACE = "key." + TotosMacroMod.MOD_ID + ".density_mace";
    private static final String KEY_BREACH_MACE = "key." + TotosMacroMod.MOD_ID + ".breach_mace";
    private static final String KEY_AXE = "key." + TotosMacroMod.MOD_ID + "axe";

    private static KeyMapping maceToggleKey;
    private static KeyMapping launchToggleKey;
    private static KeyMapping pearlLaunchKey;
    private static KeyMapping breachMaceKey;
    private static KeyMapping densityMaceKey;
    private static KeyMapping axeKey;

    private static TimingState maceState = new TimingState();
    private static TimingState launchState = new TimingState();

    private static ItemStack pearlReference = new ItemStack(Items.ENDER_PEARL);
    private static ItemStack chargeReference = new ItemStack(Items.WIND_CHARGE);

    private static boolean autoMace = false;
    private static boolean autoLaunch = false;

    private static int previousSlot;
    private static float previousAngle;

    public static void registerKeyInputs() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;
            Entity entity = client.crosshairPickEntity;

            KeyMapping attackKey = client.options.keyAttack;

            if (maceToggleKey.consumeClick()) {
                autoMace = !autoMace;
                client.player.displayClientMessage(
                        Component.literal("Auto Mace: " + autoMace), true);
            }

            if (autoMace && entity != null && attackKey.isDown() && maceState.getState() == State.IDLE) {
                previousSlot = client.player.getInventory().getSelectedSlot();

                Component breachKey = breachMaceKey.getTranslatedKeyMessage();
                Component densityKey = densityMaceKey.getTranslatedKeyMessage();
                Component axeSlotKey = axeKey.getTranslatedKeyMessage();

                int breachSlot = Integer.parseInt(breachKey.getString()) - 1;
                int densitySlot = Integer.parseInt(densityKey.getString()) - 1;
                int axeSlot = Integer.parseInt(axeSlotKey.getString()) - 1;

                if (entity.getPickResult() != null && entity.getPickResult().getItemName()
                        .equals(Component.literal("Shield"))) {
                    client.player.getInventory().setSelectedSlot(axeSlot);
                    client.gameMode.attack(client.player, entity);
                }

                if (client.player.fallDistance < 6.5) {
                    if (breachSlot >= 0 && breachSlot <= 8) {
                        client.player.getInventory().setSelectedSlot(breachSlot);
                        client.gameMode.attack(client.player, entity);
                        maceState.next();
                    }
                } else {
                    if (densitySlot >= 0 && densitySlot <= 8) {
                        client.player.getInventory().setSelectedSlot(densitySlot);
                        client.gameMode.attack(client.player, entity);
                        maceState.next();
                    }
                }
            }

            if (maceState.getState() == State.USING) {
                if (maceState.getTime() >= 1) {
                    client.player.getInventory().setSelectedSlot(previousSlot);
                    maceState.next();
                    maceState.resetTimer();
                }
                maceState.tick();
            }

            if (!attackKey.isDown() && maceState.getState() == State.RESET) {
                maceState.next();
            }

            if (launchToggleKey.consumeClick()) {
                autoLaunch = !autoLaunch;
                client.player.displayClientMessage(
                        Component.literal("Auto Pearl Launch: " + autoLaunch), true);
            }

            if (autoLaunch && pearlLaunchKey.isDown() && launchState.getState() == State.IDLE) {
                int pearlSlot = client.player.getInventory().findSlotMatchingItem(pearlReference);
                int windChargeSlot = client.player.getInventory().findSlotMatchingItem(chargeReference);

                previousSlot = client.player.getInventory().getSelectedSlot();

                if (pearlSlot > -1 && pearlSlot < 9 && windChargeSlot > -1 && windChargeSlot < 9) {
                    previousAngle = client.player.getXRot();

                    client.player.forceSetRotation(0.0f, true, -90.0f, false);

                    client.player.getInventory().setSelectedSlot(pearlSlot);

                    client.gameMode.useItem(client.player, client.player.getUsedItemHand());

                    launchState.next();
                }
            } else if (launchState.getState() == State.USING) {
                int windChargeSlot = client.player.getInventory().findSlotMatchingItem(chargeReference);

                client.player.getInventory().setSelectedSlot(windChargeSlot);

                client.gameMode.useItem(client.player, client.player.getUsedItemHand());

                client.player.forceSetRotation(0.0f, true, previousAngle, false);

                client.player.getInventory().setSelectedSlot(previousSlot);

                launchState.next();
            } else if (!pearlLaunchKey.isDown() && launchState.getState() == State.RESET) {
                if (launchState.getTime() >= 5) {
                    launchState.next();
                    launchState.resetTimer();
                }

                launchState.tick();
            }
        });
    }

    public static void register() {
        maceToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_MACE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                MACRO_CATEGORY));

        launchToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_TOGGLE_LAUNCH,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                MACRO_CATEGORY));

        pearlLaunchKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_PEARL_LAUNCH,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                MACRO_CATEGORY));

        densityMaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_DENSITY_MACE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_8,
                MACRO_CATEGORY));

        breachMaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_BREACH_MACE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_9,
                MACRO_CATEGORY));

        axeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_AXE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_7,
                MACRO_CATEGORY));
    }
}
