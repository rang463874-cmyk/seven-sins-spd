package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * The Sloth class's repose.
 *
 * The Sloth would rather not fight: dream puppets (summoned with the Cozy Pillow) do it instead.
 * Enemies defeated by puppets, nightmares or automatic attacks grant sloth points, which raise
 * the stats of the Sloth and its puppets without limit. The Sloth's own kills count for a third.
 * Also holds the pillow's recharging charges and the Sleepwalker's automatic attacks.
 */
public class SlothRepose extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	public float sloth = 0;

	public int lastPos = -1;
	/** consecutive turns the hero has stayed on the same tile */
	public int stillTurns = 0;

	public int charges = 2;
	public float partialCharge = 0;

	/** true while an automatic attack is being made, so its kills count in full */
	public static boolean autoAttacking = false;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private Hero hero() {
		return target instanceof Hero ? (Hero) target : null;
	}

	// ------------------------------------------------------------------ permanent stats

	public float pts() {
		Hero h = hero();
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.SLOTH_APATHY);
		return sloth * mult;
	}

	public int meleeBonus() {
		return (int) (0.15f * pts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.004f * pts());
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.003f * pts());
	}

	public int hpBonus() {
		return (int) (0.8f * pts());
	}

	public int drMax() {
		return (int) (0.10f * pts());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	public void addSloth(float amount) {
		Hero h = hero();
		if (h != null) amount *= 1f + 0.15f * h.pointsInTalent(Talent.SLOTH_IDLE);
		if (amount <= 0) return;
		int oldHP = hpBonus();
		sloth += amount;
		if (h != null && hpBonus() != oldHP) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ puppets

	public int maxPuppets() {
		Hero h = hero();
		int max = 1 + (mirrored ? 1 : 0);
		if (h != null && h.subClass == HeroSubClass.DREAM_LORD) max++;
		return max;
	}

	/** how many nightmares the Dream Lord's kills can keep around at once */
	public int maxLordNightmares() {
		Hero h = hero();
		return 2 + (h == null ? 0 : h.pointsInTalent(Talent.LORD_NIGHTMARE));
	}

	public static float lordNightmareLife(Hero h) {
		return 10f + 5f * h.pointsInTalent(Talent.LORD_NIGHTMARE);
	}

	// ------------------------------------------------------------------ charges

	public int maxCharges() {
		Hero h = hero();
		return 3 + (mirrored ? 1 : 0) + (h == null ? 0 : h.pointsInTalent(Talent.SLOTH_PILLOW));
	}

	public float turnsPerCharge() {
		Hero h = hero();
		return 30f / (1f + 0.2f * (h == null ? 0 : h.pointsInTalent(Talent.SLOTH_RESTFUL)));
	}

	public void gainCharge(float amount) {
		if (charges >= maxCharges()) {
			partialCharge = 0;
			return;
		}
		partialCharge += amount;
		while (partialCharge >= 1f && charges < maxCharges()) {
			partialCharge -= 1f;
			charges++;
		}
		if (charges >= maxCharges()) partialCharge = 0;
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
	}

	public boolean useCharges(int n) {
		if (charges < n) return false;
		charges -= n;
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
		return true;
	}

	// ------------------------------------------------------------------ sleepwalking

	/** the Sleepwalker dozes off after standing still for 2 turns */
	public boolean asleep() {
		Hero h = hero();
		return h != null && h.subClass == HeroSubClass.SLEEPWALKER && stillTurns >= 2;
	}

	/** makes free automatic attacks on nearby enemies while asleep or auto-hunting */
	private void autoAttack(Hero h) {
		AutoHunt hunt = h.buff(AutoHunt.class);
		boolean sleeping = asleep();
		if (!sleeping && hunt == null) return;

		int reach = 1;
		float mult = 0f;
		int targets = 1;
		if (sleeping) {
			mult = 0.6f + 0.2f * h.pointsInTalent(Talent.WALK_REFLEX);
			targets += h.pointsInTalent(Talent.WALK_BRAWL);
		}
		if (hunt != null) {
			mult = Math.max(mult, 0.7f + 0.15f * h.pointsInTalent(Talent.AUTO_POWER));
			reach += h.pointsInTalent(Talent.AUTO_REACH) / 2;
			targets = Math.max(targets, 1 + (h.pointsInTalent(Talent.AUTO_REACH) + 1) / 2);
		}

		final int hpos = h.pos;
		ArrayList<Char> foes = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch != h && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hpos, ch.pos) <= reach
					&& ch.invisible <= 0) {
				foes.add(ch);
			}
		}
		if (foes.isEmpty()) return;
		Collections.sort(foes, new Comparator<Char>() {
			@Override
			public int compare(Char a, Char b) {
				return Dungeon.level.distance(hpos, a.pos) - Dungeon.level.distance(hpos, b.pos);
			}
		});

		autoAttacking = true;
		try {
			for (int i = 0; i < Math.min(targets, foes.size()); i++) {
				Char foe = foes.get(i);
				if (!foe.isAlive()) continue;
				h.sprite.turnTo(hpos, foe.pos);
				h.attack(foe, mult, 0f, 1f);
				if (foe.sprite != null) foe.sprite.emitter().burst(Speck.factory(Speck.NOTE), 1);
			}
		} finally {
			autoAttacking = false;
		}
	}

	@Override
	public boolean act() {
		Hero h = hero();
		if (h != null && h.isAlive()) {
			if (charges < maxCharges()) gainCharge(TICK / turnsPerCharge());

			boolean wasAsleep = asleep();
			if (lastPos == h.pos) {
				stillTurns++;
			} else {
				stillTurns = 0;
			}
			lastPos = h.pos;
			if (!wasAsleep && asleep()) {
				h.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(SlothRepose.class, "doze"));
			}

			//lingering: puppets mend while the sloth stays put
			int linger = h.pointsInTalent(Talent.SLOTH_LINGER);
			if (linger > 0 && stillTurns > 0) {
				for (DreamPuppet p : DreamPuppet.active()) {
					if (p.HP < p.HT) p.HP = Math.min(p.HT, p.HP + linger);
				}
			}

			autoAttack(h);
		}
		spend(TICK);
		return true;
	}

	// ------------------------------------------------------------------ ui

	@Override
	public int icon() {
		return BuffIndicator.FURY;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.45f, 0.6f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - charges / (float) maxCharges();
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(DreamPuppet.puppets(false).size());
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return statsText();
	}

	public String statsText() {
		String s = Messages.get(SlothRepose.class, "desc_head", Math.round(sloth),
				DreamPuppet.puppets(false).size(), maxPuppets(), charges, maxCharges())
				+ "\n\n" + Messages.get(SlothRepose.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100));
		if (asleep()) s += "\n\n" + Messages.get(SlothRepose.class, "desc_asleep");
		return s;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("sloth", sloth);
		bundle.put("last_pos", lastPos);
		bundle.put("still", stillTurns);
		bundle.put("charges", charges);
		bundle.put("partial", partialCharge);
		bundle.put("mirrored", mirrored);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		sloth = bundle.getFloat("sloth");
		lastPos = bundle.getInt("last_pos");
		stillTurns = bundle.getInt("still");
		charges = bundle.getInt("charges");
		partialCharge = bundle.getFloat("partial");
		mirrored = bundle.getBoolean("mirrored");
	}

	public static SlothRepose of() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(SlothRepose.class);
	}

	// ------------------------------------------------------------------ taunt

	/** An enemy taunted by a dream puppet: it can only think about hitting that puppet. */
	public static class Taunted extends FlavourBuff {

		public int object = 0;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		/** taunts an enemy toward a puppet, refreshing to the longer duration */
		public static void apply(Char enemy, Char puppet, float duration) {
			if (enemy == null || puppet == null || !enemy.isAlive() || !puppet.isAlive()) return;
			if (enemy.alignment != Char.Alignment.ENEMY) return;
			Taunted t = Buff.prolong(enemy, Taunted.class, duration);
			if (t != null) t.object = puppet.id();
		}

		/** taunts every visible enemy within a radius of the puppet */
		public static int applyAround(Char puppet, int radius, float duration) {
			int n = 0;
			for (Char ch : Actor.chars()) {
				if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
						&& Dungeon.level.distance(puppet.pos, ch.pos) <= radius) {
					apply(ch, puppet, duration);
					n++;
				}
			}
			if (n > 0 && puppet.sprite != null) {
				puppet.sprite.showStatus(CharSprite.WARNING, Messages.get(Taunted.class, "shout"));
			}
			return n;
		}

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.5f, 0.7f, 1f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("object", object);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt("object");
		}
	}

	// ------------------------------------------------------------------ ability buffs

	/** Proxy armor ability: damage the hero would take goes to the puppets instead. */
	public static class ProxyGuard extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.5f, 0.7f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 25f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	/** Auto Hunt armor ability: free automatic attacks every turn. */
	public static class AutoHunt extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.FURY;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.7f, 0.4f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 20f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
