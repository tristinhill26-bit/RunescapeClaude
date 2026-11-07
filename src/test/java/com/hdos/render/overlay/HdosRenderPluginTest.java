package com.hdos.render.overlay;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HdosRenderPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(HdosRenderPlugin.class);
        RuneLite.main(args);
    }
}
