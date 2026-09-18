package dev.nhack.client.mixin;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.RenderEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Кадровый хук. {@code GameRenderer.render(DeltaTracker, boolean)} вызывается ровно один раз на
 * кадр, а камера настраивается внутри него ({@code updateCamera}) — значит HEAD это последний
 * момент, когда можно поменять поворот игрока и попасть в текущий кадр.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void nhack$preRender(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		EventBus.post(new RenderEvent(deltaTracker, renderLevel));
	}
}
