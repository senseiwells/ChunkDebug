package me.senseiwells.chunkdebug.client.gui.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import static me.senseiwells.chunkdebug.client.gui.state.FloatColoredRectangleRenderState.getBounds;

public record FloatColoredTriangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float minX,
    float minY,
    float maxX,
    float maxY,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public FloatColoredTriangleRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float minX,
        float minY,
        float maxX,
        float maxY,
        int color,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, minX, minY, maxX, maxY, color, scissorArea, getBounds(minX, minY, maxX, maxY, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose, this.minX, this.minY).setColor(this.color);
        vertexConsumer.addVertexWith2DPose(this.pose, this.minX, this.maxY).setColor(this.color);
        vertexConsumer.addVertexWith2DPose(this.pose, this.maxX, this.maxY - (this.maxY - this.minY) / 2).setColor(this.color);
        vertexConsumer.addVertexWith2DPose(this.pose, this.minX, this.minY).setColor(this.color);
    }
}
