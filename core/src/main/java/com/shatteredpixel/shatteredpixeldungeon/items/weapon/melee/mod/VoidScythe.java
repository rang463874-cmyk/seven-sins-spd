package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/** T6 16~40: each hit has VOID_BASE + VOID_LEVEL x level to seal a non-boss enemy in the void (instant death). */
public class VoidScythe extends ApocalypseWeapon {
	public static final float VOID_BASE = 0.04f, VOID_LEVEL = 0.01f;
	{
		image = ItemSpriteSheet.VOID_SCYTHE; hitSound = Assets.Sounds.HIT_SLASH; hitSoundPitch = 0.5f;
		baseMin = 16; baseMax = 40;
	}
	public float chance() { return VOID_BASE + VOID_LEVEL * buffedLvl(); }
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (defender.isAlive() && defender.alignment != attacker.alignment
				&& !defender.properties().contains(Char.Property.BOSS)
				&& !defender.properties().contains(Char.Property.MINIBOSS)
				&& roll(chance())) {
			if (defender.sprite != null) {
				defender.sprite.emitter().burst(ShadowParticle.UP, 12);
				defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(VoidScythe.class, "voided"));
			}
			Sample.INSTANCE.play(Assets.Sounds.CURSED);
			defender.die(this);
			return 0;
		}
		return super.proc(attacker, defender, damage);
	}
	@Override protected Object[] statsArgs() { return new Object[]{String.format(java.util.Locale.ENGLISH, "%.0f", chance() * 100)}; }
}
