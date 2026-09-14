package com.formlesslab.ae2additions.client.notification;

import ae2.api.client.CraftingJobState;
import ae2.api.client.CraftingJobStateEvent;
import com.formlesslab.ae2additions.init.Configurations;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Turns AE2's client-side crafting job notifications into Windows shell notifications.
 */
@SideOnly(Side.CLIENT)
public final class CraftingJobNotificationHandler {
    public static final CraftingJobNotificationHandler INSTANCE = new CraftingJobNotificationHandler();

    private CraftingJobNotificationHandler() {
    }

    @SubscribeEvent
    public void onCraftingJobStateChanged(CraftingJobStateEvent event) {
        if (event.getState() == CraftingJobState.FINISHED && Configurations.CLIENT.craftingJobSystemNotifications) {
            WindowsCraftingNotification.showWhenUnfocused(event.getWhat(), event.getRequestedAmount());
        }
    }
}
