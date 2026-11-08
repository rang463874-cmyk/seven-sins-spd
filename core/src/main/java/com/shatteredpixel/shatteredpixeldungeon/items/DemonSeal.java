package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonPerk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DemonFly;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The seal a demon lord grants after its trial: one skill in the lord's image, paid with the boon's
 * charge (100, +1 per turn; +2 once the lord's true form is defeated). Never lost, cannot be sold.
 */
public class DemonSeal extends Item {

	public static final String AC_INVOKE = "INVOKE";

	{
		image = ItemSpriteSheet.DEMON_SEAL;
		unique = true;
		bones = false;
		defaultAction = AC_INVOKE;
	}

	@Override
	public boolean keptThroughLostInventory() {
		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}

	private static DemonLord lord() {
		return DemonHooks.lord;
	}

	private static DemonPerk perk(Hero hero) {
		return hero.buff(DemonPerk.class);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_INVOKE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_INVOKE) && lord() != null) return lord().get("skill_name").toUpperCase();
		return super.actionName(action, hero);
	}

	@Override
	public String name() {
		return lord() == null ? super.name() : Messages.get(this, "name_lord", lord().title());
	}

	@Override
	public String desc() {
		DemonLord l = lord();
		if (l == null) return Messages.get(this, "desc_none");
		DemonPerk p = Dungeon.hero == null ? null : perk(Dungeon.hero);
		String d = Messages.get(this, "desc", l.title(), l.get("skill_name"), l.get("skill_desc"), l.get("perk"));
		if (p != null) d += "\n\n" + Messages.get(this, "charge", (int) p.charge);
		return d;
	}

	@Override
	public String status() {
		DemonPerk p = Dungeon.hero == null ? null : perk(Dungeon.hero);
		if (p == null) return null;
		return (int) p.charge + "%";
	}

	@Override
	public ItemSprite.Glowing glowing() {
		DemonLord l = lord();
		return l == null ? null : new ItemSprite.Glowing(l.color, 1.5f);
	}

	/** base damage of the seal skills, grows with the hero's level */
	public static int damage(Hero hero) {
		return Random.NormalIntRange(4 + hero.lvl, 10 + 2 * hero.lvl);
	}

	private static boolean targeted(DemonLord l) {
		return l == DemonLord.LUCIFER || l == DemonLord.MAMMON || l == DemonLord.LEVIATHAN;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_INVOKE)) return;

		DemonLord l = lord();
		DemonPerk p = perk(hero);
		if (l == null || p == null || DemonHooks.stage < DemonHooks.GRANTED) {
			GLog.w(Messages.get(this, "dormant"));
			return;
		}
		if (!p.ready()) {
			GLog.w(Messages.get(this, "not_ready", (int) p.charge));
			return;
		}
		if (targeted(l)) {
			usesTargeting = true;
			GameScene.selectCell(new CellSelector.Listener() {
				@Override
				public void onSelect(Integer cell) {
					if (cell == null) return;
					invoke(hero, l, cell);
				}

				@Override
				public String prompt() {
					return Messages.get(DemonSeal.class, "prompt");
				}
			});
		} else {
			invoke(hero, l, hero.pos);
		}
	}

	private void invoke(Hero hero, DemonLord l, int cell) {
		DemonPerk p = perk(hero);
		if (p == null || !p.ready()) return;

		switch (l) {
			case LUCIFER:
				if (!Dungeon.level.heroFOV[cell]) { GLog.w(Messages.get(this, "no_sight")); return; }
				morningStar(hero, cell);
				break;
			case MAMMON:
				Char t = Actor.findChar(cell);
				if (t == null || t == hero || t.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[cell]) {
					GLog.w(Messages.get(this, "no_target")); return;
				}
				midas(hero, t);
				break;
			case LEVIATHAN:
				if (cell == hero.pos) { GLog.w(Messages.get(this, "no_target")); return; }
				tidalWave(hero, cell);
				break;
			case ASMODEUS: allure(hero); break;
			case BEELZEBUB: swarm(hero); break;
			case SATAN: hellfire(hero); break;
			case BELPHEGOR: idleness(hero); break;
		}

		p.charge = 0;
		updateQuickslot();
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	private static void hit(Hero hero, Char ch, int dmg) {
		if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY || !ch.isAlive()) return;
		ch.damage(dmg, DemonSeal.class);
		if (ch.sprite != null) ch.sprite.flash();
	}

	/** Lucifer: a falling star bursts on a 3x3 area, blinding what survives */
	private void morningStar(Hero hero, int cell) {
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		for (int i : PathFinder.NEIGHBOURS9) {
			int c = cell + i;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 3);
			Char ch = Actor.findChar(c);
			if (ch != null && ch != hero && ch.alignment != Char.Alignment.ALLY) {
				hit(hero, ch, Math.round(damage(hero) * 1.3f));
				if (ch.isAlive()) Buff.prolong(ch, Blindness.class, 5f);
			}
		}
		GameScene.flash(0x60FFF0C0);
	}

	/** Mammon: an ordinary monster turns to gold; a boss loses 15% of its max HP */
	private void midas(Hero hero, Char t) {
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
		if (t.sprite != null) t.sprite.emitter().burst(Speck.factory(Speck.COIN), 12);
		boolean boss = t.properties().contains(Char.Property.BOSS) || t.properties().contains(Char.Property.MINIBOSS);
		if (boss) {
			hit(hero, t, Math.max(1, Math.round(t.HT * 0.15f)));
		} else {
			int pos = t.pos;
			int gold = 10 + 8 * Dungeon.depth + t.HT / 2;
			t.die(DemonSeal.class);
			Dungeon.level.drop(new Gold(gold), pos).sprite.drop();
		}
	}

	/** Leviathan: a wave rolls 6 tiles, hurting and pushing everything in it */
	private void tidalWave(Hero hero, int target) {
		Ballistica line = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
		int dist = Math.min(6, line.dist);
		ArrayList<Char> hitList = new ArrayList<>();
		for (int i = 1; i <= dist; i++) {
			int c = line.path.get(i);
			CellEmitter.center(c).burst(Speck.factory(Speck.STEAM), 4);
			Char ch = Actor.findChar(c);
			if (ch != null && ch != hero && ch.alignment != Char.Alignment.ALLY) hitList.add(ch);
		}
		Sample.INSTANCE.play(Assets.Sounds.WATER);
		for (Char ch : hitList) {
			hit(hero, ch, damage(hero));
			if (ch.isAlive()) {
				int dx = Dungeon.level.cellToPoint(ch.pos).x - Dungeon.level.cellToPoint(hero.pos).x;
				int dy = Dungeon.level.cellToPoint(ch.pos).y - Dungeon.level.cellToPoint(hero.pos).y;
				int ddx = Integer.signum(dx), ddy = Integer.signum(dy);
				int far = ch.pos + (ddx + ddy * Dungeon.level.width()) * 3;
				if (far >= 0 && far < Dungeon.level.length()) {
					Ballistica push = new Ballistica(ch.pos, far, Ballistica.MAGIC_BOLT);
					WandOfBlastWave.throwChar(ch, push, 3, true, true, DemonSeal.class);
				}
			}
		}
	}

	/** Asmodeus: every enemy in sight is charmed for 6 turns */
	private void allure(Hero hero) {
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[m.pos]) continue;
			Charm c = Buff.affect(m, Charm.class, 6f);
			if (c != null) c.object = hero.id();
			if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.HEART), 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
	}

	/** Beelzebub: three flies serve for 30 turns */
	private void swarm(Hero hero) {
		for (int i = 0; i < 3; i++) DemonFly.spawnNear(hero.pos, Char.Alignment.ALLY, 30);
		Sample.INSTANCE.play(Assets.Sounds.BEE);
	}

	/** Satan: hellfire bursts out to 2 tiles, setting enemies alight */
	private void hellfire(Hero hero) {
		PathFinder.buildDistanceMap(hero.pos, com.watabou.utils.BArray.not(Dungeon.level.solid, null), 2);
		for (int c = 0; c < PathFinder.distance.length; c++) {
			if (PathFinder.distance[c] == Integer.MAX_VALUE || c == hero.pos) continue;
			CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
			Char ch = Actor.findChar(c);
			if (ch != null && ch != hero && ch.alignment != Char.Alignment.ALLY) {
				hit(hero, ch, damage(hero));
				if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 6f);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
	}

	/** Belphegor: rest is power: wands and artifacts recharge and wounds close */
	private void idleness(Hero hero) {
		ScrollOfRecharging.charge(hero);
		Buff.affect(hero, Recharging.class, 15f);
		Buff.affect(hero, ArtifactRecharge.class).set(15f).ignoreHornOfPlenty = false;
		int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.20f));
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		CellEmitter.get(hero.pos).burst(ShadowParticle.UP, 8);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
	}
}
