package me.senseiwells.chunkdebug.server.compat;

import com.google.common.collect.HashMultimap;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ChunkDebugMixinConfig implements IMixinConfigPlugin {
	private static final String MIXIN_COMPAT = "me.senseiwells.chunkdebug.server.mixins.compat.";

	private static final HashMultimap<String, String> INCOMPATIBLE = HashMultimap.create();

	static {
		INCOMPATIBLE.put("me.senseiwells.chunkdebug.server.mixins.TickingTrackerMixin", "c2me");
		INCOMPATIBLE.put("me.senseiwells.chunkdebug.server.mixins.ChunkMapMixin", "c2me");
	}

	@Override
	public void onLoad(String mixinPackage) {

	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith(MIXIN_COMPAT)) {
			String modIdWithName = mixinClassName.substring(MIXIN_COMPAT.length());
			int index = modIdWithName.indexOf('.');
			String modId = index < 0 ? modIdWithName : modIdWithName.substring(0, index);
			return FabricLoader.getInstance().isModLoaded(modId);
		}
		for (String modId : INCOMPATIBLE.get(mixinClassName)) {
			if (FabricLoader.getInstance().isModLoaded(modId)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(
		String targetClassName,
		ClassNode targetClass,
		String mixinClassName,
		IMixinInfo mixinInfo
	) {

	}

	@Override
	public void postApply(
		String targetClassName,
		ClassNode targetClass,
		String mixinClassName,
		IMixinInfo mixinInfo
	) {

	}
}
