# Seven Sins mod for Shattered Pixel Dungeon

A mod of Shattered Pixel Dungeon v4.0.2 (GPLv3) that adds seven "Seven Deadly Sins" hero classes.
The owner plays and tests on an Android phone and talks in Korean, so reply in Korean.

## Current state

Done and pushed (see `git log`):
- Developer tools (in-game menu, debug builds only): item spawner, level up, heal, identify all.
- Cleric is hidden from hero select (kept for old saves). Hero select fits 12 classes.
- Seven sin classes, each with: signature item (no equipment slot), a permanent-growth buff,
  2 subclasses, 26 talents, 3 armor abilities, class armor, en/ko strings, placeholder art.
- Tier 1 sin weapons (starting weapons) and tier 5 sin weapons (via the blacksmith's
  Sigil of Sin, 3000 favor, keeps upgrades/enchantment). Effects only work for the owning sin.
- Imp quest vault mirror gives each sin a "reflection" of its signature item with bonuses.
- GitHub Actions builds a debug APK on every push and publishes it to the `test-build`
  pre-release: https://github.com/rang463874-cmyk/seven-sins-spd/releases/tag/test-build
- Four horsemen artifacts (all classes) from the sewer "prophet of seals" quest, see below.
- Remains items for every sin (`items/remains/`: MawFang, MeltedCard, TornDrumhead, CrackedGem,
  BrokenPerfume, MirrorShard, PillowStuffing; dispatched in `RemainsItem.get`; art row 36 +5..11).
  CrackedGem's `Ward` buff is checked in `Hero.defenseProc`.
- Gluttony's maw also has Bite: spends 60 satiety, 150% damage, heals 30% of damage dealt.

