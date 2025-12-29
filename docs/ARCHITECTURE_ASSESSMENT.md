# API-Sports KNIME Football Plugin - Architectural Assessment

**Date:** 2025-12-29
**Status:** Proposal for Review
**Purpose:** Comprehensive evaluation of node architecture before implementing remaining endpoints

---

## Executive Summary

With all API endpoints now documented (33 JSON response files across 14 endpoints), we need to assess our architectural approach to ensure:
- **Logical grouping** of related data
- **Intuitive user experience** for analysts
- **Efficient API usage** and performance
- **Scalable architecture** for future endpoints
- **Consistent patterns** across all nodes

### Key Recommendations

1. **Combine minor nodes into 3 composite "Context" nodes** (reduces 8 nodes to 3)
2. **Implement multi-selection lists** instead of single dropdowns
3. **Create specialized Odds architecture** for time-sensitive data
4. **Enhance selector pattern** with optional pre-populated lists
5. **Standardize input/output patterns** across all nodes

---

## Current State Analysis

### Implemented Nodes

| Node | Type | Input Options | Output | Status |
|------|------|---------------|---------|--------|
| Reference Data | Core | API Connection | Leagues, Seasons, Teams | ✅ Complete |
| Fixtures | Heavy | Date, League, Team, Season | 39 columns + optional data | ✅ Complete |
| Fixtures Selector | Lightweight | Date, League, Team, Season | 15 columns (IDs only) | ✅ Complete |
| Teams | Standard | League, Season | Team info + statistics | ✅ Complete |
| Players | Heavy | Various queries | 14 columns (season stats) | ✅ Complete |
| Players Selector | Lightweight | League, Team, Season | Basic player info | ✅ Complete |
| Player Stats | Specialized | Match-by-match | Player performance per match | ✅ Complete |

### Endpoints to Implement

| Endpoint | JSON Files | Logical Category | Current Plan |
|----------|-----------|------------------|--------------|
| Coaches | 1 | Team Context | Separate node |
| Venues | 1 | Team Context | Separate node |
| Standings | 1 | Team Context / Independent | Separate node |
| Injuries | 1 | Match Context | Separate node |
| Predictions | 1 | Match Context | Separate node |
| Transfers | 1 | Player Context | Separate node |
| Trophies | 1 | Player Context | Separate node |
| Sidelined | 1 | Player Context | Separate node |
| Odds Live | 2 | Real-time Data | Separate node |
| Odds Pre-match | 4 | Static Snapshot | Separate node |
| Timezone | 1 | Utility | Separate node |

**Total:** 11 new nodes (if we keep one-per-endpoint approach)

---

## Problem Statement

### Issue 1: Node Proliferation
If we create one node per endpoint, users will have **18+ nodes** in the palette:
- 7 existing nodes
- 11 new endpoint nodes
- Plus future additions

**Impact:** Overwhelming palette, difficult to discover relevant nodes, unclear relationships

### Issue 2: Fragmented Context
Related data is split across multiple nodes:
- **Team analysis** requires: Teams → Coaches → Venue → Standings (4 nodes)
- **Player profile** requires: Players → Transfers → Trophies → Sidelined (4 nodes)
- **Match preview** requires: Fixtures → Predictions → Injuries (3 nodes)

**Impact:** Complex workflows, many connections, harder to understand data relationships

### Issue 3: Single-Selection Limitation
Current dialogs use dropdowns for single-selection:
- Analyzing 5 teams requires running the node 5 times
- Doesn't leverage KNIME's batch processing capabilities
- Inefficient for comparative analysis

**Impact:** Poor UX, inefficient workflows, redundant API calls

### Issue 4: Odds Data Is Different
Odds endpoints refresh every few seconds/minutes:
- Live odds: Real-time streaming data
- Pre-match odds: Frequently updated snapshots
- Odds movement: Historical time-series data

**Impact:** Current node architecture doesn't fit time-sensitive data patterns

### Issue 5: Selector Input Not Visible
When selector output connects to detail node:
- Dialog settings are ignored (good)
- But user can't see what IDs are being used (bad)
- Can't modify selection without disconnecting (bad)

**Impact:** Black-box behavior, reduced control, harder to debug

---

## Proposed Architecture

### Core Principle: Data Context Grouping

