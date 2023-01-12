Simple Quests 1.3.0
================
- Add quest categories
- Add triggers for breaking and interacting with a block
  See the wiki for all the syntaxes

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