Simple Quests 1.5.1
================
- Change current quest command to display a gui instead of outputting to chat
- Fix daily quest amount tracker not tracking
- Add `visibility` field to quests. Determines whether the quest shows up in the gui or not:
  `DEFAULT`: default behaviour. Shows only acceptable quests
  `ALWAYS`: always shows the quest even if player can't accept it
  `NEVER`: never show the quest

Simple Quests 1.5.0
================
- Impl Sequential quest type: 
  Allows chaining of multiple quests that need to be fullfilled
- A lot of internal changes for more dynamic handling
- `is_silent` field for QuestCategory:  
  If true Quests in that category will not have any chat outputs.
- MultiEntries now randomize the entries based on fixed (daily changing) random seed
- If mod is on client now the client will handle translation
- Reduce conversion amount of older datapack versions:  
  Only position entries need update now

Simple Quests 1.4.0
================
- Add quest complete event for fabric and forge
- Moved some things to api
- Add dailyQuestAmount: If >= 0 will select up to x amount of daily quests instead of all
- Internal: All QuestEntry use Codecs now
- Added Multi-Entry types:   
  Those types allow a entry to provide multiple possible tasks.
  E.g. an item entry where you have to submit either item a or b x amount.
- Added composite quests:
  Composite quests are quests that refer to other quests. 
  You can only select one of them at the same time
- Add fishing quest entry
- Quest guis current page gets saved when going back
- Renamed /simplequests show to /simplequests quests
- is_visible for QuestCategory:
  If true quests in this category are not shown with the current quest command

Simple Quests 1.3.2
================
- Rewrite quest trackers. Now the current progress will also be displayed for e.g. kill quests
- Fix various lang stuff
- Fix daily quests counting towards concurrent quest limit
- Add support for longer quest description:
  `description` in the json file. See `item_example.json` in the Example Datapack

Simple Quests 1.3.1
================
- Update to 1.19.4
<i>
  - Add some more context to some stuff (API)
  - Add crafting task. Triggered when the player crafts an item
  </i>

Simple Quests 1.3.0
================
- Add quest categories
- Add triggers for breaking and interacting with a block
  See the wiki for all the syntaxes
- Fix mixin metadata missing on forge thus causing the mod to not work properly.
  How did no one report on this?
- Add submission triggers. default to "". If a quest has this defined it can only be completed via
  command **/simplequest submit \<type\>** which requires op
- Add an player sensitive unlock condition
- Made it so errors during loading of datapack or players gets logged but does not prevent loading the whole thing completely
- Add option to execute a command upon completing a quest

Simple Quests 1.2.0
================
- Add support for week, days etc. for quest cooldown.  
  You dont need to just use ticks anymore.
  Refer to the wiki for the format of it.
- Fix not all quests showing up in gui
- Added more quest triggers. Wiki will have the formats for them:  
  **Position Entry**: Checks if a player is within a certain distance of a given position  
  **Location Entry**: Extended version of position entry. It uses vanilla location predicate which enables you to
  check for e.g. structures, light level, dimension etc.
  **Entity Interaction Entry**: For interacting (right click) an entity (with an item).
- Some stuff for existing entries got renamed. Your current datapacks will most likely not work anymore.  
  **Ingredient Entry**: 
  - id got renamed from "simplequest:ingredient" -> "simplequest:item"
  - "ingredient" -> "predicate" but it now uses vanilla item predicates instead of ingredient. See the wiki for the correct format
  - added field "consumeItems" defaulting to true and if false will not consume the items upon submitting
  
  **Kill Entry**:
  - "entity" -> "predicate" and uses now vanilla entity predicate  
  
  **Advancement Entry**
  - added field "reset" defaulting to false and if true will revoke the advancement upon completion
- Quests can now have multiple parent quests
- Quests can now set to locked. Locked quests can't be accepted till they are unlocked.
  Use ./simplequest unlock <player> <id> to unlock it. Needs op permissions.
- Fix completing child quests resetting parent quest cooldown
- Add daily quests: set "daily_quest" in the quest json to true.
  Daily quests will auto accepted and lasts for the day. Failing to complete them simply resets them
- Fix command permissions

Simple Quests 1.0.2
================
- Add option to specify quest icon

Simple Quests 1.0.1
================
- Fix kill quests giving reward but not actually finishing the quest
- Only notify player once on kill quest completion

Simple Quests 1.0.0
================
- Initial Release
- Update to 1.19