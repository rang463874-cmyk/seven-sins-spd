package com.shatteredpixel.shatteredpixeldungeon.devtools;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Dev tools: summon a monster on a chosen tile. The list comes from the game's own bestiary
 * categories. Bosses other than Goo are left out, since they need their own boss levels;
 * the sewer prophet's Seal Warden is listed with Goo.
 */
public class WndDevMobs extends Window {

	static final int WIDTH = WndDevTools.WIDTH;
	static final int PER_PAGE = 10;

	static final String[] CAT_NAMES = {"지역 몬스터", "공통 몬스터", "희귀 몬스터", "퀘스트 몬스터", "보스·미니보스"};

	static ArrayList<Class<? extends Mob>> mobs(int cat) {
		ArrayList<Class<? extends Mob>> out = new ArrayList<>();
		if (cat == 4) {
			out.add(Goo.class);
			out.add(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SealWarden.class);
			return out;
		}
		Bestiary b = new Bestiary[]{Bestiary.REGIONAL, Bestiary.UNIVERSAL, Bestiary.RARE, Bestiary.QUEST}[cat];
		for (Class<?> c : b.entities()) {
			if (!Mob.class.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers())) continue;
			if (c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers())) continue;
			@SuppressWarnings("unchecked")
			Class<? extends Mob> mc = (Class<? extends Mob>) c;
			out.add(mc);
		}
		return out;
	}

	static String nameOf(Class<? extends Mob> c) {
		try {
			Mob m = Reflection.newInstance(c);
			return m == null ? c.getSimpleName() : m.name();
		} catch (Throwable t) {
			return c.getSimpleName();
		}
	}

	/** category list */
	public WndDevMobs() {
		super();
		float y = WndDevTools.title(this, "몬스터 소환");
		for (int i = 0; i < CAT_NAMES.length; i++) {
			final int cat = i;
			RedButton b = new RedButton(CAT_NAMES[i] + " (" + mobs(i).size() + ")", 8) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndMobList(cat, 0));
				}
			};
			add(b);
			b.setRect(0, y, WIDTH, 18);
			y += 18 + 2;
		}
		resize(WIDTH, (int) y - 2);
	}

	public static class WndMobList extends Window {

		public WndMobList(final int cat, final int page) {
			super();
			final ArrayList<Class<? extends Mob>> list = mobs(cat);
			final int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
			float y = WndDevTools.title(this, CAT_NAMES[cat] + "  " + (page + 1) + "/" + pages);

			int start = page * PER_PAGE;
			int end = Math.min(list.size(), start + PER_PAGE);
			for (int i = start; i < end; i++) {
				final Class<? extends Mob> c = list.get(i);
				RedButton b = new RedButton(nameOf(c), 7) {
					@Override
					protected void onClick() {
						hide();
						GameScene.selectCell(new Summoner(c));
					}
				};
				add(b);
				b.setRect(0, y, WIDTH, 15);
				y += 15 + 1;
			}
			y += 3;

			float bw = (WIDTH - 4) / 3f;
			RedButton prev = new RedButton("<", 8) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndMobList(cat, page - 1));
				}
			};
			prev.enable(page > 0);
			add(prev);
			prev.setRect(0, y, bw, 16);

			RedButton back = new RedButton("분류", 8) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndDevMobs());
				}
			};
			add(back);
			back.setRect(bw + 2, y, bw, 16);

			RedButton next = new RedButton(">", 8) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndMobList(cat, page + 1));
				}
			};
			next.enable(page < pages - 1);
			add(next);
			next.setRect(2 * (bw + 2), y, bw, 16);

			resize(WIDTH, (int) (y + 16));
		}
	}

	/** places the chosen monster on the tapped tile */
	static class Summoner extends CellSelector.Listener {
		private final Class<? extends Mob> cls;

		Summoner(Class<? extends Mob> cls) {
			this.cls = cls;
		}

		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			if (!Dungeon.level.passable[cell] || Dungeon.level.solid[cell] || Actor.findChar(cell) != null) {
				GLog.w("그곳에는 소환할 수 없습니다.");
				return;
			}
			Mob m;
			try {
				m = Reflection.newInstance(cls);
			} catch (Throwable t) {
				m = null;
			}
			if (m == null) {
				GLog.w("이 몬스터는 소환할 수 없습니다.");
				return;
			}
			m.pos = cell;
			m.state = m.WANDERING;
			GameScene.add(m);
			Dungeon.level.occupyCell(m);
			GLog.p(m.name() + "을(를) 소환했습니다.");
		}

		@Override
		public String prompt() {
			return "소환할 칸을 선택하세요";
		}
	}
}
