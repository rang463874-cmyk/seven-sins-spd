package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.solomon;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.solomon.SolomonSpells;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Seven Sins: Elemental Fusion (Nov 18 2026, owner: from his way of attacking by combining elements). Fuses the symbols in
 * the circle into one blast at the target: +50% damage per drawn symbol, each adding its element's effect; an empty circle
 * fuses the four elements at base power. Fire burns, water chills, air blinds, earth roots and cripples, sulfur drains life,
 * mercury leaves foes vulnerable, salt doubles damage to demons and undead.
 */
public class ElementalFusion extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	public static int radius(Hero hero) {
		return 2 + hero.pointsInTalent(Talent.FUSION_RADIUS) / 2;
	}

	@Override
	public void activate(final ClassArmor armor, final Hero hero, Integer target) {
		if (target == null || target == hero.pos) return;
		final int end = new Ballistica(hero.pos, target, Ballistica.PROJECTILE).collisionPos;

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		SolomonArcana a = hero.buff(SolomonArcana.class);
		final EnumSet<SolomonSpells.Element> els = EnumSet.noneOf(SolomonSpells.Element.class);
		final int symbols = a == null ? 0 : a.circle.size();
		if (symbols == 0) {
			els.add(SolomonSpells.Element.FIRE);
			els.add(SolomonSpells.Element.WATER);
			els.add(SolomonSpells.Element.AIR);
			els.add(SolomonSpells.Element.EARTH);
		} else {
			els.addAll(a.circle);
			int res = hero.pointsInTalent(Talent.FUSION_RESONANCE);
			if (res > 0) a.addMana(2 * res * symbols);
			a.circle.clear();
		}
		final float mult = (1f + 0.5f * symbols) * (1f + 0.15f * hero.pointsInTalent(Talent.FUSION_POWER))
				* (a == null ? 1f : a.spellPower());

		hero.sprite.zap(end);
		hero.busy();
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.RAINBOW, hero.sprite, end, new Callback() {
			@Override
			public void call() {
				blast(hero, end, els, mult);
				hero.spendAndNext(1f);
			}
		});
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	private static void blast(Hero hero, int center, EnumSet<SolomonSpells.Element> els, float mult) {
		int r = radius(hero);
		int w = Dungeon.level.width();
		int cx = center % w, cy = center / w;
		int lvl = Math.max(1, hero.lvl);
		int stolen = 0;
		ArrayList<Char> hit = new ArrayList<>();
		for (int y = cy - r; y <= cy + r; y++) {
			for (int x = cx - r; x <= cx + r; x++) {
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int c = x + y * w;
				if (Dungeon.level.solid[c]) continue;
				if (els.contains(SolomonSpells.Element.FIRE)) CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
				else CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 2);
				Char ch = Actor.findChar(c);
				if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY || !ch.isAlive()) continue;
				hit.add(ch);
			}
		}
		if (els.contains(SolomonSpells.Element.FIRE)) SolomonSpells.igniteArea(center, r);
		for (Char ch : hit) {
			float m = mult;
			if (els.contains(SolomonSpells.Element.SALT)
					&& (Char.hasProp(ch, Char.Property.DEMONIC) || Char.hasProp(ch, Char.Property.UNDEAD))) m *= 2f;
			int before = ch.HP;
			ch.damage(Math.round(Random.NormalIntRange(lvl, 3 * lvl + 10) * m), ElementalFusion.class);
			stolen += Math.max(0, before - Math.max(0, ch.HP));
			if (!ch.isAlive()) continue;
			if (els.contains(SolomonSpells.Element.FIRE)) Buff.affect(ch, Burning.class).reignite(ch);
			if (els.contains(SolomonSpells.Element.WATER)) Buff.prolong(ch, Chill.class, 5f);
			if (els.contains(SolomonSpells.Element.AIR)) Buff.prolong(ch, Blindness.class, 3f);
			if (els.contains(SolomonSpells.Element.EARTH)) {
				Buff.prolong(ch, Roots.class, 2f);
				Buff.prolong(ch, Cripple.class, 5f);
			}
			if (els.contains(SolomonSpells.Element.MERCURY)) Buff.prolong(ch, Vulnerable.class, 8f);
		}
		if (els.contains(SolomonSpells.Element.SULFUR) && stolen > 0) {
			int heal = Math.min(hero.HT - hero.HP, Math.round(stolen * 0.2f));
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		PixelScene.shake(3, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
	}

	@Override
	public int icon() {
		return HeroIcon.ELEMENTAL_FUSION;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.FUSION_POWER, Talent.FUSION_RADIUS, Talent.FUSION_RESONANCE, Talent.HEROIC_ENERGY};
	}
}
