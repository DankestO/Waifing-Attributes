package com.waifing.waifingattributes.config;

import com.waifing.waifingattributes.upgrade.UpgradeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Un material catalizador de equipo y todos los efectos que aplica (uno o mas por categoria). */
public final class CatalystDef {
    public record Effect(UpgradeCategory category, boolean durability, @Nullable ResourceLocation attribute,
                         double amount, AttributeModifier.Operation operation) {}

    private final ItemMatcher matcher;
    private final List<Effect> effects = new ArrayList<>();

    public CatalystDef(ItemMatcher matcher) {
        this.matcher = matcher;
    }

    public ItemMatcher matcher() { return matcher; }
    public String key() { return matcher.key(); }
    public void add(Effect e) { effects.add(e); }
    public List<Effect> effects() { return effects; }

    public List<Effect> effectsFor(UpgradeCategory cat) {
        List<Effect> out = new ArrayList<>();
        for (Effect e : effects) {
            if (e.category() == UpgradeCategory.ALL || e.category() == cat) out.add(e);
        }
        return out;
    }

    public boolean appliesTo(UpgradeCategory cat) {
        return !effectsFor(cat).isEmpty();
    }
}
