package betterbundle;

import betterbundle.gui.BundleCategory;
import betterbundle.gui.BundleWithdrawal;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.ClientModInitializer;

public class BetterBundleMod implements ClientModInitializer {
    public static final String MOD_ID = "better-bundle";

    @Override
    public void onInitializeClient() {
        BundleCategory.registerCategoryItems();
        ClientTickEvents.END_CLIENT_TICK.register(client -> BundleWithdrawal.tick());
    }
}
