package com.shatteredpixel.shatteredpixeldungeon.devtools;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * Developer tools window for build testing: item spawner plus a few hero shortcuts.
 * Opened from a button in the in-game menu (WndGame).
 */
public class WndDevTools extends Window {

	/**
	 * Whether the dev tools button appears in the in-game menu.
	 * Default: only in debug builds (Android Studio "Run" / desktop debug, version ends in -INDEV).
	 * Change to {@code true} to also show it in release builds.
	 */
	public static boolean enabled() {
		return DeviceCompat.isDebug();
	}

	static final int WIDTH = 120;
	static final int BTN_H = 18;
	static final int GAP = 2;

	/** Called from WndGame. Returns null when dev tools are off or there is no live hero. */
	public static RedButton menuButton(final Window owner) {
		Hero hero = Dungeon.hero;
		if (!enabled() || hero == null || !hero.isAlive()) return null;
		RedButton b = new RedButton("개발자 도구") {
			@Override
			protected void onClick() {
				owner.hide();
				GameScene.show(new WndDevTools());
			}
		};
		b.textColor(0x66FFCC);
		return b;
	}

	private int pos = 0;

	public WndDevTools() {
		super();
		pos = title(this, "개발자 도구");

		addBtn(new RedButton("아이템 생성") {
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndCategories());
			}
		});
		addBtn(new RedButton("칠죄종 스택") {
			@Override
			protected void onClick() {
				hide();
				WndDevSins.open();
			}
		});
		addBtn(new RedButton("몬스터 소환") {
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndDevMobs());
			}
		});
		addBtn(new RedButton("레벨 +1") {
			@Override
			protected void onClick() {
				Hero h = Dungeon.hero;
				if (h == null || h.lvl >= Hero.MAX_LEVEL) return;
				h.earnExp(h.maxExp() - h.exp, WndDevTools.class);
			}
		});
		addBtn(new RedButton("체력 완전 회복") {
			@Override
			protected void onClick() {
				Hero h = Dungeon.hero;
				if (h == null) return;
				h.HP = h.HT;
				h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
				GLog.p("체력을 회복했습니다.");
			}
		});
		addBtn(new RedButton("소지품 전체 감정") {
			@Override
			protected void onClick() {
				Hero h = Dungeon.hero;
				if (h == null) return;
				for (Item it : h.belongings) it.identify();
				Item.updateQuickslot();
				GLog.p("모든 소지품을 감정했습니다.");
			}
		});
		addBtn(new RedButton("대천사 소환") {
			@Override
			protected void onClick() {
				hide();
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.ArchangelBoss a =
						new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.ArchangelBoss();
				a.virtue = com.shatteredpixel.shatteredpixeldungeon.virtues.VirtueHooks.angelFor(Dungeon.hero);
				for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) {
					int c = Dungeon.hero.pos + n * 2;
					if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]
							&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c) == null) {
						a.pos = c;
						a.state = a.HUNTING;
						com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(a);
						a.notice();
						return;
					}
				}
				GLog.w("소환할 자리가 없습니다.");
			}
		});
		addBtn(new RedButton("천상 27층으로 이동") {
			@Override
			protected void onClick() {
				hide();
				devTravel(27);
			}
		});
		addBtn(new RedButton("신성 거인(31층)으로 이동") {
			@Override
			protected void onClick() {
				hide();
				devTravel(31);
			}
		});
		addBtn(new RedButton("마왕 단계 진행") {
			@Override
			protected void onClick() {
				hide();
				com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks.devAdvance(Dungeon.hero);
			}
		});
		resize(WIDTH, pos - GAP);
	}

	private static void devTravel(int depth) {
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.mode =
				com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.Mode.RETURN;
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.returnDepth = depth;
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.returnBranch = 0;
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.returnPos = -1;
		com.watabou.noosa.Game.switchScene(com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.class);
	}

	private void addBtn(RedButton b) {
		add(b);
		b.setRect(0, pos, WIDTH, BTN_H);
		pos += BTN_H + GAP;
	}

	static int title(Window w, String text) {
		RenderedTextBlock title = PixelScene.renderTextBlock(text, 9);
		title.hardlight(Window.TITLE_COLOR);
		title.setPos((WIDTH - title.width()) / 2f, 0);
		w.add(title);
		return (int) title.bottom() + 4;
	}

	// ------------------------------------------------------------------ categories

	public static class WndCategories extends Window {
		public WndCategories() {
			super();
			float y = title(this, "아이템 분류");
			String[][] cats = DevItemList.CATEGORIES;
			float half = (WIDTH - GAP) / 2f;
			for (int i = 0; i < cats.length; i++) {
				final int idx = i;
				RedButton b = new RedButton(cats[i][0] + " (" + (cats[i].length - 1) + ")", 7) {
					@Override
					protected void onClick() {
						hide();
						GameScene.show(new WndItems(idx, 0));
					}
				};
				add(b);
				float x = (i % 2 == 0) ? 0 : half + GAP;
				b.setRect(x, y, half, 16);
				if (i % 2 == 1 || i == cats.length - 1) y += 16 + GAP;
			}
			resize(WIDTH, (int) y - GAP);
		}
	}

	// ------------------------------------------------------------------ item grid

	public static class WndItems extends Window {
		static final int COLS = 5;
		static final int ROWS = 5;
		static final int SLOT = 22;
		static final int PER_PAGE = COLS * ROWS;

		public WndItems(final int cat, final int page) {
			super();
			final String[] row = DevItemList.CATEGORIES[cat];
			final ArrayList<String> classes = new ArrayList<>();
			for (int i = 1; i < row.length; i++) classes.add(row[i]);
			final int pages = Math.max(1, (classes.size() + PER_PAGE - 1) / PER_PAGE);

			float top = title(this, row[0] + "  " + (page + 1) + "/" + pages);
			float gap = (WIDTH - COLS * SLOT) / (float) (COLS - 1);

			int start = page * PER_PAGE;
			int end = Math.min(classes.size(), start + PER_PAGE);
			for (int i = start; i < end; i++) {
				final String cn = classes.get(i);
				Item preview = create(cn);
				if (preview == null) continue;
				int n = i - start;
				ItemSlot slot = new ItemSlot(preview) {
					@Override
					protected void onClick() {
						GameScene.show(new WndSpawn(cn));
					}
				};
				add(slot);
				slot.setRect((n % COLS) * (SLOT + gap), top + (n / COLS) * (SLOT + gap), SLOT, SLOT);
			}
			float y = top + ROWS * (SLOT + gap);

			float bw = (WIDTH - 2 * GAP) / 3f;
			RedButton prev = new RedButton("<", 8) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndItems(cat, page - 1));
				}
			};
			prev.enable(page > 0);
			add(prev);
			prev.setRect(0, y, bw, 16);

			RedButton back = new RedButton("분류", 8) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndCategories());
				}
			};
			add(back);
			back.setRect(bw + GAP, y, bw, 16);

			RedButton next = new RedButton(">", 8) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndItems(cat, page + 1));
				}
			};
			next.enable(page < pages - 1);
			add(next);
			next.setRect(2 * (bw + GAP), y, bw, 16);

			resize(WIDTH, (int) (y + 16));
		}
	}

	// ------------------------------------------------------------------ spawn dialog

	public static class WndSpawn extends Window {
		private final String cn;
		private final Item sample;
		private int level = 0;
		private int qty = 1;
		private boolean identify = true;

		private RenderedTextBlock levelText;
		private RenderedTextBlock qtyText;

		public WndSpawn(String cn) {
			super();
			this.cn = cn;
			this.sample = create(cn);
			build();
		}

		private void build() {
			ItemSlot icon = new ItemSlot(sample);
			add(icon);
			icon.setRect(0, 0, 18, 18);

			RenderedTextBlock name = PixelScene.renderTextBlock(safeName(sample), 9);
			name.hardlight(Window.TITLE_COLOR);
			name.maxWidth(WIDTH - 22);
			name.setPos(22, 4);
			add(name);
			float y = Math.max(18, name.bottom()) + 4;

			String shortName = cn.substring(cn.lastIndexOf('.') + 1);
			RenderedTextBlock cls = PixelScene.renderTextBlock(shortName, 6);
			cls.hardlight(0x999999);
			cls.setPos(0, y);
			add(cls);
			y = cls.bottom() + 4;

			boolean upgradable = sample != null && (sample.isUpgradable() || sample instanceof Artifact);
			if (upgradable) {
				levelText = PixelScene.renderTextBlock("", 8);
				add(levelText);
				y = stepper(y, levelText, new int[]{-5, -1, 1, 5}, true);
			}
			boolean countable = sample != null && (sample.stackable || sample instanceof Gold || sample instanceof EnergyCrystal);
			if (countable) {
				qtyText = PixelScene.renderTextBlock("", 8);
				add(qtyText);
				y = stepper(y, qtyText, new int[]{-10, -1, 1, 10}, false);
			}
			refreshTexts();

			final CheckBox idBox = new CheckBox("감정된 상태로 생성") {
				@Override
				protected void onClick() {
					super.onClick();
					identify = checked();
				}
			};
			idBox.checked(identify);
			add(idBox);
			idBox.setRect(0, y, WIDTH, 16);
			y += 16 + GAP;

			RedButton spawn = new RedButton("생성") {
				@Override
				protected void onClick() {
					spawn(cn, level, qty, identify);
				}
			};
			spawn.textColor(0xFFFF44);
			add(spawn);
			spawn.setRect(0, y, WIDTH, 18);
			y += 18;

			resize(WIDTH, (int) y);
		}

		private float stepper(float y, RenderedTextBlock label, int[] deltas, final boolean isLevel) {
			label.setPos(0, y + 4);
			float bx = 46;
			float bw = (WIDTH - bx - (deltas.length - 1)) / deltas.length;
			for (int i = 0; i < deltas.length; i++) {
				final int d = deltas[i];
				RedButton b = new RedButton((d > 0 ? "+" : "") + d, 7) {
					@Override
					protected void onClick() {
						if (isLevel) level = clamp(level + d, -3, 99);
						else qty = clamp(qty + d, 1, 9999);
						refreshTexts();
					}
				};
				add(b);
				b.setRect(bx + i * (bw + 1), y, bw, 16);
			}
			return y + 16 + GAP;
		}

		private void refreshTexts() {
			if (levelText != null) levelText.text("강화 " + (level >= 0 ? "+" : "") + level);
			if (qtyText != null) qtyText.text("수량 " + qty);
		}
	}

	// ------------------------------------------------------------------ helpers

	static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	static Item create(String cn) {
		try {
			Class<?> c = Class.forName(cn);
			return (Item) Reflection.newInstance(c);
		} catch (Throwable t) {
			return null;
		}
	}

	static String safeName(Item it) {
		if (it == null) return "?";
		try {
			return it.name();
		} catch (Throwable t) {
			return it.getClass().getSimpleName();
		}
	}

	static void spawn(String cn, int level, int qty, boolean identify) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return;
		Item item = create(cn);
		if (item == null) {
			GLog.w("생성 실패: " + cn);
			return;
		}
		try {
			if (item instanceof Gold) {
				Dungeon.gold += qty;
				GLog.p("골드 +" + qty);
				Sample.INSTANCE.play("sounds/gold.mp3");
				return;
			}
			if (item instanceof EnergyCrystal) {
				Dungeon.energy += qty;
				GLog.p("에너지 +" + qty);
				Sample.INSTANCE.play("sounds/item.mp3");
				return;
			}
			if (level != 0) {
				if (item instanceof Artifact) {
					int cap = ((Artifact) item).levelCap();
					int n = Math.min(Math.max(level, 0), cap);
					for (int i = 0; i < n; i++) item.upgrade();
				} else if (item.isUpgradable()) {
					item.level(level);
				}
			}
			if (item.stackable) item.quantity(qty);
			if (identify) item.identify();

			if (item.collect(hero.belongings.backpack)) {
				GLog.p("생성: " + safeName(item));
			} else {
				Dungeon.level.drop(item, hero.pos).sprite.drop();
				GLog.i("가방이 가득 차서 발밑에 떨어뜨렸습니다: " + safeName(item));
			}
			Sample.INSTANCE.play("sounds/item.mp3");
			Item.updateQuickslot();
		} catch (Throwable t) {
			GLog.w("생성 중 오류: " + t);
		}
	}
}