Group endpoints by **analytical context** rather than API structure:

```
┌─────────────────────────────────────────────────────────┐
│  CORE LAYER: Data Discovery & Filtering                 │
├─────────────────────────────────────────────────────────┤
│  • Reference Data (leagues, seasons, teams)              │
│  • Fixtures Selector (find matches)                      │
│  • Players Selector (find players)                       │
│  • Teams Selector (NEW - find teams by criteria)         │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  DETAIL LAYER: Comprehensive Data Retrieval              │
├─────────────────────────────────────────────────────────┤
│  • Fixtures (match details + events/stats/lineups)       │
│  • Players (season statistics)                           │
│  • Teams (team info + statistics)                        │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  CONTEXT LAYER: Related Information (NEW)                │
├─────────────────────────────────────────────────────────┤
│  • Team Context (coaches + venue + standings)            │
│  • Player Profile (transfers + trophies + status)        │
│  • Match Predictions (predictions + team injuries)       │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  SPECIALIZED LAYER: Unique Use Cases                     │
├─────────────────────────────────────────────────────────┤
│  • Player Stats (match-by-match performance)             │
│  • Standings (league table - independent use)            │
│  • Venues (stadium info - independent use)               │
│  • Odds Snapshot (pre-match betting odds)                │
│  • Odds Movement (historical odds analysis)              │
└─────────────────────────────────────────────────────────┘
```

### Recommended Node Structure

#### 1. CORE LAYER (4 nodes)
**No changes** - these work well:
- Reference Data
- Fixtures Selector
- Players Selector
- **Teams Selector (NEW)** - lightweight team discovery

#### 2. DETAIL LAYER (3 nodes)
**No changes** - enhanced with individual query pattern:
- Fixtures
- Players
- Teams

#### 3. CONTEXT LAYER (3 new composite nodes)

**A. Team Context Node**
- **Combines:** Coaches + Venue + Current Standing
- **Input:** Team ID(s), League, Season
- **Optional Input:** From Teams Selector
- **Output Schema:**
  ```
  Team_ID, Team_Name, League_ID, League_Name, Season,
  Coach_Name, Coach_Type, Coach_Start_Date,
  Venue_Name, Venue_City, Venue_Capacity, Venue_Surface,
  Rank, Points, Played, Won, Drawn, Lost, Goals_For, Goals_Against
  ```
- **Use Case:** "Get complete context for Manchester United in 2024 season"

**B. Player Profile Node**
- **Combines:** Transfers + Trophies + Sidelined Status
- **Input:** Player ID(s)
- **Optional Input:** From Players Selector
- **Output Schema:**
  ```
  Player_ID, Player_Name,
  Current_Team, Transfer_Date, Transfer_Type, Transfer_From, Transfer_To,
  Trophy_League, Trophy_Season, Trophy_Place,
  Injury_Type, Injury_Start, Injury_End, Sidelined_Reason
  ```
- **Use Case:** "Get career history and current status for Erling Haaland"

**C. Match Predictions Node**
- **Combines:** Predictions + Team Injuries
- **Input:** Fixture ID(s)
- **Optional Input:** From Fixtures Selector
- **Output Schema:**
  ```
  Fixture_ID, Home_Team, Away_Team, Date,
  Prediction_Winner, Prediction_AdviceWin_Percent, Home_Percent, Away_Percent, Draw_Percent,
  Goals_Home_Prediction, Goals_Away_Prediction,
  Home_Team_Injuries (JSON), Away_Team_Injuries (JSON)
  ```
- **Use Case:** "Get pre-match analysis for this weekend's games"

#### 4. SPECIALIZED LAYER (5 nodes)

**Standalone nodes that don't fit composite pattern:**

**A. Player Stats** (existing)
- Match-by-match player performance
- Already implemented

**B. Standings**
- League table / rankings
- Useful independently for league analysis
- Could also be part of Team Context but commonly used alone

**C. Venues**
- Stadium information
- Useful independently for venue analysis
- Could also be part of Team Context but has independent use cases

**D. Odds Snapshot**
- Pre-match betting odds at query time
- **Input:** Fixture ID(s), League, Date Range
- **Output:** Bookmaker odds, markets, values
- **Refresh:** Manual re-execution
- **Use Case:** "What are current odds for Saturday's matches?"

