package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.VirtueChampion;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.elites.EliteHooks;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sins.TrialHooks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ShadowSprite;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * Seven Sins: the trial of sin (Nov 18 2026, owner). The hero's own sin given shape: it waits in a dark sanctum on
 * one city floor (sins/TrialHooks) with the virtue champions' arena rules (it seals the door, never leaves) and
 * fights with two telegraphed skills taken from the hero's class. Beating it awakens the signature item a second
 * time (level cap 20 and a new skill).
 */
public class SinShadow extends VirtueChampion {

	/** whose shadow it is */
	public HeroClass cls = Dungeon.hero != null ? Dungeon.hero.heroClass : HeroClass.WRATH;

	{
		properties.add(Property.MINIBOSS);
		properties.add(Property.DEMONIC);
	}

	@Override
	protected void setLevel(int d) {
		super.setLevel(d);
		HP = HT = 40 + 14 * d;
		EXP = 15 + d;
	}

	@Override public Virtue virtue() { return Virtue.HUMILITY; }
	@Override public boolean nemesis() { return false; }
	@Override protected int sMin() { return Math.round(super.sMin() * 1.2f); }
	@Override protected int sMax() { return Math.round(super.sMax() * 1.2f); }

	@Override
	public CharSprite sprite() {
		return new ShadowSprite(cls);
	}

	private String clsKey() {
		return cls.name().toLowerCase();
	}

	@Override
	public String name() {
		return Messages.get(SinShadow.class, "name", cls.title());
	}

	@Override
	public String description() {
		return Messages.get(SinShadow.class, "desc", cls.title()) + "\n\n" + Messages.get(SinShadow.class, "desc_skills_" + kind());
	}

	@Override
	protected String announceLine(boolean hunting) {
		return Messages.get(SinShadow.class, hunting ? "arrives" : "awaits", name());
	}

	@Override protected String sealLine() { return Messages.get(SinShadow.class, "sealed"); }
	@Override protected String unsealLine() { return Messages.get(SinShadow.class, "unsealed"); }
	@Override protected String noticeLine() { return Messages.get(SinShadow.class, "notice"); }

	@Override
	protected void onSlain() {
		yell(Messages.get(SinShadow.class, "death"));
		if (buff(com.shatteredpixel.shatteredpixeldungeon.ascent.AscentHooks.Shade.class) != null) return; //Seven Sins: the ascent's mirror maze, not the trial
		EliteHooks.regionReward(pos);
		if (Dungeon.hero != null && Dungeon.hero.isAlive()) TrialHooks.onShadowSlain(Dungeon.hero);
	}

	// ------------------------------------------------------------------ skills: two per class

	/** the class whose skills it uses (the original classes borrow wrath's) */
	private String kind() {
		switch (cls) {
			case GLUTTONY: case GREED: case WRATH: case PRIDE: case LUST: case ENVY: case SLOTH: case SOLOMON:
				return clsKey();
			default:
				return "wrath";
		}
	}

	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == 0 ? 4 : 7; }
	@Override protected String skillKey(int s) { return kind() + "_" + s; }

	@Override
	protected int[] prepare(int s, Char target) {
		if (dist(target) > 7) return null;
		int t = target.pos;
		switch (kind() + s) {
			case "gluttony0": return crossLines(t, 2);
			case "gluttony1": return scatter(t, 3, 5);
			case "greed0":    return scatter(t, 3, 6);
			case "greed1":    return xCross(t, 2);
			case "wrath0":    return dist(target) <= 2 ? donut(1, 2) : null;
			case "wrath1":    return crossLines(t, 3);
			case "pride0":    return crossLines(t, 4);
			case "pride1":    return checker(t, 2);
			case "lust0":     return xCross(t, 2);
			case "lust1":     return scatter(t, 2, 5);
			case "envy0":     return crossLines(t, 3);
			case "envy1":     return checker(t, 2);
			case "sloth0":    return dist(target) <= 4 ? donut(2, 4) : null;
			case "sloth1":    return scatter(t, 2, 5);
			case "solomon0":  return xCross(t, 2);
			case "solomon1":  return checker(t, 2);
			default:          return null;
		}
	}

	@Override
	protected void release(int s, int[] cells) {
		String k = kind() + s;
		int dealt = 0;
		for (int c : cells) {
			if (k.startsWith("solomon0")) CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
			else CellEmitter.get(c).burst(ShadowParticle.CURSE, 3);
			Char ch = strike(c, sMin(), sMax());
			if (ch == null || !ch.isAlive()) continue;
			dealt += 1;
			switch (k) {
				case "gluttony1": Buff.affect(ch, Ooze.class).set(Ooze.DURATION); break;
				case "greed1":
					if (ch == Dungeon.hero && Dungeon.gold > 0) {
						int stolen = Math.max(1, Dungeon.gold / 20);
						Dungeon.gold -= stolen;
						ch.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(SinShadow.class, "stolen", stolen));
					}
					break;
				case "wrath0": Buff.prolong(ch, Cripple.class, 3f); break;
				case "wrath1": Buff.prolong(ch, Vulnerable.class, 4f); break;
				case "pride1": Buff.prolong(ch, Vertigo.class, 3f); break;
				case "lust0": Buff.prolong(ch, Weakness.class, 4f); break;
				case "lust1": Buff.affect(ch, Charm.class, 2f).object = id(); break;
				case "envy0": Buff.prolong(ch, Hex.class, 5f); break;
				case "envy1": Buff.prolong(ch, Vulnerable.class, 5f); break;
				case "sloth0": if (ch.buff(Drowsy.class) == null) Buff.affect(ch, Drowsy.class); break;
				case "sloth1": Buff.prolong(ch, Slow.class, 3f); break;
				case "solomon0": Buff.affect(ch, Burning.class).reignite(ch); break;
				case "solomon1": Buff.prolong(ch, Roots.class, 2f); break;
				default: break;
			}
		}
		//gluttony's and lust's shadows feed on what they hit
		if (dealt > 0 && (k.equals("gluttony0") || k.equals("lust0"))) {
			int heal = Math.min(HT - HP, HT / 15);
			if (heal > 0) {
				HP += heal;
				sprite.showStatus(CharSprite.POSITIVE, Integer.toString(heal));
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("shadow_cls", cls.name());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			cls = HeroClass.valueOf(bundle.getString("shadow_cls"));
		} catch (Exception e) {
			cls = HeroClass.WRATH;
		}
	}
}
