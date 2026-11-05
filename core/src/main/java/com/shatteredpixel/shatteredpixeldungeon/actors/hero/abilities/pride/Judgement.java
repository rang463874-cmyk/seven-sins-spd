package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.pride;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** Passes judgement on one enemy in sight with a bolt from above. Scales with glory. */
public class Judgement extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || enemy.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		PrideGlory g = hero.buff(PrideGlory.class);
		float dmg = Random.NormalIntRange(hero.lvl, 3 * hero.lvl + 5) * (1f + 0.25f * hero.pointsInTalent(Talent.JUDGE_POWER));
		if (g != null) {
			dmg += g.pts() * 0.5f;
			int jd = hero.pointsInTalent(Talent.JUDGE_DIGNITY);
			if (jd > 0 && g.dignity > 0) {
				dmg += g.dignity * 2f * jd;
				g.dignity = 0;
			}
		}

		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		arcs.add(new Lightning.Arc(enemy.sprite.center().offset(0, -60), enemy.sprite.center()));
		hero.sprite.parent.addToFront(new Lightning(arcs, null));
		GameScene.flash(0x60CC88FF);
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);

		boolean flawless = enemy.buff(PrideGlory.Insolent.class) == null;
		enemy.damage(Math.round(dmg), this);

		//Divine Glory talent: a flawless judgement kill brings extra glory
		int jg = hero.pointsInTalent(Talent.JUDGE_GLORY);
		if (!enemy.isAlive() && flawless && jg > 0 && g != null) {
			g.addGlory(PrideGlory.gloryFor(enemy) * 0.5f * jg);
		}

		hero.sprite.zap(target);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.PRIDE_JUDGEMENT;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.JUDGE_POWER, Talent.JUDGE_DIGNITY, Talent.JUDGE_GLORY, Talent.HEROIC_ENERGY};
	}
}
