package silversword.axiom.client.modules.render.blockesp;

import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;

public class BlockData {
    public ShapeModeEnum shapeMode;
    public SettingColor lineColor;
    public SettingColor sideColor;

    public boolean tracer;
    public SettingColor tracerColor;

    public BlockData(ShapeModeEnum shapeMode, SettingColor lineColor, SettingColor sideColor, boolean tracer, SettingColor tracerColor) {
        this.shapeMode = shapeMode;
        this.lineColor = lineColor;
        this.sideColor = sideColor;
        this.tracer = tracer;
        this.tracerColor = tracerColor;
    }

    public void tickRainbow() {
        lineColor.getCurrentColor();
        sideColor.getCurrentColor();
        tracerColor.getCurrentColor();
    }

    public BlockData copy() {
        return new BlockData(
                shapeMode,
                lineColor.copy(),
                sideColor.copy(),
                tracer,
                tracerColor.copy()
        );
    }
}