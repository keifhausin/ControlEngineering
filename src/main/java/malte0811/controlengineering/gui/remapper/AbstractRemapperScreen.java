package malte0811.controlengineering.gui.remapper;

import com.google.common.collect.Iterables;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import malte0811.controlengineering.ControlEngineering;
import malte0811.controlengineering.gui.SubTexture;
import malte0811.controlengineering.network.remapper.ClearMapping;
import malte0811.controlengineering.network.remapper.SetMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractRemapperScreen extends Screen implements MenuAccess<AbstractRemapperMenu> {
    protected static final ResourceLocation TEXTURE = new ResourceLocation(
            ControlEngineering.MODID, "textures/gui/rs_remapper.png"
    );
    protected static final int WIDTH = 165;
    protected static final int HEIGHT = 154;
    private static final int WIRE_COLOR = 0xffb66232;
    private static final SubTexture BACKGROUND = new SubTexture(TEXTURE, 0, 0, WIDTH, HEIGHT);
    private final AbstractRemapperMenu menu;
    private final List<ConnectionPoint> sourceConnectionPoints;
    private final List<ConnectionPoint> targetConnectionPoints;
    protected int leftPos;
    protected int topPos;
    @Nullable
    private ConnectionPoint fixedEndOfConnecting;

    public AbstractRemapperScreen(
            AbstractRemapperMenu menu,
            List<ConnectionPoint> sourceConnectionPoints,
            List<ConnectionPoint> targetConnectionPoints
    ) {
        super(TextComponent.EMPTY);
        this.menu = menu;
        this.sourceConnectionPoints = sourceConnectionPoints;
        this.targetConnectionPoints = targetConnectionPoints;
    }

    @Override
    public void render(@Nonnull PoseStack transform, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(transform);
        transform.pushPose();
        transform.translate(leftPos, topPos, 0);
        renderConnections(transform);
        if (fixedEndOfConnecting != null) {
            renderWireAtMouse(transform, fixedEndOfConnecting, mouseX - this.leftPos, mouseY - this.topPos);
        }
        transform.popPose();
        super.render(transform, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(@Nonnull PoseStack transform, int vOffset) {
        super.renderBackground(transform, vOffset);
        transform.pushPose();
        transform.translate(leftPos, topPos, vOffset);
        BACKGROUND.blit(transform, 0, 0);
        transform.popPose();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (width - WIDTH) / 2;
        this.topPos = (height - HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fixedEndOfConnecting != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            fixedEndOfConnecting = null;
            return true;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            var relativeX = (int) (mouseX - leftPos);
            var relativeY = (int) (mouseY - topPos);
            for (var connPoint : getAllConnectionPoints()) {
                if (connPoint.area().contains(relativeX, relativeY)) {
                    onClicked(connPoint);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onClicked(ConnectionPoint clicked) {
        if (fixedEndOfConnecting != null) {
            if (clicked.isMappingSource != fixedEndOfConnecting.isMappingSource) {
                var oldOtherEnd = getOtherEnd(clicked);
                var colorIndex = getIndexAtColor(clicked, fixedEndOfConnecting);
                var grayIndex = clicked.isMappingSource ? fixedEndOfConnecting.index : clicked.index;
                menu.processAndSend(new SetMapping(colorIndex, grayIndex));
                fixedEndOfConnecting = oldOtherEnd;
            } else {
                fixedEndOfConnecting = null;
            }
        } else {
            var otherEnd = getOtherEnd(clicked);
            if (otherEnd == null) {
                fixedEndOfConnecting = clicked;
            } else {
                menu.processAndSend(new ClearMapping(getIndexAtColor(clicked, otherEnd)));
                fixedEndOfConnecting = otherEnd;
            }
        }
    }

    private void renderConnections(PoseStack transform) {
        var mapping = menu.getMapping();
        for (int sourceIndex = 0; sourceIndex < mapping.length; ++sourceIndex) {
            var mappedTo = mapping[sourceIndex];
            if (mappedTo == AbstractRemapperMenu.NOT_MAPPED) {
                continue;
            }
            var sourceCP = sourceConnectionPoints.get(sourceIndex);
            var targetCP = targetConnectionPoints.get(mappedTo);
            sourceCP.sprite.blit(transform, sourceCP.area().getX(), sourceCP.area().getY());
            targetCP.sprite.blit(transform, targetCP.area().getX(), targetCP.area().getY());
            renderFullyConnectedWire(
                    transform, sourceCP.wireX, sourceCP.wireY, targetCP.wireX, targetCP.wireY
            );
        }
    }

    private void renderFullyConnectedWire(PoseStack transform, float xStart, float yStart, float xEnd, float yEnd) {
        final float wireRadius = 1;
        float deltaX = xEnd - xStart;
        float deltaY = yEnd - yStart;
        float cosAlpha = (float) (deltaX / Math.sqrt(deltaY * deltaY + deltaX * deltaX));
        float halfWireHeight = Math.abs(wireRadius / cosAlpha);

        renderWire(
                transform,
                xStart, yStart + halfWireHeight,
                xEnd, yEnd + halfWireHeight,
                xEnd, yEnd - halfWireHeight,
                xStart, yStart - halfWireHeight
        );
    }

    private void renderWireAtMouse(PoseStack transform, ConnectionPoint fixed, int mouseX, int mouseY) {
        var fixedY = fixed.wireY;
        var fixedX = fixed.wireX;
        Vec2 radius = new Vec2(fixedY - mouseY, mouseX - fixedX).normalized();

        renderWire(
                transform,
                fixedX + radius.x, fixedY + radius.y,
                mouseX + radius.x, mouseY + radius.y,
                mouseX - radius.x, mouseY - radius.y,
                fixedX - radius.x, fixedY - radius.y
        );
    }

    private void renderWire(
            PoseStack transform, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4
    ) {
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var matrix = transform.last().pose();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(matrix, x1, y1, 0.0F).color(WIRE_COLOR).endVertex();
        bufferbuilder.vertex(matrix, x2, y2, 0.0F).color(WIRE_COLOR).endVertex();
        bufferbuilder.vertex(matrix, x3, y3, 0.0F).color(WIRE_COLOR).endVertex();
        bufferbuilder.vertex(matrix, x4, y4, 0.0F).color(WIRE_COLOR).endVertex();
        bufferbuilder.end();
        BufferUploader.end(bufferbuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @Nonnull
    @Override
    public AbstractRemapperMenu getMenu() {
        return menu;
    }

    @Nullable
    private ConnectionPoint getOtherEnd(ConnectionPoint first) {
        int otherIndex;
        int invalid;
        var mapping = menu.getMapping();
        if (first.isMappingSource) {
            otherIndex = mapping[first.index];
            invalid = AbstractRemapperMenu.NOT_MAPPED;
        } else {
            otherIndex = ArrayUtils.indexOf(mapping, first.index);
            invalid = ArrayUtils.INDEX_NOT_FOUND;
        }
        if (otherIndex != invalid) {
            if (first.isMappingSource) {
                return targetConnectionPoints.get(otherIndex);
            } else {
                return sourceConnectionPoints.get(otherIndex);
            }
        } else {
            return null;
        }
    }

    private int getIndexAtColor(ConnectionPoint first, ConnectionPoint second) {
        return first.isMappingSource ? first.index : second.index;
    }

    private Iterable<ConnectionPoint> getAllConnectionPoints() {
        return Iterables.concat(sourceConnectionPoints, targetConnectionPoints);
    }

    protected record ConnectionPoint(
            boolean isMappingSource, int index, int wireX, int wireY, Rect2i area, SubTexture sprite
    ) {}
}
