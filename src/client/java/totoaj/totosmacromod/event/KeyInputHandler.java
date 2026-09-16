package totoaj.totosmacromod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.lwjgl.glfw.GLFW;
import totoaj.totosmacromod.TotosMacroMod;
import totoaj.totosmacromod.event.TimingState.State;
import totoaj.totosmacromod.mixin.client.MinecraftClientAccessor;

import java.util.Objects;

public class KeyInputHandler {
    private static final KeyMapping.Category MACRO_CATEGORY = KeyMapping.Category
            .register(Identifier.fromNamespaceAndPath(TotosMacroMod.MOD_ID, "macros"));
    private static final String KEY_TOGGLE_MACE = "key." + TotosMacroMod.MOD_ID + ".toggle_mace";
    private static final String KEY_TOGGLE_PEARL = "key." + TotosMacroMod.MOD_ID + ".toggle_pearl";
    private static final String KEY_TOGGLE_SPEAR = "key." + TotosMacroMod.MOD_ID + ".toggle_spear";
    private static final String KEY_PEARL_LAUNCH = "key." + TotosMacroMod.MOD_ID + ".pearl_launch";
    private static final String KEY_SPEAR_LAUNCH = "key." + TotosMacroMod.MOD_ID + ".spear_launch";

    private static final float DENSITY_THRESHOLD = 6.5F;

    private static KeyMapping maceToggleKey;
    private static KeyMapping pearlToggleKey;
    private static KeyMapping spearToggleKey;
    private static KeyMapping pearlLaunchKey;
    private static KeyMapping spearLaunchKey;

    private static final TimingState maceMacroState = new TimingState();
    private static final TimingState pearlMacroState = new TimingState();
    private static final TimingState spearMacroState = new TimingState();

    private static boolean maceEnabled = false;
    private static boolean pearlEnabled = false;
    private static boolean spearEnabled = false;

    private static int previousSlot;
    private static int cachedWindChargeSlot;
    private static int cachedSpearSlot;
    private static float previousPitch;

    public static void registerKeyInputs() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            handleMace(client);
            handlePearl(client);
            handleSpear(client);
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

        if (gameMode == null) return;

