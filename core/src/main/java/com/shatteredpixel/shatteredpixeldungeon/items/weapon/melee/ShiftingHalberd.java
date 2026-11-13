package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Seven Sins: a tier 4 shifting weapon. Spear form: reach 2, more accurate, lighter blows. Axe form: reach 1, heavier
 * blows, a little less accurate. Switching forms is free (no turn).
 */
public class ShiftingHalberd extends MeleeWeapon {

	public static final String AC_SHIFT = "SHIFT";

	public static final float SPEAR_DMG = 0.8f, AXE_DMG = 1.2f;
	public static final float SPEAR_ACC = 1.15f, AXE_ACC = 0.9f;

	public boolean axe = false;

	{
		tier = 4;
		applyForm();
	}

	private void applyForm() {
		image = axe ? ItemSpriteSheet.HALBERD_AXE : ItemSpriteSheet.HALBERD_SPEAR;
		RCH = axe ? 1 : 2;
		hitSound = axe ? Assets.Sounds.HIT_SLASH : Assets.Sounds.HIT_STAB;
		hitSoundPitch = axe ? 0.9f : 1f;
	}

	@Override
	public int min(int lvl) {
		return Math.round(super.min(lvl) * (axe ? AXE_DMG : SPEAR_DMG));
	}

	@Override
	public int max(int lvl) {
		return Math.round(super.max(lvl) * (axe ? AXE_DMG : SPEAR_DMG));
	}

	@Override
	public float accuracyFactor(Char owner, Char target) {
		return super.accuracyFactor(owner, target) * (axe ? AXE_ACC : SPEAR_ACC);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SHIFT);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_SHIFT)) return Messages.get(this, axe ? "ac_to_spear" : "ac_to_axe");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_SHIFT)) {
			axe = !axe;
			applyForm();
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 1f, axe ? 0.8f : 1.2f);
			if (hero.sprite != null) {
				hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, axe ? "form_axe" : "form_spear"));
				hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 3);
			}
			updateQuickslot(); //free: no turn spent
		}
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, axe ? "stats_axe" : "stats_spear");
	}

	private static final String AXE = "axe";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(AXE, axe);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		axe = bundle.getBoolean(AXE);
		applyForm();
	}
}
