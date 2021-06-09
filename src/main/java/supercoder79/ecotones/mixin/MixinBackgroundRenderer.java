package supercoder79.ecotones.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import supercoder79.ecotones.client.ClientSidedServerData;
import supercoder79.ecotones.client.FogHandler;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
    @Redirect(method = "applyFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;fogStart(F)V"))
    private static void applyEcotonesFancyFog(float start) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (ClientSidedServerData.isInEcotonesWorld) {
            float heightMultiplier = (float) (MathHelper.clamp(client.player.getY() - 128, 0, 4) / 4);

            long time = client.world.getTime();

            // TODO: biome humidity
            RenderSystem.setShaderFogStart(Math.min(start, (float) (start * FogHandler.multiplierFor(time))) * heightMultiplier);
        } else {
            RenderSystem.setShaderFogStart(start);
        }
    }
}
