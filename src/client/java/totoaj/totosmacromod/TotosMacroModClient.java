package totoaj.totosmacromod;

import totoaj.totosmacromod.event.*;

import net.fabricmc.api.ClientModInitializer;

public class TotosMacroModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as
		// rendering.

		KeyInputHandler.register();
		KeyInputHandler.registerKeyInputs();
	}
}