package totoaj.totosmacromod.event;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import totoaj.totosmacromod.TotosMacroMod;
import totoaj.totosmacromod.event.TimingState.State;

public class KeyInputHandler {
    private static final KeyMapping.Category MACRO_CATEGORY = KeyMapping.Category
            .register(Identifier.fromNamespaceAndPath(TotosMacroMod.MOD_ID, "macros"));
    private static final String KEY_TOGGLE_MACE = "key." + TotosMacroMod.MOD_ID + ".toggle_mace";
    private static final String KEY_TOGGLE_LAUNCH = "key." + TotosMacroMod.MOD_ID + ".toggle_launch";
    private static final String KEY_PEARL_LAUNCH = "key." + TotosMacroMod.MOD_ID + ".pearl_launch";
    private static final String KEY_DENSITY_MACE = "key." + TotosMacroMod.MOD_ID + ".density_mace";
    private static final String KEY_BREACH_MACE = "key." + TotosMacroMod.MOD_ID + ".breach_mace";
    private static final String KEY_AXE = "key." + TotosMacroMod.MOD_ID + ".axe";

    private static final float DENSITY_THRESHOLD = 6.5F;

    private static KeyMapping maceToggleKey;
    private static KeyMapping launchToggleKey;
    private static KeyMapping pearlLaunchKey;
    private static KeyMapping breachMaceKey;
    private static KeyMapping densityMaceKey;
    private static KeyMapping axeKey;

    private static final TimingState maceMacroState = new TimingState();
    private static final TimingState launchMacroState = new TimingState();

    private static boolean maceEnabled = false;
    private static boolean launchEnabled = false;

    private static int previousSlot;
    private static int cachedWindChargeSlot;
    private static float previousPitch;

    public static void registerKeyInputs() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            handleMace(client);
            handleLaunch(client);
            toggleMacros(client);
        });
    }

    private static void handleMace(Minecraft client) {
        KeyMapping attackKey = client.options.keyAttack;
        Entity entity = client.crosshairPickEntity;
        LocalPlayer player = client.player;
        assert player != null;
        Inventory playerInv = player.getInventory();
        MultiPlayerGameMode gameMode = client.gameMode;
        assert gameMode != null;

        switch (maceMacroState.getState()) {
            case State.IDLE:
                if (maceEnabled && entity instanceof LivingEntity && attackKey.isDown()) {
                    previousSlot = playerInv.getSelectedSlot();

                    int breachSlot = getConfiguredSlot(breachMaceKey);
                    int densitySlot = getConfiguredSlot(densityMaceKey);
                    int axeSlot = getConfiguredSlot(axeKey);

                    ItemStack entityItem = entity.getWeaponItem();
                    if (!entityItem.isEmpty() && entityItem.is(Items.SHIELD)) {
                        playerInv.setSelectedSlot(axeSlot);
                        gameMode.attack(player, entity);
                    }

                    int optimalMace = player.fallDistance < DENSITY_THRESHOLD ? breachSlot : densitySlot;

                    if (isSlot(optimalMace)) {
                        playerInv.setSelectedSlot(optimalMace);
                        gameMode.attack(player, entity);
                        maceMacroState.next();
                    }
                }
                break;

            case State.USING:
                if (maceMacroState.getTime() >= 1) {
                    playerInv.setSelectedSlot(previousSlot);
                    maceMacroState.advance();
                }
                maceMacroState.tick();
                break;

            case State.RESET:
                if (!attackKey.isDown())
                    maceMacroState.next();
                break;
        }
    }

    private static void handleLaunch(Minecraft client) {
        LocalPlayer player = client.player;
        assert player != null;
        Inventory playerInv = player.getInventory();
        MultiPlayerGameMode gameMode = client.gameMode;
        assert gameMode != null;

        switch (launchMacroState.getState()) {
            case State.IDLE:
                if (launchEnabled && pearlLaunchKey.isDown()) {
                    int pearlSlot = findHotbarSlot(playerInv, Items.ENDER_PEARL);
                    cachedWindChargeSlot = findHotbarSlot(playerInv, Items.WIND_CHARGE);

                    previousSlot = playerInv.getSelectedSlot();

                    if (isSlot(pearlSlot) && isSlot(cachedWindChargeSlot)) {
                        previousPitch = player.getXRot();

                        player.forceSetRotation(0.0f, true, -90.0f, false);

                        playerInv.setSelectedSlot(pearlSlot);

                        gameMode.useItem(player, player.getUsedItemHand());

                        launchMacroState.next();
                    }
                }
                break;

            case State.USING:
                playerInv.setSelectedSlot(cachedWindChargeSlot);

                gameMode.useItem(player, player.getUsedItemHand());

                player.forceSetRotation(0.0f, true, previousPitch, false);

                playerInv.setSelectedSlot(previousSlot);

                launchMacroState.next();
                break;

            case State.RESET:
                if (!pearlLaunchKey.isDown()) {
                    if (launchMacroState.getTime() >= 5) {
                        launchMacroState.advance();
                    }

                    launchMacroState.tick();
                }
                break;
        }
    }

    private static void toggleMacros(Minecraft client) {
        LocalPlayer player = client.player;
        assert player != null;

        if (maceToggleKey.consumeClick()) {
            maceEnabled = !maceEnabled;
            player.sendOverlayMessage(Component.literal("Auto Mace: " + maceEnabled));
        }

        if (launchToggleKey.consumeClick()) {
            launchEnabled = !launchEnabled;
            player.sendOverlayMessage(Component.literal("Auto Pearl Launch: " + launchEnabled));
        }
    }

    private static boolean isSlot(int slot) {
        return (slot >= 0 && slot <= 8);
    }

    private static int getConfiguredSlot(KeyMapping key) {
        return Integer.parseInt(key.getTranslatedKeyMessage().getString()) - 1;
    }

    private static int findHotbarSlot(Inventory inventory, Item item) {
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).is(item)) {
                slot = i;
            }
        }

        return slot;
    }

    public static void register() {
        maceToggleKey = registerKey(KEY_TOGGLE_MACE, GLFW.GLFW_KEY_BACKSLASH);

        launchToggleKey = registerKey(KEY_TOGGLE_LAUNCH, GLFW.GLFW_KEY_RIGHT_BRACKET);

        pearlLaunchKey = registerKey(KEY_PEARL_LAUNCH, GLFW.GLFW_KEY_R);

        densityMaceKey = registerKey(KEY_DENSITY_MACE, GLFW.GLFW_KEY_8);

        breachMaceKey = registerKey(KEY_BREACH_MACE, GLFW.GLFW_KEY_9);

        axeKey = registerKey(KEY_AXE, GLFW.GLFW_KEY_7);
    }

    private static KeyMapping registerKey(String name, int keybind) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                name,
                InputConstants.Type.KEYSYM,
                keybind,
                MACRO_CATEGORY));
    }
}
