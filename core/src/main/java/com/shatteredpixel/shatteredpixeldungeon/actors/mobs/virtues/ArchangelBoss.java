package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinRewards;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;
import com.shatteredpixel.shatteredpixeldungeon.virtues.VirtueFoe;
import com.shatteredpixel.shatteredpixeldungeon.virtues.VirtueHooks;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The final boss after Yog: the archangel of the virtue opposing the hero's sin, standing guard before the
 * amulet on the last floor (the amulet can't be picked up while it lives). Telegraphed skills: Judgment
 * (3x3 light, blinds), Holy Lance (a long line) and its virtue's signature move. It ascends at 2/3 and 1/3
 * HP (a holy barrier each time; faster skills after the second). Nemesis rules apply (+25% both ways).
 */
public class ArchangelBoss extends RegionElite implements VirtueFoe {

	public Virtue virtue = Virtue.HUMILITY;

	private static final int JUDGMENT = 0, LANCE = 1, SIGNATURE = 2;

	private int ascensions = 0;

	{
		spriteClass = VirtueSprites.Michael.class;
		HP = HT = 650;
		defenseSkill = 30;
		EXP = 100;
		maxLvl = -2;
		properties.remove(Property.MINIBOSS);
		properties.add(Property.BOSS);
		immunities.add(Charm.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Terror.class);
	}

	@Override public Virtue virtue() { return virtue; }
	@Override public boolean nemesis() { return VirtueHooks.isNemesis(virtue); }

	@Override
	public CharSprite sprite() {
		return VirtueSprites.forAngel(virtue);
	}

	@Override
	public String name() {
		return Messages.get(this, "name", virtue.angel());
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", virtue.angel(), virtue.title())
				+ "\n\n" + Messages.get(this, "sig_" + virtue.name().toLowerCase());
		if (nemesis()) d += "\n\n" + Messages.get(VirtueChampion.class, "desc_nemesis");
		return d;
	}

	private String line(String part) {
		return Messages.get(this, virtue.name().toLowerCase() + "_" + part + (nemesis() ? "_own" : ""));
	}

	@Override public int damageRoll() { return Random.NormalIntRange(25, 40); }
	@Override public int attackSkill(Char target) { return 55; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 12); }

	@Override
	protected boolean act() {
		//it waits by the amulet and rises as soon as the hero draws near (no sneak attacks on an archangel)
		if (state == SLEEPING && Dungeon.hero != null && Dungeon.level.distance(pos, Dungeon.hero.pos) <= 6) {
			state = HUNTING;
			target = Dungeon.hero.pos;
			notice();
		}
		return super.act();
	}

	@Override
	protected String noticeLine() {
		return line("notice");
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss(this);
	}

	// ------------------------------------------------------------------ skills

	@Override protected int skillCount() { return 3; }

	@Override
	protected int cooldown(int s) {
		int cd = s == JUDGMENT ? 4 : s == LANCE ? 5 : 6;
		return ascensions >= 2 ? Math.max(2, cd - 2) : cd;
	}

	@Override
	protected String skillKey(int s) {
		if (s == JUDGMENT) return "judgment";
		if (s == LANCE) return "lance";
		return virtue.name().toLowerCase();
	}

	private int dist(Char t) {
		return Dungeon.level.distance(pos, t.pos);
	}

	private int[] square(int center) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS9) addIfOpen(cells, center + n);
		return toArray(cells);
	}

	private int[] plus(int center) {
		ArrayList<Integer> cells = new ArrayList<>();
		addIfOpen(cells, center);
		for (int n : PathFinder.NEIGHBOURS4) {
			addIfOpen(cells, center + n);
			addIfOpen(cells, center + 2 * n);
		}
		return toArray(cells);
	}

	private int[] around(int r) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++) {
			if (c != pos && Dungeon.level.distance(pos, c) <= r) addIfOpen(cells, c);
		}
		return toArray(cells);
	}

	private int[] line(Char t, int len) {
		Ballistica b = new Ballistica(pos, t.pos, Ballistica.STOP_SOLID);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 1; i <= Math.min(len, b.dist); i++) addIfOpen(cells, b.path.get(i));
		return toArray(cells);
	}

	@Override
	protected int[] prepare(int s, Char t) {
		int d = dist(t);
		if (s == JUDGMENT) return d <= 7 ? square(t.pos) : null;
		if (s == LANCE) return d >= 2 && d <= 9 ? line(t, 9) : null;
		switch (virtue) {
			case HUMILITY: case TEMPERANCE: return d <= 7 ? plus(t.pos) : null;
			case CHARITY: case KINDNESS: return d <= 7 ? square(t.pos) : null;
			case CHASTITY: case PATIENCE: return d <= 2 ? around(2) : null;
			case DILIGENCE: default: return d >= 2 && d <= 7 ? line(t, 8) : null;
		}
	}

	private void heal(int amount) {
		amount = Math.min(amount, HT - HP);
		if (amount <= 0) return;
		HP += amount;
		if (sprite != null) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == JUDGMENT) {
			GameScene.flash(0x40FFFFE0);
			for (int c : cells) {
				CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 3);
				Char ch = strike(c, 28, 46);
				if (ch != null && ch.isAlive()) Buff.prolong(ch, Blindness.class, 2f);
			}
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
			return;
		}
		if (s == LANCE) {
			for (int c : cells) {
				CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 2);
				strike(c, 30, 48);
			}
			Sample.INSTANCE.play(Assets.Sounds.RAY);
			return;
		}
		switch (virtue) {
			case HUMILITY:
				for (int c : cells) {
					Char ch = strike(c, 24, 40);
					if (ch == null || !ch.isAlive()) continue;
					Buff.prolong(ch, Weakness.class, 8f);
					for (Buff b : ch.buffs().toArray(new Buff[0])) {
						if (b.type == Buff.buffType.POSITIVE && b instanceof FlavourBuff) b.detach();
					}
				}
				break;
			case CHARITY:
				heal(Math.round(HT * 0.08f));
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.COIN), 2);
					Char ch = strike(c, 24, 40);
					if (ch == Dungeon.hero && Dungeon.gold > 0) {
						int take = Math.min(Dungeon.gold, Math.max(30, Dungeon.gold / 5));
						Dungeon.gold -= take;
						GLog.w(Messages.get(VirtueChampion.class, "tithe_taken", take));
					}
				}
				break;
			case CHASTITY:
				for (int c : cells) {
					Char ch = strike(c, 26, 42);
					if (ch == null || !ch.isAlive()) continue;
					Buff.prolong(ch, Blindness.class, 3f);
					for (Buff b : ch.buffs().toArray(new Buff[0])) {
						if (b.type == Buff.buffType.POSITIVE && b instanceof FlavourBuff) b.detach();
					}
				}
				break;
			case PATIENCE:
				Buff.affect(this, Barrier.class).setShield(Math.round(HT * 0.1f));
				for (int c : cells) {
					Char ch = strike(c, 26, 42);
					if (ch != null && ch.isAlive() && !Char.hasProp(ch, Property.IMMOVABLE)) {
						int dx = Integer.signum(Dungeon.level.cellToPoint(ch.pos).x - Dungeon.level.cellToPoint(pos).x);
						int dy = Integer.signum(Dungeon.level.cellToPoint(ch.pos).y - Dungeon.level.cellToPoint(pos).y);
						int far = ch.pos + (dx + dy * Dungeon.level.width()) * 3;
						if (far >= 0 && far < Dungeon.level.length() && far != ch.pos) {
							WandOfBlastWave.throwChar(ch, new Ballistica(ch.pos, far, Ballistica.MAGIC_BOLT), 2, true, true, this);
						}
					}
				}
				break;
			case TEMPERANCE:
				for (int c : cells) {
					Char ch = strike(c, 24, 40);
					if (ch == null || !ch.isAlive()) continue;
					Hunger h = ch.buff(Hunger.class);
					if (h != null) h.affectHunger(-150);
					Buff.prolong(ch, Cripple.class, 4f);
				}
				break;
			case KINDNESS:
				heal(Math.round(HT * 0.12f));
				for (int c : cells) {
					Char ch = strike(c, 18, 30);
					if (ch != null && ch.isAlive()) Buff.prolong(ch, Roots.class, 3f);
				}
				break;
			case DILIGENCE:
			default:
				int landing = -1;
				for (int i = cells.length - 1; i >= 0; i--) {
					if (Dungeon.level.passable[cells[i]] && Actor.findChar(cells[i]) == null) { landing = cells[i]; break; }
				}
				for (int c : cells) {
					Char ch = strike(c, 26, 42);
					if (ch != null && ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
					if (c == landing) break;
				}
				if (landing != -1 && !rooted) {
					int from = pos;
					pos = landing;
					Actor.add(new Pushing(this, from, landing));
					Dungeon.level.occupyCell(this);
				}
				Buff.prolong(this, Haste.class, 3f);
				break;
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC);
	}

	// ------------------------------------------------------------------ ascension and death

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		if (!isAlive()) return;
		int want = HP < HT / 3 ? 2 : HP < HT * 2 / 3 ? 1 : 0;
		while (ascensions < want) {
			ascensions++;
			Buff.affect(this, Barrier.class).setShield(Math.round(HT * 0.15f));
			if (sprite != null) sprite.emitter().burst(Speck.factory(Speck.LIGHT), 20);
			GameScene.flash(0x60FFFFFF);
			yell(line("phase"));
			skillCooldown = 0;
		}
	}

	@Override
	protected boolean dropsReward() {
		return false;
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		yell(line("death"));
		GameScene.bossSlain();
		VirtueHooks.angelStage = 2;
		if (nemesis() && Dungeon.hero != null && Dungeon.hero.isAlive()) SinRewards.grant(Dungeon.hero, 100);
		GLog.p(Messages.get(this, "amulet_free"));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("virtue", virtue.name());
		bundle.put("ascensions", ascensions);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			virtue = Virtue.valueOf(bundle.getString("virtue"));
		} catch (Exception e) {
			virtue = Virtue.HUMILITY;
		}
		ascensions = bundle.getInt("ascensions");
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
	}
}
