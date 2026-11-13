package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** T4 8~21, reach 2, magic damage (ignores armor), the runic blade's scaling (+tier+2 max per level). */
public class RuneSpear extends ModMeleeWeapon {
	{
		image = ItemSpriteSheet.RUNE_SPEAR; hitSound = Assets.Sounds.HIT_STAB; hitSoundPitch = 1.1f;
		tier = 4; baseMin = 8; baseMax = 21; maxScale = 6; RCH = 2;
	}
	@Override public float armorFactor(Char attacker, Char defender) { return 0f; }
}