Not yet done / known gaps:
- Never play-tested on a device until the first APK; expect UI/layout fixes.
- Balance is untuned (Hibernator armor, Pride's Duel, Banker interest are on the strong side).
- Sigil costs 3000 favor, which is the quest maximum (owner's choice, may lower to 2000).
- Talent/hero icons for the sins are recolored cleric icons (placeholders).

Design rule for every sin: progress is **permanent and unbounded** (stat bonuses scale with
a points value that never decreases; accuracy/evasion are capped at 40%/30%).
Exception (owner's call): Greed's stats follow the gold currently carried, so shopping lowers them.
Greed's card has no deposit/withdraw; payment skills and Remote Sale (shop price, anywhere)
spend a gauge filled 1:1 by gold pickups (`GreedHooks.selling` keeps sale proceeds from filling it).
Sloth (reworked Nov 2026, "auto-battle"): `actors/mobs/npcs/DreamPuppet` allies (DirectableAlly that roam the
level on their own via a `Roaming` wandering state, `intelligentAlly = false`; still carried between floors; `nightmare` = temporary 70% copy). Taunt = `SlothRepose.Taunted` on an enemy, read by
`SlothHooks.tauntTarget` in `Mob.chooseEnemy`. Sleepwalker/Auto Hunt free attacks run in `SlothRepose.act`.
Old sloth talent names are mapped in `Talent.renamedTalents`; old subclass saves fall back to none.
Lust (reworked Nov 2026, dancer + vampire): perfume skills cost HP (`LustDesire.hpCost/pay`), lifesteal
on charmed enemies via `LustDesire.drain` from `LustHooks.onAttack`; dodging charms the attacker
(`LustHooks.onDodge`, called from the miss branch of `Char.attack`; Night Dancer counters there).
Siren infighting is gone. Old lust talent names are mapped in `Talent.renamedTalents` too.

Unique stats (one per sin, on top of the shared five; all in `sins/UniqueStats`, scaled by the sin's
points, capped): Wrath attack speed (Hero.attackDelay), Gluttony HP regen (ticked from Hunger.act),
Greed loot chance (Mob.lootChance), Pride debuff resistance (Char.resist, negative buffs only),
Lust move speed (Hero.speed), Envy exp (Hero.earnExp), Sloth slower hunger (Hunger.act).
Each sin buff's `statsText()` appends `UniqueStats.line(target)`.

## Skills and the skill bar

Every sin's signature item implements `skills/SkillHolder` and lists its active skills as
`skills/SinSkill`s (usually `BasicSkill` built from lambdas: cost type/amount, usable, onUse, desc args).
The same list drives the item's skill window (`windows/WndSkills`, name + cost + description) and the
skill bar (`ui/SkillBar`, icon buttons right above the quickslot toolbar; tap = use, long-press = info).
`GameScene.syncSkillBar` keeps it above the toolbar; tags/log are laid out from `toolbarAreaTop()`.
Adding a skill: add a `BasicSkill` to the item's `skills()`, strings `skill_<id>` and `skill_<id>_desc`
in the item's namespace, and an icon in `tools/sinsart/make_skill_icons.py`
(`interfaces/skill_icons.png`, 16x16, row = sin index, 8 per row; re-run the script).
Cost labels live under `skills.sinskill.cost_*`. Two-step cell selections must open the second
selector on the next frame (`Game.runOnRenderThread`), because the scene resets the selector after
each pick (that was the Discord bug).

## Four horsemen artifacts and the sewer prophet

`items/artifacts/HorsemanArtifact` (shared: 0-100% charge, full charge per use, levels 0-10 by uses,
never cursed) with `ConquestCrown` (surrender a wounded enemy -> temporary ally `Subjugated`),
`WarBlade` (more damage at low HP, charges from hits taken, Amok on nearby enemies), `FamineScales`
(Weakness aura, +10% hunger, satiety -> HP) and `DeathScythe` (reap enemies near death, survive a
lethal hit once per floor). Hooks: `horsemen/HorsemenHooks` (Hero.attackProc/defenseProc/die, Mob.die).
Quest: `actors/mobs/npcs/Prophet` (+ `Prophet.Quest`, saved with the other quests in `Dungeon`) spawns on
sewer floor 2-4 from `SewerLevel.createMobs`, with 4 champion mobs carrying `SealBearer`; each drops an
`items/quest/SealFragment`. Four fragments -> `WndProphet`, pick one artifact. Art: items.png row 36.

## The seven sins

| Sin | Enum | Signature item | Buff (progress) | Hooks | Subclasses | Armor abilities |
|---|---|---|---|---|---|---|
| Gluttony 식탐 | GLUTTONY | PredatorMaw | PredatorStomach (devour points) | predator/PredatorHooks | GOURMAND, INHERITOR | abilities/predator/* |
| Greed 탐욕 | GREED | GoldenCard | GreedVault (stats from carried gold; payment gauge filled by gold pickups) | greed/GreedHooks | BANKER, USURER | abilities/greed/* |
| Wrath 분노 | WRATH | WarDrum | WrathGrudge (grudge from infighting) | wrath/WrathHooks | INSTIGATOR, AVENGER | abilities/wrath/* |
| Pride 교만 | PRIDE | PrideCrown | PrideGlory (glory from flawless kills, dignity) | pride/PrideHooks | MONARCH, PARAGON | abilities/pride/* |
| Lust 색욕 | LUST | Perfume | LustDesire (life stolen; 0.25 desire per HP) | lust/LustHooks | NIGHT_DANCER, CRIMSON_NOBLE | abilities/lust/* |
| Envy 시기 | ENVY | EnvyMirror | EnvyCovet (robbed kills + bestiary of 27 special mobs) | envy/EnvyHooks | USURPER, COPYCAT | abilities/envy/* |
| Sloth 나태 | SLOTH | SlothPillow | SlothRepose (kills by dream puppets / auto-attacks; own kills 1/3) | sloth/SlothHooks | DREAM_LORD, SLEEPWALKER | abilities/sloth/* |

Paths are under `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/`
(items in `items/`, buffs in `actors/buffs/`, abilities in `actors/hero/abilities/`).
Charge-based sins keep their item charges inside the buff. Each buff has a `mirrored` flag
set by the vault mirror relic (`items/MirrorRelics.java`).
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
- `items.png` (TX_HEIGHT in `ItemSpriteSheet` must equal the PNG height, now 576):
  row 33 signature item/class armor pairs, row 34 sin weapons + sigil, row 35 mirror relics,
  row 36 horsemen artifacts + seal fragment (+0..4), sin remains (+5..11)
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
- Sandboxes without Maven/Google access: `bash tools/compilecheck/check.sh` compiles against
  libGDX built from GitHub sources (prints `exit: 0` on success). Headless logic tests can be run
  with `java -cp <out>/spdcls:<deps.jar>`; anything touching Messages/UI needs the real game.
- After adding items: regenerate the dev-tools item list with
  `CLASSES=<spdcls> CP=<deps.jar> bash tools/devitems/regen.sh`
  (on a PC: `./gradlew :core:compileJava && bash tools/devitems/regen.sh`).

Debug builds (version suffix -INDEV) unlock all classes and show the dev tools; they install
alongside official SPD (`.indev` package suffix).
