// package me.senseiwells.chunkdebug.server.mixins.compat.c2me;
//
// import com.ishland.flowsched.scheduler.ItemHolder;
// import com.ishland.flowsched.scheduler.StatusAdvancingScheduler;
// import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.gen.Accessor;
//
// import java.util.concurrent.locks.StampedLock;
//
// @Mixin(value = StatusAdvancingScheduler.class, remap = false)
// public interface StatusAdvancingSchedulerAccessor<K, V, Ctx, UserData> {
// 	@Accessor
// 	StampedLock getItemsLock();
//
// 	@Accessor
// 	Object2ReferenceOpenHashMap<K, ItemHolder<K, V, Ctx, UserData>> getItems();
// }
