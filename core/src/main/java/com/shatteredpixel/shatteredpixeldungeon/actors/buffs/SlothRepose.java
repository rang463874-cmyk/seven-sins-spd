package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
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
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
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
 * The pillow's skills cost time: the sloth dozes off for a few turns after each one (Doze).
 * Also runs the Sleepwalker's automatic attacks.
 */
public class SlothRepose extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	public float sloth = 0;

	public int lastPos = -1;
	/** consecutive turns the hero has stayed on the same tile */
	public int stillTurns = 0;


	/** floors where the Dream Forge has already been used (once per floor) */
	public java.util.HashSet<Integer> forgedDepths = new java.util.HashSet<>();

	public boolean forgedHere() {
		return forgedDepths.contains(Dungeon.depth * 10 + Dungeon.branch);
	}

	public void markForged() {
		forgedDepths.add(Dungeon.depth * 10 + Dungeon.branch);
	}

	/** true while an automatic attack is being made, so its kills count in full */
	public static boolean autoAttacking = false;

	/** share of max HP each dream puppet borrows while it is out (nightmares are free) */
	public static final float DRAIN_PER_PUPPET = 0.15f;
	public static final float MAX_DRAIN = 0.75f;

	/** how much of the sloth's max HP its puppets are holding right now (0..MAX_DRAIN) */
	public static float puppetDrain(Hero h) {
		int n = DreamPuppet.puppets(false).size();
		if (n <= 0) return 0;
		float each = DRAIN_PER_PUPPET * (1f - 0.25f * h.pointsInTalent(Talent.SLOTH_SHOVE));
		return Math.min(MAX_DRAIN, each * n);
	}

	/** puppet count the hero's max HP was last computed with */
	private int drainedFor = 0;

	/** recomputes the hero's max HP when the number of puppets changes */
	public static void refreshDrain(Hero h) {
		SlothRepose r = h == null ? null : h.buff(SlothRepose.class);
		if (r == null) return;
		int n = DreamPuppet.puppets(false).size();
		if (n != r.drainedFor) {
			r.drainedFor = n;
			h.updateHT(false);
		}
	}

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
		return (int) (0.075f * pts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.004f * pts());
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.003f * pts());
	}

	public int hpBonus() {
		return (int) (0.2f * pts());
	}

	public int drMax() {
		return (int) (0.05f * pts());
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
		int max = 1 + (mirrored ? 1 : 0) + pillowPuppets(h);
		if (h != null && h.subClass == HeroSubClass.DREAM_LORD) max++;
		return max;
	}

	/** extra puppets from the pillow's level: +1 at +3, +5 and +10 */
	public static int pillowPuppets(Hero h) {
		int lvl = SinArtifact.level(h);
		return (lvl >= 3 ? 1 : 0) + (lvl >= 5 ? 1 : 0) + (lvl >= 10 ? 1 : 0);
	}

	/** how many nightmares the Dream Lord's kills can keep around at once */
	public int maxLordNightmares() {
		Hero h = hero();
		return 2 + (h == null ? 0 : h.pointsInTalent(Talent.LORD_NIGHTMARE));
	}

	public static float lordNightmareLife(Hero h) {
		return 10f + 5f * h.pointsInTalent(Talent.LORD_NIGHTMARE);
	}

	// ------------------------------------------------------------------ dozing (skill cost)

	/** turns a pillow skill with this base cost puts the sloth to sleep for */
	public static int turnCost(Hero h, int base) {
		float t = base * SinArtifact.rechargeScale(h);
		t *= 1f - 0.2f * (h == null ? 0 : h.pointsInTalent(Talent.SLOTH_RESTFUL));
		return Math.max(1, Math.round(t));
	}

	/** pays a skill's cost: the hero dozes off and loses that many turns */
	public static void doze(Hero h, int turns) {
		Refreshed fresh = h.buff(Refreshed.class);
		if (fresh != null && fresh.skills > 0) {
			//a good meal keeps the sloth awake for a skill or two
			if (--fresh.skills <= 0) fresh.detach();
			SinArtifact.onSpent(h, turns);
			BuffIndicator.refreshHero();
			h.spendAndNext(1f);
			return;
		}
		SlothRepose r = h.buff(SlothRepose.class);
		if (r != null) {
			//dozing doesn't count as moving
			r.lastPos = h.pos;
		}
		SinArtifact.onSpent(h, turns);
		Buff.prolong(h, Doze.class, turns);
		h.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(SlothRepose.class, "doze"));
		h.spendAndNext(turns);
	}

	/** Well Fed (sloth meal talent): the next few pillow skills don't make the sloth doze off */
	public static class Refreshed extends Buff {

		public int skills = 0;

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.WELL_FED;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(skills);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", skills);
		}

		@Override
		public void storeInBundle(com.watabou.utils.Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("skills", skills);
		}

		@Override
		public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
			super.restoreFromBundle(bundle);
			skills = bundle.getInt("skills");
		}
	}

	/** dozing after a pillow skill */
	public static class Doze extends FlavourBuff {

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public int icon() {
			return BuffIndicator.DROWSY;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 6f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
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

			refreshDrain(h);

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
		return statsTextBase() + com.shatteredpixel.shatteredpixeldungeon.sins.UniqueStats.line(target);
	}

	private String statsTextBase() {
		String s = Messages.get(SlothRepose.class, "desc_head", Math.round(sloth),
				DreamPuppet.puppets(false).size(), maxPuppets())
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
		bundle.put("mirrored", mirrored);
		int[] forged = new int[forgedDepths.size()];
		int i = 0;
		for (int d : forgedDepths) forged[i++] = d;
		bundle.put("forged", forged);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		sloth = bundle.getFloat("sloth");
		lastPos = bundle.getInt("last_pos");
		stillTurns = bundle.getInt("still");
		mirrored = bundle.getBoolean("mirrored");
		forgedDepths.clear();
		if (bundle.contains("forged")) for (int d : bundle.getIntArray("forged")) forgedDepths.add(d);
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

	// ------------------------------------------------------------------ dream order

	/** Order (pillow skill): every dream puppet drops what it's doing to hunt this enemy, and hits it harder. */
	public static class DreamMark extends FlavourBuff {

		public static final float DURATION = 30f;
		public static final float DAMAGE_BONUS = 0.25f;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		/** the enemy currently marked on this level, if any */
		public static Char marked() {
			for (Char ch : Actor.chars()) {
				if (ch.isAlive() && ch.buff(DreamMark.class) != null) return ch;
			}
			return null;
		}

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.3f, 0.5f, 1f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round(DAMAGE_BONUS * 100), dispTurns());
		}
	}

	// ------------------------------------------------------------------ dream forge

	/**
	 * Dream Forge (pillow skill): a long, deep sleep (200-300 turns) that ends with +1 on the chosen
	 * weapon or armor. Any damage wakes the sloth and the dream is lost. Once per floor.
	 */
	public static class DreamForge extends Buff {

		public static final String WEAPON = "weapon", ARMOR = "armor";

		public int left = 0;
		public int total = 0;
		public String slot = WEAPON;

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		/** sleep length for an item: 200 turns, +25 per upgrade level, at most 300 */
		public static int turnsFor(com.shatteredpixel.shatteredpixeldungeon.items.Item item) {
			int lvl = item == null ? 0 : Math.max(0, item.level());
			return Math.min(300, 200 + 25 * lvl);
		}

		public static com.shatteredpixel.shatteredpixeldungeon.items.Item item(Hero h, String slot) {
			if (h == null) return null;
			com.shatteredpixel.shatteredpixeldungeon.items.Item it =
					ARMOR.equals(slot) ? h.belongings.armor : h.belongings.weapon;
			return it != null && it.isUpgradable() ? it : null;
		}

		public void set(String slot, int turns) {
			this.slot = slot;
			this.left = this.total = turns;
		}

		@Override
		public boolean attachTo(Char target) {
			if (super.attachTo(target)) {
				target.paralysed++;
				return true;
			}
			return false;
		}

		@Override
		public boolean act() {
			Hero h = target instanceof Hero ? (Hero) target : null;
			if (h != null) h.resting = true;
			left--;
			if (left <= 0) {
				finish(h);
				return true;
			}
			spend(TICK);
			return true;
		}

		private void finish(Hero h) {
			com.shatteredpixel.shatteredpixeldungeon.items.Item it = item(h, slot);
			if (it != null) {
				it.upgrade();
				com.shatteredpixel.shatteredpixeldungeon.items.Item.updateQuickslot();
				if (h.sprite != null) {
					h.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
					h.sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "forged_status"));
				}
				com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.EVOKE);
				GLog.p(Messages.get(this, "forged", it.name()));
			} else {
				GLog.w(Messages.get(this, "nothing"));
			}
			detach();
		}

		/** damage wakes the sloth up and the dream is lost */
		public void interrupt() {
			GLog.w(Messages.get(this, "broken"));
			if (target.sprite != null) target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "broken_status"));
			detach();
		}

		@Override
		public void detach() {
			if (target.paralysed > 0) target.paralysed--;
			if (target instanceof Hero) ((Hero) target).resting = false;
			super.detach();
		}

		@Override
		public void fx(boolean on) {
			if (target.sprite == null) return;
			if (on) target.sprite.showSleep();
			else target.sprite.hideSleep();
		}

		@Override
		public int icon() {
			return BuffIndicator.MAGIC_SLEEP;
		}

		@Override
		public float iconFadePercent() {
			return total <= 0 ? 0 : Math.max(0, 1f - left / (float) total);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			com.shatteredpixel.shatteredpixeldungeon.items.Item it = item(target instanceof Hero ? (Hero) target : null, slot);
			return Messages.get(this, "desc", it == null ? "-" : it.name(), left);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
			bundle.put("total", total);
			bundle.put("slot", slot);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt("left");
			total = bundle.getInt("total");
			slot = bundle.getString("slot");
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
