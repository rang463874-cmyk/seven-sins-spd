package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A demon lord's true form, fought after its boon was granted (Tengu's floor). Telegraphed skills like the
 * region elites: a ring of hellfire, summoned wraiths, and the lord's own signature move. Enraged below half
 * HP (shorter cooldowns). Defeating it empowers the boon and drops three sin essences.
 */
public class DemonLordBoss extends RegionElite {

	public DemonLord lord = DemonLord.LUCIFER;

	{
		spriteClass = DemonSprites.Lucifer.class;
		//harder since Nov 2026 (owner's call): was 300 HP, 10-20 damage, 30 accuracy, 0-8 armor, slower skills
		HP = HT = 450;
		defenseSkill = 24;
		EXP = 40;
		maxLvl = -2;
		properties.remove(Property.MINIBOSS);
		properties.add(Property.BOSS);
		properties.add(Property.DEMONIC);
	}

	private static final int HELLFIRE = 0, SUMMON = 1, SIGNATURE = 2;

	protected boolean enraged = false;
	private int stolenGold = 0;

	@Override
	public CharSprite sprite() {
		return DemonSprites.forLord(lord);
	}

	@Override
	public String name() {
		return lord.title();
	}

	@Override
	public String description() {
		return lord.get("boss_desc");
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(13, 26);
	}

	@Override
	public int attackSkill(Char target) {
		return 36;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(2, 10);
	}

	@Override
	protected int skillCount() {
		return 3;
	}

	@Override
	protected String skillKey(int skill) {
		switch (skill) {
			case HELLFIRE: return "hellfire";
			case SUMMON:   return "summon";
			default:       return lord.key();
		}
	}

	@Override
	protected int cooldown(int skill) {
		int cd;
		switch (skill) {
			case HELLFIRE: cd = 3; break;
			case SUMMON:   cd = 6; break;
			default:       cd = 4; break;
		}
		return enraged ? Math.max(2, cd / 2) : cd;
	}

	@Override
	protected String noticeLine() {
		return DemonHooks.line(lord, "boss_notice");
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss(this);
	}

