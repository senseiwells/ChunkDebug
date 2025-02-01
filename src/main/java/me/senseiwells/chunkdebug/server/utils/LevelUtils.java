package me.senseiwells.chunkdebug.server.utils;

import me.senseiwells.chunkdebug.server.mixins.ChunkMapAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;

public class LevelUtils {
    private LevelUtils() { }

    public static BlockableEventLoop<Runnable> getExecutor(ServerLevel level) {
        return ((ChunkMapAccessor) level.getChunkSource().chunkMap).getMainThreadExecutor();
    }

    public static boolean isSameThread(ServerLevel level) {
        return getExecutor(level).isSameThread();
    }

    public static void execute(ServerLevel level, Runnable runnable) {
        getExecutor(level).execute(runnable);
    }
}
