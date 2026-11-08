package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DemonLordBoss;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

/**
 * The chosen demon lord, come to Tengu's prison after the trial. The first talk grants the boon (seal +
 * passive); after that it offers its true form as a boss fight, now or later.
 */
public class DemonLordNPC extends NPC {

	public DemonLord lord = DemonLord.LUCIFER;

	{
		spriteClass = DemonSprites.Lucifer.class;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.DEMONIC);
	}

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
		return lord.get("desc");
	}

	@Override
	protected boolean act() {
		if (DemonHooks.stage == DemonHooks.DEFEATED || DemonHooks.lord != lord) {
			vanish();
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

	public void vanish() {
		if (sprite != null) CellEmitter.get(pos).burst(ShadowParticle.UP, 16);
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	@Override
	public boolean interact(Char c) {
		sprite.turnTo(pos, Dungeon.hero.pos);
		if (c != Dungeon.hero) return true;

		if (DemonHooks.stage == DemonHooks.TRIAL) {
			final String msg = DemonHooks.line(lord, "judge");
			DemonHooks.grantPerk(Dungeon.hero);
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndQuest(DemonLordNPC.this, msg) {
						@Override
						public void hide() {
							super.hide();
							offerFight();
						}
					});
				}
			});
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					offerFight();
				}
			});
		}
		return true;
	}

	private void offerFight() {
		GameScene.show(new WndOptions(DemonSprites.icon(lord), Messages.titleCase(lord.fullTitle()),
				DemonHooks.line(lord, "challenge"),
				Messages.get(DemonLordNPC.class, "fight"),
				Messages.get(DemonLordNPC.class, "later")) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) startFight();
				else yell(DemonHooks.line(lord, "wait"));
			}
		});
	}

	private void startFight() {
		if (!isAlive() || Dungeon.level == null) return;
		yell(DemonHooks.line(lord, "boss_notice"));
		//the true form waits in the lord's own domain; this avatar stays here until it is defeated
		//(so the hero can go back in if it ever leaves the domain some other way)
		DemonHooks.travelToArena();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("lord", lord.name());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			lord = DemonLord.valueOf(bundle.getString("lord"));
		} catch (Exception e) {
			lord = DemonLord.LUCIFER;
		}
	}
}
