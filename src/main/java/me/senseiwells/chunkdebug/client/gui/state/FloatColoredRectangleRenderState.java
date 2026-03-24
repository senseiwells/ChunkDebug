package me.senseiwells.chunkdebug.client.gui.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record FloatColoredRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float minX,
    float minY,
    float maxX,
    float maxY,
    int col1,
    int col2,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public FloatColoredRectangleRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float minX,
        float minY,
        float maxX,
        float maxY,
        int col1,
        int col2,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, minX, minY, maxX, maxY, col1, col2, scissorArea, getBounds(minX, minY, maxX, maxY, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose, this.minX, this.minY).setColor(this.col1);
        vertexConsumer.addVertexWith2DPose(this.pose, this.minX, this.maxY).setColor(this.col2);
        vertexConsumer.addVertexWith2DPose(this.pose, this.maxX, this.maxY).setColor(this.col2);
        vertexConsumer.addVertexWith2DPose(this.pose, this.maxX, this.minY).setColor(this.col1);
    }

    @Nullable
    static ScreenRectangle getBounds(
        float minX,
        float minY,
        float maxX,
        float maxY,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissor
    ) {
        ScreenRectangle bounds = new ScreenRectangle(
            (int) minX, (int) minY, Mth.ceil(maxX - minX), Mth.ceil(maxY - minY)
        ).transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(bounds) : bounds;
    }
}
