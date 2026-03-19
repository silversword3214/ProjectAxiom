package silversword.axiom.client.render.rendersystem.utils.postprocess;

import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.eventbus.AxiomEvent;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class PostProcessShaders {
    public static EntityShader ENTITY_OUTLINE;
    public static PostProcessShader STORAGE_OUTLINE;
    public static EntityShader CHAMS;

    // Tätä kutsutaan suoraan ObsidianMain.onInitializeClient() lopussa
    public static void init() {
        ENTITY_OUTLINE = new EntityOutlineShader();
        // STORAGE_OUTLINE = new StorageOutlineShader();
        CHAMS = new ChamsShader();
        AxiomInitialize.EVENT_BUS.subscribe(PostProcessShaders.class);
    }

    // PostProcessShaders.java
    public static void beginRender() {
        if (ENTITY_OUTLINE != null) ENTITY_OUTLINE.clearTexture();
        if (CHAMS != null) CHAMS.clearTexture();
        if (STORAGE_OUTLINE != null) STORAGE_OUTLINE.clearTexture();

        // Advance uniform buffers to next frame
        OutlineUniforms.flipFrame();
        PostProcessShader.flipFrame(); // for PostData
    }

    public static void submitEntityVertices() {
        if (ENTITY_OUTLINE != null) ENTITY_OUTLINE.submitVertices();
        if (CHAMS != null) CHAMS.submitVertices();
    }

    @AxiomEvent
    public static void onRender2D(Render2DEvent event) {
        if (ENTITY_OUTLINE != null) ENTITY_OUTLINE.render();
        if (CHAMS != null) CHAMS.render();
        if (STORAGE_OUTLINE != null) STORAGE_OUTLINE.render();

    }


    public static void onResized(int width, int height) {
        if (mc == null) return;
        if (ENTITY_OUTLINE != null) ENTITY_OUTLINE.onResized(width, height);
        if (CHAMS != null) CHAMS.onResized(width, height);
        if (STORAGE_OUTLINE != null) STORAGE_OUTLINE.onResized(width, height);
    }
}