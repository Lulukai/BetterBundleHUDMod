package betterbundle.mixin;

import betterbundle.gui.BundleCategory;
import betterbundle.gui.BundlePanelRenderer;
import betterbundle.gui.BundleWithdrawal;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (BundleWithdrawal.busy()) { cir.setReturnValue(true); return; }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        double mouseX = event.x();
        double mouseY = event.y();
        int bx = self.leftPos + self.imageWidth;
        int by = self.topPos + 5;
        if (mouseX >= bx && mouseX < bx + 20 && mouseY >= by && mouseY < by + 20) {
            BundlePanelRenderer.toggleVisible();
            cir.setReturnValue(true);
            return;
        }
        if (BundlePanelRenderer.isEffectivelyVisible()) {
            BundleCategory category = BundlePanelRenderer.getCategoryAt(mouseX, mouseY, self.leftPos, self.topPos, self.imageHeight);
            if (category != null) {
                BundlePanelRenderer.currentCategory = category;
                BundlePanelRenderer.scrollToTop();
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (BundleWithdrawal.busy() && !event.isEscape()) cir.setReturnValue(true);
    }
}
