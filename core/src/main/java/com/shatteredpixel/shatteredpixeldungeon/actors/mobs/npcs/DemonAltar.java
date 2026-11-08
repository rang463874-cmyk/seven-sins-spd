package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDemonAltar;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

/**
 * The altar of the demon lords, raised where Goo fell. Choose one lord's trial: endure its penalty until
 * Tengu is beaten, and the lord grants a seal skill and a passive. Crumbles after a choice.
 */
public class DemonAltar extends NPC {

	{
		spriteClass = DemonSprites.Altar.class;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	@Override
	protected boolean act() {
		if (DemonHooks.stage != DemonHooks.NONE) {
			crumble();
			return true;
		}
		return super.act();
	}

	@Override
	public int defenseSkill(Char enemy) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage(int dmg, Object src) {
		//do nothing
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	public void crumble() {
		if (sprite != null) CellEmitter.get(pos).burst(ShadowParticle.UP, 16);
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero) return true;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new WndDemonAltar(DemonAltar.this));
			}
		});
		return true;
	}
}
