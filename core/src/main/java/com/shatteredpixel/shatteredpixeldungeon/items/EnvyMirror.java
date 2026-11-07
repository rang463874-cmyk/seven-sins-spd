package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.envy.EnvyMimicry;
import com.shatteredpixel.shatteredpixeldungeon.envy.EnvyMimicry.Growth;
import com.shatteredpixel.shatteredpixeldungeon.envy.EnvyMimicry.Kind;
import com.shatteredpixel.shatteredpixeldungeon.envy.EnvyMimicry.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.BasicSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSkills;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;

import java.util.ArrayList;

/**
 * The Envy class's signature mirror. It holds a few mimicked skills (other sins' skills and
 * bestiary monsters' abilities, see EnvyMimicry) paid for from the jealousy gauge, and lets
 * Envy pick another sin's growth to imitate.
 */
public class EnvyMirror extends SinArtifact {

	public static final String AC_GAZE = "GAZE";
	public static final String AC_CODEX = "CODEX";

	{
		image = ItemSpriteSheet.ENVY_MIRROR;
		defaultAction = AC_GAZE;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (isEquipped(hero)) actions.add(AC_GAZE);
		actions.add(AC_CODEX);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_GAZE)) return Messages.get(this, "ac_gaze");
		if (action.equals(AC_CODEX)) return Messages.get(this, "ac_codex");
		return super.actionName(action, hero);
	}

	static EnvyCovet covet(Hero hero) {
		return Buff.affect(hero, EnvyCovet.class);
	}

	public static boolean royal(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS);
	}

	/**
	 * Drags an enemy's health ratio down to the hero's. Bosses only lose half the difference.
	 * Used by the Downfall armor ability. Returns damage dealt.
	 */
	public static int dragDown(Hero hero, Char enemy, float mult, Object src) {
		float heroRatio = hero.HP / (float) hero.HT;
		int target = Math.max(1, (int) Math.ceil(heroRatio * enemy.HT));
		if (enemy.HP <= target) return 0;
		int dmg = enemy.HP - target;
		if (royal(enemy)) dmg /= 2;
		dmg = Math.round(dmg * mult);
		dmg = Math.min(dmg, enemy.HP - 1);
		if (dmg <= 0) return 0;
		enemy.damage(dmg, src);
		return dmg;
	}

	// ------------------------------------------------------------------ skills list

	@Override
	public String skillHeader(Hero hero) {
		EnvyCovet c = covet(hero);
		return Messages.get(this, "skill_header", (int) c.jealousy, Math.round(EnvyCovet.MAX_JEALOUSY),
				c.slots.size(), c.maxSlots(), c.growth.title());
	}

	@Override
	protected String levelEffects(Hero hero) {
		return Messages.get(SinArtifact.class, "effects_jealousy", Math.round((1f / SinArtifact.rechargeScale(hero) - 1f) * 100));
	}

	@Override
	public ArrayList<SinSkill> skills(final Hero hero) {
		ArrayList<SinSkill> list = new ArrayList<>();
		EnvyCovet c = covet(hero);
		for (final Mimic m : new ArrayList<>(c.slots)) {
			list.add(new BasicSkill(EnvyMirror.class, m.id(), m.icon, SinSkill.CostType.JEALOUSY)
					.cost(h -> EnvyMimicry.cost(h, m))
					.usableIf(h -> covet(h).jealousy >= EnvyMimicry.cost(h, m))
					.descArgs(h -> EnvyMimicry.descArgs(h, m))
					.onUse(h -> EnvyMimicry.use(h, m)));
		}
		list.add(new BasicSkill(EnvyMirror.class, "copy", 40, SinSkill.CostType.NONE)
				.descArgs(h -> new Object[]{covet(h).maxSlots()})
				.onUse(EnvyMirror::chooseCategory));
		list.add(new BasicSkill(EnvyMirror.class, "growth", 41, SinSkill.CostType.NONE)
				.descArgs(h -> new Object[]{Math.round(EnvyMimicry.growthEfficiency(h) * 100), covet(h).growth.title()})
				.onUse(EnvyMirror::chooseGrowth));
		return list;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);
		final EnvyCovet c = covet(hero);

		//the bestiary can be read whether or not the mirror is worn
		if (action.equals(AC_CODEX)) {
			GameScene.show(new WndTitledMessage(new ItemSprite(this),
					Messages.get(this, "codex_title", c.codex.size(), EnvyCovet.CODEX.size()), c.codexText()));
			return;
		}
		if (!canUseSkills(hero, action)) return;
		if (!action.equals(AC_GAZE)) return;

		c.refreshSlots();
		GameScene.show(new WndSkills(this, hero));
	}

	// ------------------------------------------------------------------ choosing what to copy

	private static void chooseCategory(final Hero hero) {
		covet(hero).refreshSlots();
		GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.ENVY_MIRROR),
				Messages.get(EnvyMirror.class, "copy_title"),
				Messages.get(EnvyMirror.class, "copy_msg", covet(hero).slots.size(), covet(hero).maxSlots()),
				Messages.get(EnvyMirror.class, "copy_sins"), Messages.get(EnvyMirror.class, "copy_monsters")) {
			@Override
			protected void onSelect(int index) {
				chooseMimic(hero, index == 0 ? Kind.SIN : Kind.MONSTER);
			}
		});
	}

	private static void chooseMimic(final Hero hero, final Kind kind) {
		final EnvyCovet c = covet(hero);
		final ArrayList<Mimic> options = new ArrayList<>();
		ArrayList<String> labels = new ArrayList<>();
		Mimic nextLocked = null;
		int locked = 0;
		for (Mimic m : Mimic.values()) {
			if (m.kind != kind) continue;
			if (!m.unlocked(hero, c)) {
				locked++;
				if (nextLocked == null && kind == Kind.SIN) nextLocked = m;
				continue;
			}
			options.add(m);
			labels.add((c.slots.contains(m) ? "[v] " : "[ ] ") + m.title());
		}
		String msg;
		if (kind == Kind.SIN) {
			msg = nextLocked == null ? Messages.get(EnvyMirror.class, "copy_all_sins")
					: Messages.get(EnvyMirror.class, "copy_next_sin", nextLocked.level, nextLocked.title());
		} else {
			msg = Messages.get(EnvyMirror.class, "copy_monsters_msg", locked);
		}
		if (options.isEmpty()) {
			GameScene.show(new WndTitledMessage(new ItemSprite(ItemSpriteSheet.ENVY_MIRROR),
					Messages.get(EnvyMirror.class, "copy_title"), msg));
			return;
		}
		GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.ENVY_MIRROR),
				Messages.get(EnvyMirror.class, "copy_title"),
				msg + "\n\n" + Messages.get(EnvyMirror.class, "copy_toggle", c.slots.size(), c.maxSlots()),
				labels.toArray(new String[0])) {
			@Override
			protected void onSelect(int index) {
				Mimic m = options.get(index);
				if (c.slots.contains(m)) {
					c.slots.remove(m);
				} else if (c.slots.size() >= c.maxSlots()) {
					GLog.w(Messages.get(EnvyMirror.class, "slots_full"));
					return;
				} else {
					c.slots.add(m);
				}
				BuffIndicator.refreshHero();
				Item.updateQuickslot();
				chooseMimic(hero, kind);
			}

			@Override
			protected boolean hasInfo(int index) {
				return true;
			}

			@Override
			protected void onInfo(int index) {
				Mimic m = options.get(index);
				GameScene.show(new WndTitledMessage(new ItemSprite(ItemSpriteSheet.ENVY_MIRROR), m.title(),
						Messages.get(EnvyMirror.class, "skill_" + m.id() + "_desc", EnvyMimicry.descArgs(hero, m))
								+ "\n\n" + m.source()
								+ "\n" + Messages.get(EnvyMirror.class, "copy_cost", EnvyMimicry.cost(hero, m))));
			}
		});
	}

	private static void chooseGrowth(final Hero hero) {
		final EnvyCovet c = covet(hero);
		final Growth[] all = Growth.values();
		String[] labels = new String[all.length];
		for (int i = 0; i < all.length; i++) {
			labels[i] = (c.growth == all[i] ? "[v] " : "[ ] ") + all[i].title();
		}
		GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.ENVY_MIRROR),
				Messages.get(EnvyMirror.class, "growth_title"),
				Messages.get(EnvyMirror.class, "growth_msg", Math.round(EnvyMimicry.growthEfficiency(hero) * 100)),
				labels) {
			@Override
			protected void onSelect(int index) {
				c.growth = all[index];
				hero.updateHT(true);
				BuffIndicator.refreshHero();
				GLog.i(Messages.get(EnvyMirror.class, "growth_set", c.growth.title()));
			}

			@Override
			protected boolean hasInfo(int index) {
				return index > 0;
			}

			@Override
			protected void onInfo(int index) {
				GameScene.show(new WndTitledMessage(new ItemSprite(ItemSpriteSheet.ENVY_MIRROR),
						all[index].title(), all[index].desc()));
			}
		});
	}

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		EnvyCovet c = EnvyCovet.of();
		return c == null ? null : Integer.toString((int) c.jealousy);
	}

	@Override
	public String desc() {
		String s = Messages.get(this, "desc");
		EnvyCovet c = EnvyCovet.of();
		if (c != null) s += "\n\n" + c.statsText();
		return s;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}
}
