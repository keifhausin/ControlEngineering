package malte0811.controlengineering.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class SubTexture {
    private final ResourceLocation mainTexture;
    private final int minU;
    private final int minV;
    private final int maxU;
    private final int maxV;
    private final int mainSize;

    public SubTexture(ResourceLocation mainTexture, int minU, int minV, int maxU, int maxV) {
        this(mainTexture, minU, minV, maxU, maxV, 256);
    }

    public SubTexture(ResourceLocation mainTexture, int minU, int minV, int maxU, int maxV, int mainSize) {
        this.mainTexture = mainTexture;
        this.minU = minU;
        this.minV = minV;
        this.maxU = maxU;
        this.maxV = maxV;
        this.mainSize = mainSize;
    }

    public int getMaxV() {
        return maxV;
    }

    public int getMaxU() {
        return maxU;
    }

    public int getMinV() {
        return minV;
    }

    public int getMinU() {
        return minU;
    }

    public int getHeight() {
        return maxV - minV;
    }

    public int getWidth() {
        return maxU - minU;
    }

    public ResourceLocation getMainTexture() {
        return mainTexture;
    }

    public ImageButton createButton(int x, int y, Button.OnPress onPress) {
        return new ImageButton(
                x, y,
                getWidth(), getHeight(), getMinU(), getMinV(),
                0, getMainTexture(),
                onPress
        );
    }

    public void blit(PoseStack transform, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getMainTexture());
        Screen.blit(
                transform, x, y, getWidth(), getHeight(), getMinU(), getMinV(), getWidth(), getHeight(),
                mainSize, mainSize
        );
    }
}
