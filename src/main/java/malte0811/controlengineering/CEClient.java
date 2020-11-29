package malte0811.controlengineering;

import malte0811.controlengineering.blocks.BlockRenderLayers;
import malte0811.controlengineering.controlpanels.renders.ComponentRenderers;
import malte0811.controlengineering.gui.ContainerScreenManager;
import malte0811.controlengineering.render.PanelCNCRenderer;
import malte0811.controlengineering.render.PanelRenderer;
import malte0811.controlengineering.tiles.CETileEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ControlEngineering.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CEClient {
    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent ev) {
        ComponentRenderers.init();
        ClientRegistry.bindTileEntityRenderer(CETileEntities.CONTROL_PANEL.get(), PanelRenderer::new);
        ClientRegistry.bindTileEntityRenderer(CETileEntities.PANEL_CNC.get(), PanelCNCRenderer::new);
        ContainerScreenManager.registerScreens();
        BlockRenderLayers.init();
    }
}