**E. Odds Movement**
- Historical odds changes over time
- **Input:** Fixture ID, Start Date, End Date
- **Output:** Time-series of odds changes
- **Use Case:** "How did odds move in the 24 hours before kickoff?"

**Note:** Skip "Odds Live" streaming for now - requires different architecture (streaming nodes, continuous updates). Document as future enhancement.

**F. Timezone**
- Probably not needed as separate node
- Most KNIME users work in their local timezone
- API handles timezone conversions
- **Recommendation:** Skip or implement as utility function

---

## UI/UX Enhancements

### 1. Multi-Selection Lists

**Current:** Single-selection dropdowns
**Proposed:** Multi-selection lists with search

**Implementation:**
```java
// Replace single SettingsModelString with:
SettingsModelStringArray m_teamIds = new SettingsModelStringArray("teamIds", new String[]{});

// Dialog shows:
- JList with MULTIPLE_INTERVAL_SELECTION
- Search/filter box
- "Select All" / "Clear All" buttons
- Selected count: "5 of 20 teams selected"
```

**Apply to:**
- Team selection (analyze multiple teams at once)
- Player selection (compare multiple players)
- Fixture selection (get multiple matches)

**Keep single-selection for:**
- League (usually analyze one league at a time)
- Season (usually analyze one season at a time)

### 2. Selector Input Pre-population

**Current Behavior:**
```
Selector connected → Skip dialog → Use input IDs
```

**Enhanced Behavior:**
```
Selector connected → Show dialog with:
  ┌─────────────────────────────────────────┐
  │ ⚠ Using 23 Team IDs from input port     │
  │                                          │
  │ Teams: [Manchester United     ] [x]      │
  │        [Liverpool             ] [x]      │
  │        [Arsenal               ] [x]      │
  │        ... (20 more)                     │
  │                                          │
  │ [Modify Selection] [Use Input As-Is]    │
  └─────────────────────────────────────────┘
```

**Benefits:**
- User sees what data is being queried
- Can modify selection without disconnecting
- Better debugging and transparency

**Implementation:**
- Override `loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])`
- If `inSpecs[2] != null`, extract IDs and populate list
- Set flag indicating "from input port"
- Show warning message in dialog

### 3. Date Range Picker

For Fixtures-related nodes, replace "From Date" + "To Date" text fields with:
- Visual calendar picker
- Quick selections: "Next 7 days", "This weekend", "This month"
- Validation: End date must be after start date

### 4. Progressive Disclosure

For nodes with many options (like Fixtures with checkboxes for events/stats/lineups/players):

```
┌─────────────────────────────────────────┐
│ Basic Settings                           │
│   League: [Premier League     ▼]        │
│   Season: [2024               ▼]        │
│                                          │
│ ▶ Advanced Options (click to expand)    │
│                                          │
└─────────────────────────────────────────┘

After expansion:
┌─────────────────────────────────────────┐
│ Basic Settings                           │
│   League: [Premier League     ▼]        │
│   Season: [2024               ▼]        │
│                                          │
│ ▼ Advanced Options                       │
│   ☑ Include Events                       │
│   ☑ Include Statistics                   │
│   ☐ Include Lineups                      │
│   ☑ Include Player Stats                 │
│                                          │
└─────────────────────────────────────────┘
```

---

## Data Flow Examples

### Example 1: Team Performance Analysis

```
Reference Data
   ↓
Teams Selector (filter: "Premier League", "2024", "Top 6 teams")
   ↓ (6 Team IDs)
┌──────────┴──────────┐
│                     │
Teams Node      Team Context Node
│                     │
↓                     ↓
Team stats          Coach, Venue, Standing
(goals, wins, etc.) (context data)
```

**Result:** Complete team analysis with both performance and context

### Example 2: Player Scouting

```
Reference Data
   ↓
Players Selector (filter: "Forwards", "Age < 25", "Goals > 10")
   ↓ (15 Player IDs)
KNIME Row Filter (top 5 by goals/90)
   ↓ (5 Player IDs)
┌──────────┴──────────┐
│                     │
Players Node    Player Profile Node
│                     │
↓                     ↓
Season stats        Transfer history, Trophies, Injuries
```

**Result:** Detailed profiles of top young strikers

