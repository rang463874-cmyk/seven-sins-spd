package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seven Sins (Nov 23 2026, owner): the hollow ring, dropped by the dwarf king. Feeding it another ring destroys that ring
 * and teaches the hollow ring its power; feeding a kind again raises it to the higher level. Each power works at the fed
 * level - 1 (min 0); once all twelve kinds are fed it becomes the Ring of the Twelve Kings and every power works at the
 * fed level + 1. Worn like a ring; each power runs through an unseen "phantom" ring of that kind whose buff is put on the
 * wearer, so every vanilla ring hook keeps working unchanged.
 */
public class HollowRing extends Ring {

	public static final String AC_FEED = "FEED";

	{
		image = ItemSpriteSheet.HOLLOW_RING;
		defaultAction = AC_FEED;
		unique = true;
	}

	/** fed kinds (class name) -> the highest level fed */
	private final LinkedHashMap<String, Integer> fed = new LinkedHashMap<>();
	/** the phantom rings whose buffs are on the wearer */
	private final ArrayList<Ring> phantoms = new ArrayList<>();

	@SuppressWarnings("unchecked")
	public static Class<? extends Ring>[] kinds() {
		return (Class<? extends Ring>[]) Generator.Category.RING.classes;
	}

	public boolean complete() {
		for (Class<? extends Ring> k : kinds()) if (!fed.containsKey(k.getName())) return false;
		return true;
	}

	/** the level a fed power works at */
	public int powerLevel(int fedLevel) {
		return complete() ? fedLevel + 1 : Math.max(0, fedLevel - 1);
	}

	@Override
	public void reset() {
		super.reset();
		image = ItemSpriteSheet.HOLLOW_RING;
	}

	@Override
	public boolean isKnown() {
		return true;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public int image() {
		return complete() ? ItemSpriteSheet.TWELVE_KINGS_RING : ItemSpriteSheet.HOLLOW_RING;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return complete() ? new ItemSprite.Glowing(0xFFE070, 1.5f) : null;
	}

	@Override
	public String name() {
		return Messages.get(this, complete() ? "name_complete" : "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	@Override
	public String info() {
		StringBuilder sb = new StringBuilder(desc());
		sb.append("\n\n").append(Messages.get(this, complete() ? "status_complete" : "status", fed.size(), kinds().length));
		for (Class<? extends Ring> k : kinds()) {
			Integer lvl = fed.get(k.getName());
			String kind = Messages.get(k, "name");
			if (lvl == null) sb.append("\n- ").append(Messages.get(this, "missing", kind));
			else sb.append("\n- ").append(Messages.get(this, "power", kind, lvl, powerLevel(lvl)));
		}
		return sb.toString();
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_FEED);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_FEED)) return Messages.get(this, "ac_feed");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_FEED)) GameScene.selectItem(selector);
	}

	private final WndBag.ItemSelector selector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(HollowRing.class, "prompt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Ring && !(item instanceof HollowRing) && !item.isEquipped(Dungeon.hero)
					&& isKind(item.getClass());
		}

		@Override
		public void onSelect(Item item) {
			if (item instanceof Ring) feed((Ring) item);
		}
	};

	private static boolean isKind(Class<?> c) {
		for (Class<? extends Ring> k : kinds()) if (k == c) return true;
		return false;
	}

	private void feed(Ring ring) {
		Hero hero = Dungeon.hero;
		if (hero == null || ring.isEquipped(hero)) return;
		boolean wasComplete = complete();
		int lvl = Math.max(0, ring.level());
		String key = ring.getClass().getName();
		Integer old = fed.get(key);
		fed.put(key, old == null ? lvl : Math.max(old, lvl));
		ring.identify();
		String kind = ring.name();
		ring.detachAll(hero.belongings.backpack);
		GLog.p(Messages.get(this, "fed", kind, fed.get(key)));
		if (!wasComplete && complete()) GLog.h(Messages.get(this, "completed"));
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
		Sample.INSTANCE.play(Assets.Sounds.DRINK);
		if (isEquipped(hero)) activate(hero);
		updateQuickslot();
		hero.spendAndNext(1f);
	}

	// ---- the powers ----

	@Override
	public void activate(Char ch) {
		clearPhantoms();
		for (Map.Entry<String, Integer> e : fed.entrySet()) {
			try {
				Ring p = (Ring) Reflection.newInstance(Class.forName(e.getKey()));
				if (p == null) continue;
				p.cursed = false;
				p.level(powerLevel(e.getValue()));
				p.activate(ch);
				phantoms.add(p);
			} catch (ClassNotFoundException ignored) {
			}
		}
		//while a save loads Dungeon.hero is still null (ring levels read it); HT comes from the save then
		if (ch instanceof Hero && Dungeon.hero == ch) ((Hero) ch).updateHT(false);
	}

	private void clearPhantoms() {
		for (Ring p : phantoms) {
			if (p.buff != null) {
				p.buff.detach();
				p.buff = null;
			}
		}
		phantoms.clear();
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)) {
			clearPhantoms();
			hero.updateHT(false);
			return true;
		}
		return false;
	}

	@Override
	protected RingBuff buff() {
		return new RingBuff();
	}

	@Override
	public void onHeroGainExp(float levelPercent, Hero hero) {
		//nothing to learn: it is always known
	}

	@Override
	public Item random() {
		return this;
	}

	@Override
	public int value() {
		return 0;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("fed_kinds", fed.keySet().toArray(new String[0]));
		int[] lv = new int[fed.size()];
		int i = 0;
		for (int v : fed.values()) lv[i++] = v;
		bundle.put("fed_levels", lv);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		fed.clear();
		if (bundle.contains("fed_kinds")) {
			String[] k = bundle.getStringArray("fed_kinds");
			int[] lv = bundle.getIntArray("fed_levels");
			for (int i = 0; i < k.length && i < lv.length; i++) fed.put(k[i], lv[i]);
		}
	}
}
