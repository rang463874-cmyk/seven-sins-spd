package com.shatteredpixel.shatteredpixeldungeon.skills;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * A SinSkill put together from small lambdas, so signature items can declare their skills
 * in a few lines each:
 *
 *   new BasicSkill(WarDrum.class, "provoke", 16, CostType.CHARGES)
 *       .cost(h -> COST_PROVOKE)
 *       .usableIf(h -> grudge(h).charges >= COST_PROVOKE)
 *       .onUse(h -> GameScene.selectCell(provoke));
 */
public class BasicSkill extends SinSkill {

	private final CostType type;
	private ToIntFunction<Hero> cost = h -> 0;
	private Predicate<Hero> usable = h -> true;
	private Predicate<Hero> visible = h -> true;
	private Consumer<Hero> use = h -> {};
	private Function<Hero, Object[]> args = h -> new Object[0];
	private Function<Hero, String> nameKey = null;

	public BasicSkill(Class<?> owner, String id, int icon, CostType type) {
		super(owner, id, icon);
		this.type = type;
	}

	public BasicSkill cost(ToIntFunction<Hero> cost) {
		this.cost = cost;
		return this;
	}

	public BasicSkill usableIf(Predicate<Hero> usable) {
		this.usable = usable;
		return this;
	}

	public BasicSkill visibleIf(Predicate<Hero> visible) {
		this.visible = visible;
		return this;
	}

	public BasicSkill onUse(Consumer<Hero> use) {
		this.use = use;
		return this;
	}

	public BasicSkill descArgs(Function<Hero, Object[]> args) {
		this.args = args;
		return this;
	}

	/** picks a different name string ("skill_[returned id]") depending on the situation */
	public BasicSkill nameFrom(Function<Hero, String> nameKey) {
		this.nameKey = nameKey;
		return this;
	}

	@Override
	public String name(Hero hero) {
		if (nameKey != null) {
			return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(owner, "skill_" + nameKey.apply(hero));
		}
		return super.name(hero);
	}

	@Override
	protected Object[] descArgs(Hero hero) {
		return args.apply(hero);
	}

	@Override
	public CostType costType() {
		return type;
	}

	@Override
	public int cost(Hero hero) {
		return cost.applyAsInt(hero);
	}

	@Override
	public boolean usable(Hero hero) {
		return usable.test(hero);
	}

	@Override
	public boolean visible(Hero hero) {
		return visible.test(hero);
	}

	@Override
	public void use(Hero hero) {
		use.accept(hero);
	}
}
