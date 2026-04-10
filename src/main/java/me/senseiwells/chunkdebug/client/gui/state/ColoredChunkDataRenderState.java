package me.senseiwells.chunkdebug.client.gui.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ColoredChunkDataRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    Int2ObjectMap<List<ChunkPos>> chunks,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredChunkDataRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        Int2ObjectMap<List<ChunkPos>> chunks,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, chunks, scissorArea, getBounds(chunks, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        for (Int2ObjectMap.Entry<List<ChunkPos>> chunks : this.chunks.int2ObjectEntrySet()) {
            int color = chunks.getIntKey();
            for (ChunkPos pos : chunks.getValue()) {
                int minX = pos.x(), maxX = minX + 1;
                int minY = pos.z(), maxY = minY + 1;
                vertexConsumer.addVertexWith2DPose(this.pose, minX, minY).setColor(color);
                vertexConsumer.addVertexWith2DPose(this.pose, minX, maxY).setColor(color);
                vertexConsumer.addVertexWith2DPose(this.pose, maxX, maxY).setColor(color);
                vertexConsumer.addVertexWith2DPose(this.pose, maxX, minY).setColor(color);
            }
        }
    }

    @Nullable
    private static ScreenRectangle getBounds(
        Int2ObjectMap<List<ChunkPos>> chunks,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissor
    ) {
        if (chunks.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (List<ChunkPos> entry : chunks.values()) {
            for (ChunkPos pos : entry) {
                minX = Math.min(minX, pos.x());
                maxX = Math.max(maxX, pos.x());
                minY = Math.min(minY, pos.z());
                maxY = Math.max(maxY, pos.z());
            }
        }

        ScreenRectangle bounds = new ScreenRectangle(
            minX, minY, maxX - minX, maxY - minY
        ).transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(bounds) : bounds;
    }
}