        switch (maceMacroState.getState()) {
            case State.IDLE:
                if (maceEnabled && entity instanceof LivingEntity && attackKey.isDown()) {
                    previousSlot = playerInv.getSelectedSlot();

                    int breachMaceSlot = findSlotMatchingEnchantment(client, Enchantments.BREACH);
                    int densityMaceSlot = findSlotMatchingEnchantment(client, Enchantments.DENSITY);
                    int axeSlot = findHotbarSlot(playerInv, "axe");

                    ItemStack entityItem = entity.getWeaponItem();
                    if (!entityItem.isEmpty() && entityItem.is(Items.SHIELD) && axeSlot != -1) {
                        playerInv.setSelectedSlot(axeSlot);
                        gameMode.attack(player, entity);
                    }

                    int optimalMace = player.fallDistance < DENSITY_THRESHOLD ? breachMaceSlot : densityMaceSlot;

                    if (breachMaceSlot + densityMaceSlot == -2) {
                        optimalMace = findHotbarSlot(playerInv, Items.MACE);
                    }

                    if (isSlot(optimalMace) && optimalMace != -1) {
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

    private static void handlePearl(Minecraft client) {
        LocalPlayer player = client.player;
        assert player != null;
        Inventory playerInv = player.getInventory();
        MultiPlayerGameMode gameMode = client.gameMode;

        if (gameMode == null) return;

        switch (pearlMacroState.getState()) {
            case State.IDLE:
                if (pearlEnabled && pearlLaunchKey.isDown()) {
                    int pearlSlot = findHotbarSlot(playerInv, Items.ENDER_PEARL);
                    cachedWindChargeSlot = findHotbarSlot(playerInv, Items.WIND_CHARGE);

                    if (pearlSlot < 0 || cachedWindChargeSlot < 0) return;

                    if (!Objects.requireNonNull(playerInv.getSlot(cachedWindChargeSlot)).get().is(Items.WIND_CHARGE)) {
                        cachedWindChargeSlot = findHotbarSlot(playerInv, Items.WIND_CHARGE);
                    }

                    previousSlot = playerInv.getSelectedSlot();

                    if (isSlot(pearlSlot) && isSlot(cachedWindChargeSlot)) {
                        previousPitch = player.getXRot();

                        player.forceSetRotation(0.0f, true, -90.0f, false);

                        playerInv.setSelectedSlot(pearlSlot);

                        gameMode.useItem(player, player.getUsedItemHand());

                        pearlMacroState.next();
                    }
                }
                break;

            case State.USING:
                playerInv.setSelectedSlot(cachedWindChargeSlot);

                gameMode.useItem(player, player.getUsedItemHand());

                player.forceSetRotation(0.0f, true, previousPitch, false);

                playerInv.setSelectedSlot(previousSlot);

                pearlMacroState.next();
                break;

            case State.RESET:
                if (!pearlLaunchKey.isDown()) {
                    if (pearlMacroState.getTime() >= 5) {
                        pearlMacroState.advance();
                    }

                    pearlMacroState.tick();
                }
                break;
        }
    }

    private static void handleSpear(Minecraft client) {
        LocalPlayer player = client.player;
        assert player != null;
        Inventory playerInv = player.getInventory();
        MultiPlayerGameMode gameMode = client.gameMode;

        if (gameMode == null) return;

        switch (spearMacroState.getState()) {
            case IDLE:
                if (spearEnabled && spearLaunchKey.isDown()) {
                    cachedWindChargeSlot = findHotbarSlot(playerInv, Items.WIND_CHARGE);
                    cachedSpearSlot = findHotbarSlot(playerInv, Items.NETHERITE_SPEAR);

                    if (cachedWindChargeSlot < 0) return;
                    if (cachedSpearSlot < 0) {
                        cachedSpearSlot = findHotbarSlot(playerInv, Items.DIAMOND_SPEAR);
                        if (cachedSpearSlot < 0) return;
                    }

                    previousSlot = playerInv.getSelectedSlot();

                    if (isSlot(cachedWindChargeSlot) && isSlot(cachedSpearSlot)) {
                        playerInv.setSelectedSlot(cachedWindChargeSlot);

                        previousPitch = player.getXRot();

                        player.forceSetRotation(0.0f, true, 90.0f, false);

                        player.jumpFromGround();

                        gameMode.useItem(player, player.getUsedItemHand());

                        spearMacroState.next();
                    }
                }
                break;

            case USING:
                player.forceSetRotation(0.0f, true, previousPitch, false);

                spearMacroState.advance();
                break;

            case RESET:
                if (spearMacroState.getTime() >= 8) {
                    playerInv.setSelectedSlot(cachedSpearSlot);

                    ((MinecraftClientAccessor) client).callStartAttack();

                    playerInv.setSelectedSlot(previousSlot);

                    spearMacroState.advance();
                }

                spearMacroState.tick();
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

        if (pearlToggleKey.consumeClick()) {
            pearlEnabled = !pearlEnabled;
            player.sendOverlayMessage(Component.literal("Auto Pearl Launch: " + pearlEnabled));
        }

        if (spearToggleKey.consumeClick()) {
            spearEnabled = !spearEnabled;
            player.sendOverlayMessage(Component.literal("Auto Spear Launch: " + spearEnabled));
        }
    }

    private static boolean isSlot(int slot) {
        return (slot >= 0 && slot <= 8);
    }

    private static int findHotbarSlot(Inventory inventory, Item item) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).is(item)) {
                return i;
            }
        }

        return -1;
    }

    private static int findHotbarSlot(Inventory inventory, String item) {
        for (int i = 0; i < 9; i++) {
            if (isItem(inventory.getItem(i), item)) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isItem(ItemStack item, String type) {
        return BuiltInRegistries.ITEM.getKey(item.getItem()).toString().toLowerCase().contains(type);
    }

    private static int findSlotMatchingEnchantment(Minecraft client, ResourceKey<Enchantment> enchant) {
        int slot = -1;

        LocalPlayer player = client.player;
        assert player != null;
        Inventory inventory = player.getInventory();

        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);

            Holder<Enchantment> enchantmentHolder = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchant);

            if (EnchantmentHelper.getItemEnchantmentLevel(enchantmentHolder, item) > 0) {
                slot = i;
                break;
            }
        }

        return slot;
    }

    public static void register() {
        maceToggleKey = registerKey(KEY_TOGGLE_MACE, GLFW.GLFW_KEY_BACKSLASH);

        pearlToggleKey = registerKey(KEY_TOGGLE_PEARL, GLFW.GLFW_KEY_RIGHT_BRACKET);

        spearToggleKey = registerKey(KEY_TOGGLE_SPEAR, GLFW.GLFW_KEY_LEFT_BRACKET);

        pearlLaunchKey = registerKey(KEY_PEARL_LAUNCH, GLFW.GLFW_KEY_R);

        spearLaunchKey = registerKey(KEY_SPEAR_LAUNCH, GLFW.GLFW_KEY_5);
    }

    private static KeyMapping registerKey(String name, int keybind) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                name,
                InputConstants.Type.KEYSYM,
                keybind,
                MACRO_CATEGORY));
    }
}
