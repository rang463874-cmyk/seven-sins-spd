package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.solomon;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

/**
 * Seven Sins: the Judgment of Solomon (Nov 18 2026, owner: from his gift of telling truth from lies). Every enemy on the
 * floor is seen for a moment, hidden mimics and invisible foes in sight are unmasked, and the enemies in sight are Exposed:
 * they can't evade anything (Char.hit) and take more damage (SolomonHooks.modifyDamage). JUDGE_SECRETS also finds the
 * secret doors and traps nearby.
 */
public class SolomonJudgment extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static float exposedTurns(Hero hero) {
		return 10f + 3f * hero.pointsInTalent(Talent.JUDGE_TRUTH);
	}

	/** extra damage Exposed enemies take */
	public static float exposedBonus(Hero hero) {
		return 0.25f + 0.10f * (hero == null ? 0 : hero.pointsInTalent(Talent.JUDGE_SENTENCE));
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Buff.affect(hero, MindVision.class, 3f);
		Dungeon.observe();
		int judged = 0;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (!Dungeon.level.heroFOV[m.pos] || m.alignment == Char.Alignment.ALLY) continue;
			if (m instanceof Mimic && m.state == m.PASSIVE) {
				m.alignment = Char.Alignment.ENEMY;
				((Mimic) m).stopHiding();
			}
			if (m.alignment != Char.Alignment.ENEMY) continue;
			Buff.detach(m, Invisibility.class);
			Buff.affect(m, Exposed.class, exposedTurns(hero));
			CellEmitter.center(m.pos).burst(Speck.factory(Speck.LIGHT), 4);
			judged++;
		}

		int secrets = hero.pointsInTalent(Talent.JUDGE_SECRETS);
		if (secrets > 0) {
			int r = 2 + 2 * secrets;
			boolean found = false;
			for (int i = 0; i < Dungeon.level.length(); i++) {
				if (Dungeon.level.distance(hero.pos, i) > r) continue;
				int terr = Dungeon.level.map[i];
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {
					Dungeon.level.discover(i);
					if (Dungeon.level.heroFOV[i]) {
						GameScene.discoverTile(i, terr);
						ScrollOfMagicMapping.discover(i);
					}
					found = true;
				}
			}
			if (found) {
				GameScene.updateFog();
				Sample.INSTANCE.play(Assets.Sounds.SECRET);
			}
		}

		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 12);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		hero.sprite.showStatus(0xFFDD77, Messages.get(this, "verdict", judged));
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.SOLOMON_JUDGMENT;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.JUDGE_TRUTH, Talent.JUDGE_SECRETS, Talent.JUDGE_SENTENCE, Talent.HEROIC_ENERGY};
	}

	/** an enemy whose lies are laid bare: can't evade, takes more damage */
	public static class Exposed extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.85f, 0.4f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round(exposedBonus(Dungeon.hero) * 100), dispTurns());
		}
	}
}
