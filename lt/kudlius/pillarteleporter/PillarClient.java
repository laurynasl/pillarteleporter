package lt.kudlius.pillarteleporter;

import net.minecraftforge.client.MinecraftForgeClient;

public class PillarClient extends PillarProxy {

    @Override
    public void registerRenderInformation() {
        MinecraftForgeClient.preloadTexture("/lt/kudlius/pillarteleporter/items.png");
    }
}