	private int minions() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) {
			if ((m instanceof Wraith || m instanceof DemonFly) && m.alignment == Alignment.ENEMY) n++;
		}
		return n;
	}

	private int[] square(int center) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS9) addIfOpen(cells, center + i);
		return toArray(cells);
	}

	private int[] radius(int center, int r) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++) {
			if (c != center && Dungeon.level.distance(center, c) <= r) addIfOpen(cells, c);
		}
		return toArray(cells);
	}

	@Override
	protected int[] prepare(int skill, Char target) {
		int dist = Dungeon.level.distance(pos, target.pos);
		switch (skill) {
			case HELLFIRE: {
				if (dist > 1) return null;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int n : PathFinder.NEIGHBOURS8) addIfOpen(cells, pos + n);
				return toArray(cells);
			}
			case SUMMON:
				return minions() < (enraged ? 4 : 2) ? new int[0] : null;
			default:
				switch (lord) {
					case LUCIFER: case MAMMON: case ASMODEUS: case BELPHEGOR:
						return dist <= 6 ? square(target.pos) : null;
					case LEVIATHAN: {
						if (dist > 7) return null;
						Ballistica b = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
						ArrayList<Integer> cells = new ArrayList<>();
						for (int i = 1; i <= Math.min(7, b.dist); i++) addIfOpen(cells, b.path.get(i));
						return toArray(cells);
					}
					case SATAN:
						return dist <= 2 ? radius(pos, 2) : null;
					case BEELZEBUB:
					default:
						return minions() < 5 ? new int[0] : null;
				}
		}
	}

	@Override
	protected void release(int skill, int[] cells) {
		switch (skill) {
			case HELLFIRE:
				for (int c : cells) {
					CellEmitter.get(c).burst(FlameParticle.FACTORY, 6);
					Char ch = strike(c, 12, 22);
					if (ch != null && ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 4f);
				}
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				break;
			case SUMMON:
				for (int i = 0; i < 2; i++) {
					Wraith w = Wraith.spawnAt(pos);
					if (w != null) w.beckon(Dungeon.hero.pos);
				}
				CellEmitter.get(pos).burst(ShadowParticle.CURSE, 10);
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
				break;
			default:
				signature(cells);
				break;
		}
	}

	private void signature(int[] cells) {
		switch (lord) {
			case LUCIFER:
				GameScene.flash(0x50FFF0C0);
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 3);
					Char ch = strike(c, 14, 26);
					if (ch != null && ch.isAlive()) Buff.prolong(ch, Blindness.class, 3f);
				}
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				break;
			case MAMMON:
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.COIN), 3);
					Char ch = strike(c, 10, 20);
					if (ch == Dungeon.hero && Dungeon.gold > 0) {
						int take = Math.min(Dungeon.gold, Math.max(20, Dungeon.gold / 10));
						Dungeon.gold -= take;
						stolenGold += take;
						GLog.w(Messages.get(this, "steal", take));
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.GOLD);
				break;
			case ASMODEUS:
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.HEART), 3);
					Char ch = strike(c, 8, 16);
					if (ch != null && ch.isAlive()) {
						Charm ch2 = Buff.affect(ch, Charm.class, 3f);
						if (ch2 != null) ch2.object = id();
						heal(8);
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.CHARMS);
				break;
			case LEVIATHAN:
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.STEAM), 4);
					Char ch = strike(c, 12, 24);
					if (ch != null && ch.isAlive()) {
						int dx = Integer.signum(Dungeon.level.cellToPoint(ch.pos).x - Dungeon.level.cellToPoint(pos).x);
						int dy = Integer.signum(Dungeon.level.cellToPoint(ch.pos).y - Dungeon.level.cellToPoint(pos).y);
						int far = ch.pos + (dx + dy * Dungeon.level.width()) * 3;
						if (far >= 0 && far < Dungeon.level.length() && far != ch.pos) {
							WandOfBlastWave.throwChar(ch, new Ballistica(ch.pos, far, Ballistica.MAGIC_BOLT), 3, true, true, this);
						}
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.WATER);
				break;
			case SATAN:
				for (int c : cells) {
					CellEmitter.get(c).burst(FlameParticle.FACTORY, 6);
					Char ch = strike(c, 14, 24);
					if (ch != null && ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 6f);
				}
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				break;
			case BELPHEGOR:
				for (int c : cells) {
					CellEmitter.get(c).burst(ShadowParticle.UP, 4);
					Char ch = strike(c, 10, 18);
					if (ch != null && ch.isAlive()) Buff.prolong(ch, Slow.class, 4f);
				}
				Sample.INSTANCE.play(Assets.Sounds.DEGRADE);
				break;
			case BEELZEBUB:
			default:
				for (int i = 0; i < 3; i++) {
					DemonFly f = DemonFly.spawnNear(pos, Alignment.ENEMY, -1);
					if (f != null) f.beckon(Dungeon.hero.pos);
				}
				Sample.INSTANCE.play(Assets.Sounds.BEE);
				break;
		}
	}

	private void heal(int amount) {
		amount = Math.min(amount, HT - HP);
		if (amount <= 0) return;
		HP += amount;
		if (sprite != null) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(soften(dmg, softThreshold(HT)), src);
		if (isAlive() && !enraged && HP < HT / 2) {
			enraged = true;
			yell(DemonHooks.line(lord, "boss_enrage"));
			if (sprite != null) sprite.emitter().burst(ShadowParticle.UP, 20);
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
		yell(DemonHooks.line(lord, "boss_death"));
		GameScene.bossSlain();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if ((m instanceof Wraith || m instanceof DemonFly) && m.alignment == Alignment.ENEMY) m.die(null);
		}
		if (stolenGold > 0) Dungeon.level.drop(new Gold(stolenGold), pos).sprite.drop();
		DemonHooks.onLordDefeated(pos);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("lord", lord.name());
		bundle.put("enraged", enraged);
		bundle.put("stolen", stolenGold);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			lord = DemonLord.valueOf(bundle.getString("lord"));
		} catch (Exception e) {
			lord = DemonLord.LUCIFER;
		}
		enraged = bundle.getBoolean("enraged");
		stolenGold = bundle.getInt("stolen");
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
	}
}
