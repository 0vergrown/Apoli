package dev.overgrown.apoli.compat.spellengine.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.compat.spellengine.power.SpellsPower;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.container.SpellContainerSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// apoli:spells with "always": Spell Engine only takes active spells from the spell item in the main hand. This adds the
// active spells of those powers to the list on both sides, whatever is held. When nothing held carries spells, they
// become the active container.
@Mixin(SpellContainerSource.class)
public abstract class SpellContainerSourceMixin {
    @WrapOperation(method = "update", at = @At(value = "NEW", target = "Lnet/spell_engine/internals/container/SpellContainerSource$Result;"))
    private static SpellContainerSource.Result apoli$alwaysSpells(SpellContainer active, List<Holder<Spell>> actives,
                                                                  List<Holder<Spell>> passives, List<Holder<Spell>> modifiers,
                                                                  List<SpellContainerSource.SourcedContainer> sources,
                                                                  Operation<SpellContainerSource.Result> original,
                                                                  @Local(argsOnly = true) Player player) {
        List<String> granted = null;
        for (Map.Entry<String, SpellContainer> entry : ((SpellContainerSource.Owner) player).serverSideSpellContainers().entrySet()) {
            if (!entry.getKey().startsWith(SpellsPower.ALWAYS_PREFIX) || entry.getValue().spell_ids() == null) continue;
            if (granted == null) granted = new ArrayList<>();
            for (String id : entry.getValue().spell_ids()) {
                if (!granted.contains(id)) granted.add(id);
            }
        }
        if (granted == null || granted.isEmpty()) return original.call(active, actives, passives, modifiers, sources);

        Registry<Spell> registry = SpellRegistry.from(player.level());
        List<Holder<Spell>> merged = actives == null ? new ArrayList<>() : new ArrayList<>(actives);
        for (String raw : granted) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            Holder<Spell> spell = id == null || registry == null ? null
                : registry.getHolder(ResourceKey.create(SpellRegistry.KEY, id)).orElse(null);
            if (spell == null || spell.value().type != Spell.Type.ACTIVE || merged.contains(spell)) continue;
            merged.add(spell);
        }
        boolean noActive = active == null || active == SpellContainer.EMPTY || !active.isResolver();
        SpellContainer container = noActive
            ? new SpellContainer(SpellContainer.ContentType.ANY, "", "", granted.size(), List.copyOf(granted))
            : active;
        return original.call(container, merged, passives, modifiers, sources);
    }
}
