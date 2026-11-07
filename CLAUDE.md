# Seven Sins mod for Shattered Pixel Dungeon

A mod of Shattered Pixel Dungeon v4.0.2 (GPLv3) that adds seven "Seven Deadly Sins" hero classes.
The owner plays and tests on an Android phone and talks in Korean, so reply in Korean.

## Current state

Done and pushed (see `git log`):
- Developer tools (in-game menu, debug builds only): item spawner, sin stacks (`devtools/WndDevSins`:
  +10/+100/+1000 to the current sin's points, refill charges/gauge/HP), monster summon
  (`devtools/WndDevMobs`: bestiary categories, tap a tile; bosses except Goo left out), level up, heal,
  identify all.
- Cleric is hidden from hero select (kept for old saves). Hero select fits 12 classes.
- Seven sin classes, each with: signature artifact (`items/artifacts/SinArtifact`, starts worn in the
  artifact slot; growth/stats always work, skills and the skill bar only while worn; levels +0..+10 like
  the cloak of shadows from resource spent via `SinArtifact.onSpent`, 3+level per level: charge sins +1 max
  charge per 3 levels and up to +60% recharge, Lust -30% HP costs, Greed +60% gauge, Gluttony -33% satiety
  costs and +50% consumable points), a permanent-growth buff,
  2 subclasses, 26 talents, 3 armor abilities, class armor, en/ko strings, placeholder art.
- Tier 1 sin weapons (starting weapons) and tier 5 sin weapons (via the blacksmith's
  Sigil of Sin, 2000 favor, keeps upgrades/enchantment). Effects only work for the owning sin.
- Imp quest vault mirror gives `items/MirrorCase` (every class; originals get it alongside their usual
  reward): one use per run, sets `Belongings.extraSlot`, opening `Belongings.misc2`, a second shared
  artifact/ring slot (equip logic in `KindofMisc`, shown in `WndBag`; `InventoryPane` lists it first in
  the bag). The old mirror relics (`MirrorRelics`, buffs' `mirrored` flag) are kept only for old saves.
- GitHub Actions builds a debug APK on every push and publishes it to the `test-build`
  pre-release: https://github.com/rang463874-cmyk/seven-sins-spd/releases/tag/test-build
- Four horsemen relics (all classes) from the sewer "prophet of seals" quest, see below.
- Elite monsters (Nov 2026, `elites/EliteHooks`, called from the end of `RegularLevel.createMobs`, `Mob.die`,
  `Mob.name`; region record saved with the quests in `Dungeon`). Sin elites (`elites/SinElite`, built on
  `ChampionEnemy`: aura, factors and attack procs are already wired; +50% HT once via `boosted`, x2 exp, name
  prefix; 7 kinds gluttony..sloth) appear on 25% of floors 2-24. `Char.buff(Class)` matches exact classes, so use
  `EliteHooks.sinElite(ch)`. Region elites (`actors/mobs/elites/`, base `RegionElite` = Seal Warden style telegraphed
  skills via `prepare/release`): RatChieftain (sewers), ChainWarden (prison), GnollForeman (caves), FallenPriest
  (city), Hellhound twins (halls, `twin` id, survivor enrages); one per region (chance 1/floors left, forced on the
  region's last regular floor), listed in the bestiary's QUEST group. Drops: sin elite 1 `items/SinEssence` (+ own
  sin: `sins/SinRewards.grant` 15 pts; greedy: stolen gold x2), region elite 2 essences + uncursed +1
  weapon/armor/ring (`EliteHooks.regionReward`).
- Alchemy (Nov 2026): `items/elite/ElixirOfSin` (2 essences + any potion: +20 sin points and resource refill,
  else full heal + bless), `items/bombs/SinBomb` (bomb + essence, random sin debuff), `items/bombs/StickyBomb`
  (bomb + earthroot seed, roots + webs, no blast; both in `Bomb.EnhanceBomb`), `items/elite/Bandage` (2 sungrass
  seeds -> 2), `items/elite/Stimulant` (swiftthistle + blindweed seeds, instant haste/adrenaline). Art: items.png row 37.
- Remains items for every sin (`items/remains/`: MawFang, MeltedCard, TornDrumhead, CrackedGem,
  BrokenPerfume, MirrorShard, PillowStuffing; dispatched in `RemainsItem.get`; art row 36 +5..11).
  CrackedGem's `Ward` buff is checked in `Hero.defenseProc`.

Not yet done / known gaps:
- Never play-tested on a device until the first APK; expect UI/layout fixes.
- Balance is untuned (Hibernator armor, Pride's Duel, Banker interest are on the strong side).
- Sigil costs 2000 favor (lowered from 3000, the quest maximum, in Nov 2026).
- Talent/hero icons for the sins are recolored cleric icons (placeholders).

Design rule for every sin: progress is **permanent and unbounded** (stat bonuses scale with
a points value that never decreases; accuracy/evasion are capped at 40%/30%).
Melee/max HP/armor growth from sin points was halved again for all seven sins (Nov 2026, in each buff's
`meleeBonus/hpBonus/drMax`; accuracy/evasion and unique stats unchanged). Max HP growth from sin points was halved (Nov 2026), and starvation damage scales with `Hero.starvationHT`
(max HP before the sins' bonus, set in `updateHT`), not the full HT.
Exception (owner's call): Greed's stats follow the gold currently carried, so shopping lowers them.
Greed's card has no deposit/withdraw; payment skills and Remote Sale (shop price, anywhere)
spend a gauge filled 1:1 by gold pickups (`GreedHooks.selling` keeps sale proceeds from filling it).
Greed also has Mail Order (`GreedVault.catalog`, 8 consumables restocked per floor, gold at shop price +
gauge fee) and Gilding (+1 upgrade for 300 + 150 per previous use of gauge, `GreedVault.gilded`).
Sloth (reworked Nov 2026, "auto-battle"): `actors/mobs/npcs/DreamPuppet` allies (DirectableAlly that roam the
level on their own via a `Roaming` wandering state, `intelligentAlly = false`; they vanish when the hero changes floors (`Mob.holdAllies` drops them); max puppets +1 at pillow +3/+5/+10 (`SlothRepose.pillowPuppets`); `nightmare` = temporary 70% copy). Taunt = `SlothRepose.Taunted` on an enemy, read by
`SlothHooks.tauntTarget` in `Mob.chooseEnemy`. Sleepwalker/Auto Hunt free attacks run in `SlothRepose.act`.
Pillow skills also include Order (`SlothRepose.DreamMark` on one enemy: every puppet hunts it via
`DreamPuppet.chooseEnemy`/`act`, +25% damage, 30 turns) and Dream Forge (`SlothRepose.DreamForge`: paralysed deep sleep
of 200 + 25/upgrade turns (max 300), then +1 to the equipped weapon or armor; any damage wakes and cancels it via
`SlothHooks.modifyDamage`; once per floor, `SlothRepose.forgedDepths`; can't start with enemies in view).
Old sloth talent names are mapped in `Talent.renamedTalents`; old subclass saves fall back to none.
Gluttony (reworked Nov 2026, "eats anything"): no more equipment devouring (that moved to the conquest gun).
`PredatorStomach.consume` runs from `PredatorHooks.onConsumed` for potions (`Potion.drink`), scrolls
(`Scroll.readAnimation` -> `onScrollRead`), food (`Food.execute` -> `onFoodEaten`), runestones
(`Runestone.onThrow`, `InventoryStone.useAnimation`) and eaten seeds (`Plant.Seed`, AC_EAT_SEED). Pools keep
their old field names: weaponPts = potions, armorPts = food/seeds, trinketPts = scrolls/stones. New kinds go in
the `codex` (class names) with milestones 5/10/20/30/40/50/60 (`PredatorStomach.CODEX_*`). Harmful potions and
scroll of rage become `GluttonImbue` buffs (`PredatorHooks.drinkOverride/readRage`), psionic blast has no
backlash. Maw skills: Bite, Hunger Dash (haste), Acid Spray, Swallow Whole (free, 3x points, identifies),
Ruminate (repeat `lastUsed`, growth items excluded via `PredatorHooks.canRepeat`); satiety costs via
`PredatorMaw.hungerCost`. Talent enum names are unchanged (meanings and strings changed); INHERITOR is now
shown as the "alchemic glutton". Old Inheritor enchant/glyph lists are kept in saves but unused.
Pride is a ranged class (Nov 2026): starts with `items/weapon/RoyalBow` (SpiritBow subclass, endless arrows,
level = hero lvl/5 + upgrades, takes scrolls of upgrade; gilded art row 37 +6/+7) in quickslot 0, crown in slot 1;
older Pride saves get the bow once via `PrideGlory.bowGiven`. `PrideHooks.onAttack` gives glory's flat bonus
(`meleeBonus`, name kept) and the dignity multiplier to MissileWeapon attacks only, and melee deals x0.7
(`MELEE_PENALTY`). KingsAegis: +30% ranged damage (`RANGED_BONUS`) and +5 max dignity (no more glory bonus).
Paragon-only crown skill Effigy (`actors/mobs/npcs/PrideEffigy`, immovable ALLY statue, taunts enemies within 4 via
`SlothRepose.Taunted`, 20 turns, excluded from `PrideHooks.alone`).
Skill resources (Nov 2026): Wrath spends **rage** (`WrathGrudge.rage`, 0..maxRage 100+, filled by hits dealt
(+4) and damage taken (100 per max HP), cools 1/turn with no enemies in view; old charge sources convert at
1 charge = 25 rage). Pride spends **dignity** stacks (`PrideGlory.useDignity`; dignity also gives +4% melee per
stack and shatters when hit); Disdain = 1-dignity ranged bolt, x dignityMult, kills count as flawless via
`PrideHooks.heroCaused` accepting `PrideCrown.class`). Sloth pays in **time**: each pillow skill ends with `SlothRepose.doze(hero, turns)`
(Doze buff + spendAndNext; `turnCost` scales with level and SLOTH_RESTFUL; a meal with SLOTH_PILLOW skips
dozing). Cost types RAGE/DIGNITY/TURNS/JEALOUSY in `SinSkill.CostType`.
Envy (reworked Nov 2026, "copycat"): no skills of its own. `envy/EnvyMimicry` lists `Mimic`s: 12 copies of other
sins' skills (unlocked by hero level 2,4..24) and 8 monster abilities (unlocked by bestiary traits); their own
implementations live there (not the other sins' code). The mirror holds `EnvyCovet.slots` (3 + Copycat +
mirrored), chosen in `EnvyMirror.chooseMimic`; newly learned copies fill free slots (`refreshSlots`).
Paid from `EnvyCovet.jealousy` (0..maxJealousy, ~3 turns per point, x2 with a rival in view). `EnvyCovet.growth`
imitates another sin's growth at 50% (+15%/pt ENVY_SWITCH) in `EnvyHooks` (gluttony via
`PredatorHooks.onConsumed`, greed = carried gold in `pts()`, wrath/pride/sloth kills in `mimicKill`, lust
lifesteal in `onAttack`). Robbed stacks are gone (`EnvyCovet.Robbed` kept only for old saves, detaches
itself); Envy's own envy comes from new bestiary entries (`EnvyHooks.registerEnvy`). USURPER is now shown as the
**Doppelganger**: imitates two growths (`growth2`, `secondEfficiency`), sin copies -25% cost. Talent enums
unchanged, meanings changed. `WndHeroInfo` tolerates more description paragraphs than icons.
Monster ability icons are row 7 of skill_icons.png.
Lust (reworked Nov 2026, dancer + vampire): perfume skills cost HP (`LustDesire.hpCost/pay`), lifesteal
on charmed enemies via `LustDesire.drain` from `LustHooks.onAttack`; dodging charms the attacker
(`LustHooks.onDodge`, called from the miss branch of `Char.attack`; Night Dancer counters there).
Siren infighting is gone. Old lust talent names are mapped in `Talent.renamedTalents` too.

Meal talents (Nov 2026): each sin's first tier-2 talent (PREDATOR_GLUTTONY, GREED_HOARD, WRATH_DRUMMER,
PRIDE_REGALIA, LUST_BOTTLE, ENVY_MIRROR, SLOTH_PILLOW) makes eating take 1 turn and refills the sin's resource
(`sins/SinMeals`, from `Talent.onFoodEaten` / `Food.eatingTime`); their old effects are gone. Sloth's meal gives
`SlothRepose.Refreshed` (next skills skip dozing). Envy's gauge max = 100 + 10/mirror level + 25/pt ENVY_POLISH
(`EnvyCovet.maxJealousy`).

Unique stats (one per sin, on top of the shared five; all in `sins/UniqueStats`, scaled by the sin's
points, capped): Wrath attack speed (Hero.attackDelay), Gluttony HP regen (ticked from Hunger.act),
Greed loot chance (Mob.lootChance), Pride debuff resistance (Char.resist, negative buffs only),
Lust move speed (Hero.speed), Envy rare variant spawns (x up to 4, via `RatSkull.exoticChanceMultiplier`;
at the cap, kills have a 5% chance of 3 turns of mind vision), Sloth slower hunger (Hunger.act).
Each sin buff's `statsText()` appends `UniqueStats.line(target)`.

## Skills and the skill bar

Every sin's signature item implements `skills/SkillHolder` and lists its active skills as
`skills/SinSkill`s (usually `BasicSkill` built from lambdas: cost type/amount, usable, onUse, desc args).
The same list drives the item's skill window (`windows/WndSkills`, name + cost + description) and the
skill bar (`ui/SkillBar`, icon buttons right above the quickslot toolbar, right-aligned; tap = use, long-press = info).
`GameScene.syncSkillBar` keeps it above the toolbar; tags/log are laid out from `toolbarAreaTop()`.
Adding a skill: add a `BasicSkill` to the item's `skills()`, strings `skill_<id>` and `skill_<id>_desc`
in the item's namespace, and an icon in `tools/sinsart/make_skill_icons.py`
(`interfaces/skill_icons.png`, 16x16, row = sin index, 8 per row; re-run the script).
Auto-aim: `BasicSkill.targetsEnemy()` / `.projectile()` mark targeted skills; after the first tap the bar picks
`SkillBar.autoTarget()` (last quickslot target, else nearest visible enemy), shows a crosshair, and a second tap
fires (`GameScene.handleCell`, projectiles via `QuickSlotButton.autoAim`). The conquest gun uses the quickslot's
`usesTargeting`. Cost labels live under `skills.sinskill.cost_*`. Two-step cell selections must open the second
selector on the next frame (`Game.runOnRenderThread`), because the scene resets the selector after
each pick (that was the Discord bug).

## Four horsemen artifacts and the sewer prophet

Reworked for combat (Nov 2026). `items/artifacts/HorsemanArtifact` (shared: 0-100% charge, full charge per use,
levels 0-10 by uses, never cursed; `timeCharges()` off for the gun) with `ConquestCrown` (class name kept for
saves, now the **gun of conquest**: loads bullets by devouring unequipped weapons/armor (tier+2*lvl each), whole thrown-weapon stacks (half that per weapon) and rings (4+3*lvl),
keeps the last good enchantment for its bullets; fire = one bullet, ranged hit, drawn as `effects/Bullet` (2px black, 2400px/s); levels by shots (damage only),
no magazine limit (`maxAmmo` = Integer.MAX_VALUE); `Subjugated` kept for old saves), `FamineScales` (hunger factor 0..1 at HUNGRY, 1.25 starving,
1.5 in `Frenzy`: attack speed/move speed/damage bonuses; power burns satiety down to HUNGRY, heals, Frenzy)
and `DeathScythe` (hits leave `DeathScythe.Mark`s = dmg*rate; reaped when marks >= HP via
`HorsemenHooks.afterDamage` in `Char.damage`; bosses burst at 15% HT; power bursts all marks in sight;
survive a lethal hit once per floor). War is now a weapon, `items/weapon/melee/WarSword` (tier 3 sword,
damage from kill stacks: +stacks/36 min, +stacks/12 max; upgrades don't add damage, each level +20%
stacks per kill). The old `WarBlade` artifact is kept only for old saves.
Hooks: `horsemen/HorsemenHooks` (Hero.attackProc/defenseProc/die/speed/attackDelay, Char.damage, Mob.die).
Quest: `actors/mobs/npcs/Prophet` (+ `Prophet.Quest`, saved with the other quests in `Dungeon`) spawns on
sewer floor 2-4: the roll happens in `SewerLevel.initRooms` (`Prophet.Quest.spawnRoom` adds
`levels/rooms/quest/SealSanctumRoom`, 11x11 dead end, 4 statues, pedestal) and `Quest.spawn` (from `createMobs`)
puts the miniboss `actors/mobs/SealWarden` on the pedestal (`home`). The warden (60 HP) never leaves the arena,
walls up the door (`sealedDoors`) once it hunts a hero inside, reopens it on death or when the hero is gone.
It shifts aspect every 25% HP lost (Conquest armor, War 1.5x attack speed, Famine weakness + satiety drain,
Death +50% vs a hero below half HP) and each aspect adds a telegraphed skill (`TargetedCell` marks, released on
its next turn; skills accumulate, newest preferred): Volley (plus shape on the hero), Charge (straight dash
through the hero, cripple), Blight (3x3, weakness/hunger, heals it), Reap (all 8 cells around it).
Wardens from old saves have `home = -1` and fight without an arena. Placeholder art = tinted armored statue. It drops one
`items/quest/SealFragment` (shown as "seal of the apocalypse"); `Prophet.FRAGMENTS` = 1 -> `WndProphet`,
pick one artifact. `SealBearer` (the old 4-champion version) is kept only for old saves. Art: items.png row 36.

## The seven sins

| Sin | Enum | Signature item | Buff (progress) | Hooks | Subclasses | Armor abilities |
|---|---|---|---|---|---|---|
| Gluttony 식탐 | GLUTTONY | PredatorMaw | PredatorStomach (consumable points + gourmet codex) | predator/PredatorHooks | GOURMAND, INHERITOR | abilities/predator/* |
| Greed 탐욕 | GREED | GoldenCard | GreedVault (stats from carried gold; payment gauge filled by gold pickups) | greed/GreedHooks | BANKER, USURER | abilities/greed/* |
| Wrath 분노 | WRATH | WarDrum | WrathGrudge (grudge from infighting) | wrath/WrathHooks | INSTIGATOR, AVENGER | abilities/wrath/* |
| Pride 교만 | PRIDE | PrideCrown | PrideGlory (glory from flawless kills, dignity) | pride/PrideHooks | MONARCH, PARAGON | abilities/pride/* |
| Lust 색욕 | LUST | Perfume | LustDesire (life stolen; 0.25 desire per HP) | lust/LustHooks | NIGHT_DANCER, CRIMSON_NOBLE | abilities/lust/* |
| Envy 시기 | ENVY | EnvyMirror | EnvyCovet (new bestiary entries, imitated growth, mimic slots) | envy/EnvyHooks | USURPER (doppelganger), COPYCAT | abilities/envy/* |
| Sloth 나태 | SLOTH | SlothPillow | SlothRepose (kills by dream puppets / auto-attacks; own kills 1/3) | sloth/SlothHooks | DREAM_LORD, SLEEPWALKER | abilities/sloth/* |

Paths are under `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/`
(items in `items/`, buffs in `actors/buffs/`, abilities in `actors/hero/abilities/`).
Charge-based sins keep their item charges inside the buff. Each buff has a `mirrored` flag
set by the old vault mirror relics (`items/MirrorRelics.java`, no longer handed out).
Sin weapons: `items/weapon/melee/sins/` (`SinWeapons` helper, each extends a base weapon).

## How the mod plugs into the game

Each sin has a `*Hooks` class with static pass-through methods. Call sites (search for `Hooks.`):
- `Hero`: `updateHT` (bonusHT), `attackSkill` (accuracy), `defenseSkill` (evasion), `drRoll`,
  `attackProc` (onAttack), `defenseProc` (onDefense), `die` (Greed insurance).
- `Char.damage`: `modifyDamage` before damage (Wrath, Pride, Envy, Sloth Proxy redirect), `afterDamage` after HP is reduced.
- `Mob.die`: `onMobDeath`; `Mob.chooseEnemy`: Wrath discord / Sloth taunt targeting;
  `Char.attack` miss branch: Lust dodge.
- `Gold.doPickUp`: bonus gold. Per-class switches: `HeroClass`, `Talent` (trees + heroic energy
  icon), `ClassArmor.upgrade`, `Icons.get`, `WndHeroInfo`, `Badges` (null-safe for new classes),
  `VaultMirror`, `WndBlacksmith`.

Adding a new class means touching all of the above; follow an existing sin as the template.

## Assets

`tools/sinsart/make_sin_assets.py` regenerates all placeholder art from the Cleric's art
(hero sheet, splash, avatar, talent/hero icons, signature items, class armor, sin weapons,
mirror relics). Re-run after editing it: `python3 tools/sinsart/make_sin_assets.py`.
Reserved layout (must match the Java constants):
- `talent_icons.png`: row 8 + k (icon 256 + 32k + i), heroic energy at +26
- `hero_icons.png`: row 16 + k (icon 128 + 8k): sub1, sub2, ability 1-3 (`HeroIcon`)
- `items.png` (TX_HEIGHT in `ItemSpriteSheet` must equal the PNG height, now 592):
  row 33 signature item/class armor pairs, row 34 sin weapons + sigil, row 35 mirror relics,
  row 36 horsemen relics (gun, war sword, scales, scythe) + seal fragment (+0..4), sin remains (+5..11), mirror case (+12),
  row 37 sin essence, elixir of sin, sin bomb, sticky bomb, bandage, stimulant (`elite_art`)
- `avatars.png`: frame = HeroClass ordinal (sins are 6..12)
k = sin index in `SINS` (gluttony 0, greed 1, wrath 2, pride 3, lust 4, envy 5, sloth 6).

## Strings

English and Korean only. Source copies live in `tools/strings/*.properties` and are appended to
`core/src/main/assets/messages/{actors,items,windows}/*.properties` under
`###### Seven Sins: ... ######` headers. After editing a source copy, run `python3 tools/strings/sync.py` to update the game files
(new keys land after the key that precedes them, a new file gets its own section at the end;
delete removed keys by hand). Keep both languages in sync; a missing key shows as
"!!!NO TEXT FOUND!!!" in game. Messages fall back to superclass keys, so sin weapons inherit
their base weapon's ability text. Gluttony's strings predate the source copies; only the keys in
`gluttony_*.properties` are mirrored there, the rest live only in the game files.

## Building and checking

- Normal PC: open in Android Studio, or `./gradlew android:assembleDebug`.
- Phone testing: push to GitHub; `.github/workflows/build-apk.yml` builds and publishes the APK.
  Debug builds are signed with the committed `android/sevensins-debug.keystore` (password `android`),
  so new APKs install over old ones. Don't replace it, or the owner has to uninstall (and lose saves).
- Sandboxes without Maven/Google access: `bash tools/compilecheck/check.sh` compiles against
  libGDX built from GitHub sources (prints `exit: 0` on success). Headless logic tests can be run
  with `java -cp <out>/spdcls:<deps.jar>`; anything touching Messages/UI needs the real game.
- After adding items: regenerate the dev-tools item list with
  `CLASSES=<spdcls> CP=<deps.jar> bash tools/devitems/regen.sh`
  (on a PC: `./gradlew :core:compileJava && bash tools/devitems/regen.sh`).

Debug builds (version suffix -INDEV) unlock all classes and show the dev tools; they install
alongside official SPD (`.indev` package suffix).
