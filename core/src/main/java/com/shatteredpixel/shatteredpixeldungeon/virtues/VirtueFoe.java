package com.shatteredpixel.shatteredpixeldungeon.virtues;

/** a servant of the virtues that can be the hero's nemesis (champions and archangels) */
public interface VirtueFoe {
	Virtue virtue();
	boolean nemesis();
}
