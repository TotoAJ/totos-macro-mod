package totoaj.totosmacromod.event;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
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

    private static KeyMapping maceToggleKey;
    private static KeyMapping launchToggleKey;
    private static KeyMapping pearlLaunchKey;
    private static KeyMapping breachMaceKey;
    private static KeyMapping densityMaceKey;

    private static TimingState maceState = new TimingState();
    private static TimingState launchState = new TimingState();

    private static ItemStack pearlReference = new ItemStack(Items.ENDER_PEARL);
    private static ItemStack chargeReference = new ItemStack(Items.WIND_CHARGE);

    private static boolean autoMace = true;
    private static boolean autoLaunch = true;

    private static int swingTimer = 0;
    private static int launchTimer = 0;
    private static int previousSlot;

    public static void registerKeyInputs() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            if (client.player.swingingArm == null)
                client.player.swingingArm = client.player.getUsedItemHand();

            KeyMapping attackKey = client.options.keyAttack;

            if (maceToggleKey.consumeClick()) {
                autoMace = !autoMace;
                client.player.displayClientMessage(
                        Component.literal("Auto Mace: " + autoMace), true);
            }

            if (autoMace) {
                if (attackKey.isDown() && maceState.equals(State.IDLE)) {
                    if (client.crosshairPickEntity != null) {

                        previousSlot = client.player.getInventory().getSelectedSlot();

                        Component breachKey = breachMaceKey.getTranslatedKeyMessage();
                        Component densityKey = densityMaceKey.getTranslatedKeyMessage();

                        int breachSlot = Integer.parseInt(breachKey.getString()) - 1;
                        int densitySlot = Integer.parseInt(densityKey.getString()) - 1;

                        if (client.player.fallDistance < 6.5) {
                            if (breachSlot >= 0 && breachSlot <= 8) {
                                client.player.getInventory().setSelectedSlot(breachSlot);
                                maceState.next();
                                swingTimer = 0;
                            }
                        } else {
                            if (densitySlot >= 0 && densitySlot <= 8) {
                                client.player.getInventory().setSelectedSlot(densitySlot);
                                maceState.next();
                                swingTimer = 0;
                            }
                        }
                    }
                }

                if (attackKey.isDown() && maceState.equals(State.USING)) {
                    if (swingTimer >= 1) {
                        client.player.getInventory().setSelectedSlot(previousSlot);
                        maceState.next();
                    }
                    swingTimer++;
                }

                if (!attackKey.isDown() && maceState.equals(State.RESET)) {
                    maceState.next();
                }
            }

            if (launchToggleKey.consumeClick()) {
                autoLaunch = !autoLaunch;
                client.player.displayClientMessage(
                        Component.literal("Auto Pearl Launch: " + autoLaunch), true);
            }

            if (autoLaunch && pearlLaunchKey.isDown() && launchState.equals(State.IDLE)) {
                int pearlSlot = client.player.getInventory().findSlotMatchingItem(pearlReference);
                int windChargeSlot = client.player.getInventory().findSlotMatchingItem(chargeReference);

                previousSlot = client.player.getInventory().getSelectedSlot();

                if (pearlSlot > -1 && pearlSlot < 9 && windChargeSlot > -1 && windChargeSlot < 9) {
                    client.player.forceSetRotation(0.0f, true, -90.0f, false);

                    client.player.getInventory().setSelectedSlot(pearlSlot);

                    client.gameMode.useItem(client.player, client.player.swingingArm);

                    launchState.next();
                }
            } else if (pearlLaunchKey.isDown() && launchState.equals(State.USING)) {
                int windChargeSlot = client.player.getInventory().findSlotMatchingItem(chargeReference);

                client.player.getInventory().setSelectedSlot(windChargeSlot);

                client.gameMode.useItem(client.player, client.player.swingingArm);

                launchState.next();
                launchTimer = 0;
            } else if (!pearlLaunchKey.isDown() && launchState.equals(State.RESET)) {
                if (launchTimer >= 5) {
                    client.player.getInventory().setSelectedSlot(previousSlot);

                    launchState.next();
                }

                launchTimer++;
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
                GLFW.GLFW_KEY_3,
                MACRO_CATEGORY));

        breachMaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KEY_BREACH_MACE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_4,
                MACRO_CATEGORY));
    }
}