### Example 3: Match Preview

```
Reference Data
   ↓
Fixtures Selector (filter: "This Weekend", "Premier League")
   ↓ (10 Fixture IDs)
┌──────────┴──────────┬──────────────┐
│                     │              │
Fixtures Node   Match Predictions  Odds Snapshot
│                     │              │
↓                     ↓              ↓
H2H, Form         Predictions,    Current odds
                  Injuries
```

**Result:** Comprehensive pre-match analysis

---

## Implementation Impact Analysis

### Nodes to Create (Net: +6 nodes)

**New Composite Nodes:** 3
- Team Context
- Player Profile
- Match Predictions

**New Selector:** 1
- Teams Selector

**New Specialized:** 2
- Odds Snapshot
- Odds Movement

**Skip:** 2
- Odds Live (too complex, different architecture)
- Timezone (not needed)

### Nodes Avoided (Reduction: -6 nodes)
By using composite pattern, we avoid creating:
- Coaches (→ Team Context)
- Venues standalone (→ Team Context, plus independent Venues node)
- Injuries (→ Match Predictions)
- Transfers (→ Player Profile)
- Trophies (→ Player Profile)
- Sidelined (→ Player Profile)

### Final Node Count

| Category | Count | Nodes |
|----------|-------|-------|
| Core | 4 | Reference Data, Fixtures Selector, Players Selector, Teams Selector |
| Detail | 3 | Fixtures, Players, Teams |
| Context | 3 | Team Context, Player Profile, Match Predictions |
| Specialized | 5 | Player Stats, Standings, Venues, Odds Snapshot, Odds Movement |
| **TOTAL** | **15** | (vs 18 with one-per-endpoint approach) |

---

## Technical Implementation Notes

### Composite Node Pattern

**Team Context Example:**

```java
public class TeamContextNodeModel extends AbstractFootballQueryNodeModel {

    @Override
    protected BufferedDataTable executeQuery(...) throws Exception {
        // Make 3 API calls in parallel or sequence:
        JsonNode coaches = callApi(client, "/coachs", params, mapper);
        JsonNode venues = callApi(client, "/venues", params, mapper);
        JsonNode standings = callApi(client, "/standings", params, mapper);

        // Combine into single row per team
        return combineTeamContext(teams, coaches, venues, standings, exec);
    }

    private BufferedDataTable combineTeamContext(
        List<Integer> teamIds,
        JsonNode coaches,
        JsonNode venues,
        JsonNode standings,
        ExecutionContext exec) {

        // Create output spec with all columns
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        for (Integer teamId : teamIds) {
            // Find data for this team in each response
            JsonNode teamCoach = findByTeamId(coaches, teamId);
            JsonNode teamVenue = findByTeamId(venues, teamId);
            JsonNode teamStanding = findByTeamId(standings, teamId);

            // Combine into single row
            DataRow row = createContextRow(
                teamId, teamCoach, teamVenue, teamStanding, rowNum
            );
            container.addRowToTable(row);
            rowNum++;
        }

        container.close();
        return container.getTable();
    }
}
```

### Multi-Selection Dialog Example

```java
public class TeamSelectorNodeDialog extends NodeDialogPane {

    private JList<String> m_teamList;
    private DefaultListModel<String> m_teamListModel;
    private JTextField m_searchField;

    protected void loadSettingsFrom(...) {
        // Populate team list from reference data
        List<String> allTeams = loadTeamsFromReferenceData();
        m_teamListModel.clear();
        allTeams.forEach(m_teamListModel::addElement);

        // Restore previous selection
        String[] selectedTeams = m_settings.getStringArrayValue();
        for (String team : selectedTeams) {
            int index = m_teamListModel.indexOf(team);
            if (index >= 0) {
                m_teamList.addSelectionInterval(index, index);
            }
        }

        // If optional input connected, pre-select those teams
        if (inSpecs[2] != null) {
            List<Integer> inputTeamIds = extractFromInputSpec(inSpecs[2]);
            highlightInputTeams(inputTeamIds);
        }
    }
}
```

---

## Migration Strategy

### Phase 1: Enhance Existing (Low Risk)
1. ✅ Fixtures - add individual query error handling (DONE)
2. ✅ Players - add individual query error handling (DONE)
3. Add Teams Selector (follows Players Selector pattern)
4. Enhance dialogs with multi-selection (Teams, Players)

