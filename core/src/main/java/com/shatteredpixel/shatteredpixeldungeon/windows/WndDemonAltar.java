package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DemonAltar;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

/** The altar's list of the seven lords; picking one shows its trial and boon before accepting. */
public class WndDemonAltar extends WndOptions {

	private final DemonAltar altar;

	private static String[] options() {
		DemonLord[] all = DemonLord.values();
		String[] out = new String[all.length];
		for (int i = 0; i < all.length; i++) {
			out[i] = DemonHooks.ownLord(all[i])
					? Messages.get(WndDemonAltar.class, "own_option", all[i].fullTitle())
					: all[i].fullTitle();
		}
		return out;
	}

	public WndDemonAltar(DemonAltar altar) {
		super(DemonSprites.altarIcon(),
				Messages.titleCase(Messages.get(DemonAltar.class, "name")),
				Messages.get(WndDemonAltar.class, "message"),
				options());
		this.altar = altar;
	}

	@Override
	protected void onSelect(int index) {
		final DemonLord lord = DemonLord.values()[index];
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new LordWindow(altar, lord));
			}
		});
	}

	public static class LordWindow extends WndOptions {

		private final DemonAltar altar;
		private final DemonLord lord;

		public LordWindow(DemonAltar altar, DemonLord lord) {
			super(DemonSprites.icon(lord),
					Messages.titleCase(lord.fullTitle()),
					DemonHooks.line(lord, "altar") + "\n\n"
							+ Messages.get(WndDemonAltar.class, "trial", lord.get("trial")) + "\n\n"
							+ Messages.get(WndDemonAltar.class, "perk", lord.get("perk")) + "\n\n"
							+ Messages.get(WndDemonAltar.class, "skill", lord.get("skill_name"), lord.get("skill_desc")),
					Messages.get(WndDemonAltar.class, "accept"),
					Messages.get(WndDemonAltar.class, "back"));
			this.altar = altar;
			this.lord = lord;
		}

		@Override
		protected void onSelect(int index) {
			if (index == 0) {
				if (DemonHooks.stage != DemonHooks.NONE) return;
				DemonHooks.beginTrial(Dungeon.hero, lord);
				GLog.n(DemonHooks.line(lord, "accepted"));
				altar.crumble();
			} else {
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndDemonAltar(altar));
					}
				});
			}
		}
	}
}
