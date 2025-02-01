package me.senseiwells.chunkdebug.server.utils;

import me.senseiwells.chunkdebug.server.mixins.LevelAccessor;
import net.minecraft.world.level.Level;

public class LevelUtils {
    private LevelUtils() { }

    public static boolean isSameThread(Level level) {
        return ((LevelAccessor) level).getThread() == Thread.currentThread();
    }
}
