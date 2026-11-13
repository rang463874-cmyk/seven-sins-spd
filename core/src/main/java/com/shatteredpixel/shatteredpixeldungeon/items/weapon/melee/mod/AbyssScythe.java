package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T5 11~28: each kill heals HEAL_SHARE of max HP + the weapon's level. */
public class AbyssScythe extends ModMeleeWeapon {
	public static final float HEAL_SHARE = 0.08f;
	{
		image = ItemSpriteSheet.ABYSS_SCYTHE; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.7f;
		tier = 5; baseMin = 11; baseMax = 28;
	}
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (attacker instanceof Hero && lethal(defender, damage) && defender.alignment == Char.Alignment.ENEMY) {
			Hero h = (Hero) attacker;
			int heal = Math.min(h.HT - h.HP, Math.round(h.HT * HEAL_SHARE) + buffedLvl());
			if (heal > 0) {
				h.HP += heal;
				if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{Math.round(HEAL_SHARE * 100)}; }
}
