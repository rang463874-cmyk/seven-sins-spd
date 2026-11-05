package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.wrath;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** Summons three corrupted wraiths of wrath that fight beside the hero until they fade. */
public class WrathSpirits extends ArmorAbility {

	public static final int COUNT = 3;

	{
		baseChargeUse = 50f;
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + n;
			if (!Dungeon.level.solid[c] && Actor.findChar(c) == null) cells.add(c);
		}
		if (cells.isEmpty()) {
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int might = hero.pointsInTalent(Talent.SPIRIT_MIGHT);
		for (int i = 0; i < COUNT && !cells.isEmpty(); i++) {
			int cell = cells.remove(Random.Int(cells.size()));
			Spirit s = new Spirit();
			s.adjustStats(Dungeon.scalingDepth() + 2 * might);
			s.HT = s.HP = Math.round((8 + 3 * Dungeon.scalingDepth()) * (1f + 0.25f * might));
			s.pos = cell;
			s.state = s.HUNTING;
			GameScene.add(s, 1f);
			Dungeon.level.occupyCell(s);
			Buff.affect(s, Corruption.class);
			s.sprite.alpha(0);
			s.sprite.parent.add(new AlphaTweener(s.sprite, 1, 0.5f));
			s.sprite.emitter().burst(ShadowParticle.CURSE, 6);
		}

		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.WRATH_SPIRITS;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SPIRIT_MIGHT, Talent.SPIRIT_HUNGER, Talent.SPIRIT_SPITE, Talent.HEROIC_ENERGY};
	}

	/** A wraith of wrath: normal accuracy/evasion (unlike regular wraiths) but real HP. */
	public static class Spirit extends Wraith {

		@Override
		public void adjustStats(int level) {
			this.level = level;
			defenseSkill = 5 + level;
			enemySeen = true;
		}

		@Override
		public int attackProc(Char enemy, int damage) {
			damage = super.attackProc(enemy, damage);
			Hero hero = Dungeon.hero;
			int hunger = hero == null ? 0 : hero.pointsInTalent(Talent.SPIRIT_HUNGER);
			if (hunger > 0 && hero.isAlive() && hero.HP < hero.HT) {
				hero.HP = Math.min(hero.HT, hero.HP + hunger);
				hero.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(
						com.shatteredpixel.shatteredpixeldungeon.effects.Speck.HEALING), 1);
			}
			return damage;
		}

		@Override
		public void die(Object cause) {
			Hero hero = Dungeon.hero;
			int spite = hero == null ? 0 : hero.pointsInTalent(Talent.SPIRIT_SPITE);
			if (spite > 0) {
				for (int n : PathFinder.NEIGHBOURS8) {
					Char ch = Actor.findChar(pos + n);
					if (ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && !WrathGrudge.immune(ch)) {
						Buff.prolong(ch, Amok.class, 2f * spite);
					}
				}
			}
			super.die(cause);
		}

		@Override
		public void storeInBundle(com.watabou.utils.Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("spirit_ht", HT);
		}

		@Override
		public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
			super.restoreFromBundle(bundle);
			if (bundle.contains("spirit_ht")) HT = bundle.getInt("spirit_ht");
		}
	}
}