### Phase 2: Create Context Nodes (Medium Risk)
1. Team Context (combines 3 endpoints)
2. Player Profile (combines 3 endpoints)
3. Match Predictions (combines 2 endpoints)

### Phase 3: Specialized Nodes (Low Risk)
1. Standings (simple, single endpoint)
2. Venues (simple, single endpoint)
3. Odds Snapshot (new pattern, needs careful design)
4. Odds Movement (time-series pattern)

### Phase 4: UI Enhancements (Optional)
1. Multi-selection lists
2. Date range pickers
3. Selector input pre-population
4. Progressive disclosure

---

## Open Questions for Discussion

### 1. Composite vs Separate: Standings
**Question:** Should Standings be:
- **Option A:** Part of Team Context (current proposal)
- **Option B:** Separate node (also in proposal)
- **Option C:** Both (composite includes current standing, separate node shows full league table)

**My recommendation:** Option C - best of both worlds

### 2. Composite vs Separate: Venues
**Question:** Should Venues be:
- **Option A:** Part of Team Context only
- **Option B:** Separate node only (current proposal)
- **Option C:** Both

**My recommendation:** Option C - venues have independent use cases (stadium analysis)

### 3. Multi-Selection Default Behavior
**Question:** When multi-selection is enabled:
- **Option A:** Default to empty selection (user must choose)
- **Option B:** Default to "Select All" (user can deselect)
- **Option C:** Remember previous selection

**My recommendation:** Option C with smart defaults (top 10 teams for large lists)

### 4. Odds Node Scope
**Question:** Should we implement Odds nodes now or defer?
- **Option A:** Implement Snapshot + Movement (full scope)
- **Option B:** Implement Snapshot only (defer Movement)
- **Option C:** Defer all Odds (focus on core data first)

**My recommendation:** Option B - Snapshot is useful, Movement is nice-to-have

### 5. API Call Efficiency: Composite Nodes
**Question:** For composite nodes making multiple API calls:
- **Option A:** Sequential calls (simple, slower)
- **Option B:** Parallel calls (complex, faster)
- **Option C:** Configurable (let user choose)

**My recommendation:** Option A initially, Option B later if performance issues

---

## Success Criteria

This architecture will be successful if:

1. ✅ **Reduced Complexity:** Node count ≤ 15 (vs 18+ with one-per-endpoint)
2. ✅ **Logical Grouping:** Related data combined in intuitive ways
3. ✅ **Consistent Patterns:** All nodes follow same input/output patterns
4. ✅ **Efficient Workflows:** Multi-selection reduces need for loops
5. ✅ **Good Performance:** Individual query pattern with error handling works reliably
6. ✅ **Clear Documentation:** Users understand which node to use for each use case
7. ✅ **Extensible:** Easy to add new endpoints or composite combinations

---

## Next Steps

1. **Review & Decide:** Discuss this proposal and make decisions on open questions
2. **Update Requirements:** Revise requirement.md files based on decisions
3. **Prioritize:** Determine implementation order (suggest: Context nodes first)
4. **Prototype:** Build one composite node (Team Context) to validate pattern
5. **Iterate:** Refine based on prototype learnings
6. **Implement:** Build remaining nodes following established patterns
7. **Test:** Validate with real workflows and use cases
8. **Document:** Create user guide showing node selection decision tree

---

## Appendix: Node Selection Decision Tree

```
START: What do you want to analyze?

├─ Discover/Filter data
│  ├─ Find matches → Fixtures Selector
│  ├─ Find players → Players Selector
│  └─ Find teams → Teams Selector
│
├─ Get detailed data
│  ├─ Match details → Fixtures
│  ├─ Player stats → Players
│  └─ Team info → Teams
│
├─ Get contextual info
│  ├─ Team context (coach, venue, standing) → Team Context
│  ├─ Player background (transfers, trophies, injuries) → Player Profile
│  └─ Match preview (predictions, injuries) → Match Predictions
│
└─ Specialized analysis
   ├─ Player match performance → Player Stats
   ├─ League table → Standings
   ├─ Stadium info → Venues
   └─ Betting odds → Odds Snapshot / Odds Movement
```

---

**End of Assessment**
