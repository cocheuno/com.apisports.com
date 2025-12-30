# Use Case Analysis - Architectural Validation

**Date:** 2025-12-29
**Purpose:** Validate proposed architecture against 50 real-world use cases
**Result:** Architecture validated with critical refinements identified

---

## Executive Summary

### Validation Results: ✅ Architecture Confirmed with Enhancements

After analyzing all 50 use cases:

**✅ VALIDATED:**
- Composite Context nodes (17 use cases directly benefit)
- Multi-selection UI (12 use cases require batch operations)
- Selector pattern (15+ use cases follow discover→filter→details)
- Individual query approach (aligns with incremental loading patterns)

**🔧 ENHANCEMENTS NEEDED:**
- Date range picker (23 use cases need date filtering)
- Incremental loading support (8 use cases emphasize caching/smart updates)
- Additional query types for Fixtures and Players nodes
- Head-to-head fixture comparison

**❌ DEFER:**
- Live endpoints (only 2 use cases, complex architecture)
- Streaming odds (high quota cost, specialized use case)

---

## Use Case Categorization

### By Node Mapping

| Our Proposed Node | Use Cases Served | Count | Validation |
|-------------------|------------------|-------|------------|
| Reference Data | 1, 2, 3, 20 | 4 | ✅ Validated |
| Fixtures Selector | 5, 8, 9, 10, 24, 39 | 6 | ✅ Validated |
| Fixtures | 25, 30, 32, 42, 49 | 5 | ✅ Enhanced needed |
| Players Selector | 11, 12, 13, 43 | 4 | ✅ Validated |
| Players | 14, 15, 16, 22, 23, 31, 38, 41, 48 | 9 | ✅ Enhanced needed |
| Teams Selector | 6 (new capability) | 1 | ✅ Needed |
| Teams | 7, 29, 45 | 3 | ✅ Validated |
| **Team Context** | 6, 7 (partial) | 2 | ✅ Composite validated |
| **Player Profile** | 17, 18, 19, 23, 36 | 5 | ✅ Composite validated |
| **Match Predictions** | 27, 28, 33, 34, 37 | 5 | ✅ Composite validated |
| Standings | 4, 21, 44 (partial) | 3 | ✅ Standalone validated |
| Venues | (implicit in 6, 7) | - | ✅ Standalone validated |
| **Odds Snapshot** | 26, 28, 46 | 3 | ✅ New node validated |
| **Odds Movement** | (none explicit) | 0 | ⚠️ Defer to later |
| Player Stats | 35, 41 | 2 | ✅ Validated (existing) |
| Live/Streaming | 25, 40 | 2 | ❌ Defer (complex) |

### By Analytical Theme

| Theme | Use Cases | Pattern | Architecture Fit |
|-------|-----------|---------|------------------|
| **Dashboard Building** | 1, 2, 4, 5, 8, 20, 35 | Static/daily refresh | Reference + Selectors → Cache |
| **Scouting/Player Analysis** | 11-19, 22, 23, 43, 48 | Discover → Filter → Detail | Players Selector → Player Profile ✅ |
| **Team Performance** | 6, 7, 29, 32, 42, 45 | Historical aggregation | Teams + Fixtures Statistics |
| **Pre-Match Analysis** | 10, 26, 27, 28, 33, 34, 37 | Multiple sources combined | Match Predictions ✅ + Odds ✅ |
| **Betting/Odds** | 26, 27, 28, 40, 44, 46 | Real-time + value detection | Odds Snapshot + analysis |
| **Incremental Pipelines** | 21, 24, 36, 39, 49 | Smart caching, date-driven | **Need incremental mode** 🔧 |
| **Historical Analysis** | 22, 30, 45, 49 | Season loops, caching | Fixtures + loop patterns |
| **Live Tracking** | 25, 40 | Polling loops | **Defer live endpoints** ❌ |
| **Fantasy Football** | 11, 12, 13, 31, 41, 48 | Player stats + trends | Players + Player Stats |

---

## Critical Insights

### 1. Composite Nodes Are Strongly Validated ✅

**Player Profile Node** directly serves:
- UC #17: Transfers → ✅ transfers endpoint
- UC #18: Trophies → ✅ trophies endpoint
- UC #19: Current injuries → ✅ sidelined endpoint
- UC #23: Career path → ✅ players/teams endpoint
- UC #36: Transfer monitoring → ✅ transfers endpoint

**Match Predictions Node** directly serves:
- UC #27: Value bets (predictions + odds) → ✅
- UC #28: Poisson + odds → ✅ predictions provide probabilities
- UC #33: Derby analysis → ✅ predictions + context
- UC #34: Injury impact → ✅ injuries in predictions node
- UC #37: Prediction validation → ✅ predictions endpoint

**Conclusion:** Composite pattern is correct ✅

### 2. Date Range Handling Is Critical 🔧

**23 use cases** involve date filtering:
- UC #8: "Next 10 fixtures" → Need "Next X" shortcut
- UC #9: "Last 10 matches" → Need "Last X" shortcut
- UC #24: "Incremental daily" → Need date range picker
- UC #36: "Transfer window dates" → Need date range
- UC #39: "From last update" → Need incremental mode
- UC #49: "Full season range" → Need season → date range conversion

**Current:** Text fields for dates ❌
**Needed:**
```
┌─────────────────────────────────────────────┐
│ Date Selection                               │
│                                              │
│ ○ Date Range                                 │
│   From: [📅 2024-01-01] To: [📅 2024-12-31] │
│                                              │
│ ○ Relative                                   │
│   [Last ▼] [10 ▼] [matches ▼]              │
│   Options: Next/Last, 1-100, matches/days   │
│                                              │
│ ○ Current Season                             │
│   (automatically from Reference Data)        │
└─────────────────────────────────────────────┘
```

### 3. Incremental Loading Is Essential 🔧

**8 use cases** emphasize smart caching:
- UC #21: "Weekly full refresh" → Track last run
- UC #24: "Incremental daily loader" → Only fetch new dates
- UC #39: "Cache-aware updater" → Only fetch since last
- UC #49: "Smart incremental pipeline" → Append mode

**Implementation:**
```java
// Add to AbstractFootballQueryNodeModel
protected final SettingsModelBoolean m_incrementalMode =
    new SettingsModelBoolean("incrementalMode", false);
protected final SettingsModelString m_incrementalSinceVariable =
    new SettingsModelString("incrementalSinceVariable", "lastQueryDate");
```

**Dialog:**
```
┌─────────────────────────────────────────────┐
│ ☑ Incremental Mode                          │
│   Only fetch data newer than:               │
│   Flow Variable: [lastQueryDate    ▼]       │
│                                              │
│   Outputs flow variable: lastQueryDate      │
│   (use in next execution)                   │
└─────────────────────────────────────────────┘
```

### 4. Multi-Selection Validated by 12 Use Cases ✅

**Batch operations needed:**
- UC #21: "All top-5 leagues" → Multi-select 5 leagues
- UC #32: "Team form multiple teams" → Multi-select teams
- UC #35: "Team of the week" → Multiple queries
- UC #38: "Multi-league player comparison" → Multi-select leagues
- UC #43: "Scouting shortlist" → Filter then multi-select

**Conclusion:** Multi-selection is critical for efficiency ✅

### 5. Missing Endpoints Identified 🆕

**From use case analysis:**

| Endpoint | Use Case | Status in Docs | Action Needed |
|----------|----------|----------------|---------------|
| /countries | UC #2 | ❓ Check if available | Add to Reference Data if exists |
| /fixtures/headtohead | UC #10 | ✅ We have JSON | Add as Fixtures query type |
| /players/squads | UC #14 | ✅ We have JSON | Add as Players query type |
| /players/profiles | UC #16 | ✅ We have JSON | Add as Players query type |
| /players/seasons | UC #15 | ✅ We have JSON | Add as Players query type |
| /players/teams | UC #23 | ✅ We have JSON | Add to Player Profile composite |
| /odds/mapping | UC #27, #44 | ✅ We have JSON | Add as Odds query type |
| /odds/bookmakers | UC #44 | ✅ We have JSON | Add as Odds query type |

**Action:** Enhance Fixtures and Players nodes with additional query types

### 6. Live Endpoints Are Low Priority ❌

Only **2 use cases** (UC #25, #40) need live polling:
- UC #25: Live match tracker
- UC #40: Live odds polling

**Both require:**
- Polling loops (while loop + wait)
- Streaming architecture
- High quota consumption
- Complex state management

**Recommendation:** Defer to Phase 2 or document as external workflow pattern (KNIME loop + wait nodes)

### 7. Player Match-by-Match Stats Are Critical ✅

**UC #41** explicitly needs player match stats:
- Minutes trend last 10 matches
- Requires `/fixtures/players` endpoint
- Per-match granularity, not season aggregates

**Current:** Player Stats node (existing) ✅
**Validation:** This specialized node is essential, not redundant

### 8. Odds Architecture Needs Refinement 🔧

**Current proposal:** Odds Snapshot + Odds Movement

**Use case analysis shows:**
- UC #26: Pre-match odds comparison → Odds Snapshot ✅
- UC #27: Odds mapping/coverage → **Need Mapping query type** 🔧
- UC #28: Value bets → Predictions + Odds join ✅
- UC #40: Live odds → Defer ❌
- UC #44: Bookmaker coverage → **Need Bookmakers query type** 🔧
- UC #46: Arbitrage → Advanced analysis (user builds with KNIME nodes)

**Refined Odds Snapshot Node:**
```
Query Types:
1. Fixture Odds (UC #26, #28)
   - Input: Fixture ID(s), Bookmaker filter
   - Output: Odds per market per bookmaker

2. Odds Mapping (UC #27, #44)
   - Input: League, Date range
   - Output: Coverage map (which fixtures have odds)

3. Bookmakers List (UC #44)
   - Input: None (reference data)
   - Output: All available bookmakers
```

**Conclusion:** Single Odds Snapshot node with 3 query types, defer Odds Movement

---

## Architectural Refinements

### Validated: Keep As Proposed ✅

1. **Composite Context Nodes** - 17 use cases directly benefit
2. **Selector Pattern** - 15+ use cases follow this flow
3. **Individual Query Approach** - Aligns with incremental patterns
4. **Multi-Selection UI** - 12 use cases require it
5. **Defer Live Endpoints** - Only 2 use cases, complex

### Required Enhancements 🔧

#### 1. Date Range Picker (HIGH PRIORITY)
- Visual calendar selection
- Quick shortcuts: "Next X", "Last X", "This week", "This month"
- Season → date range auto-conversion
- **Affects:** Fixtures, Fixtures Selector, any time-based query

#### 2. Incremental Loading Mode (HIGH PRIORITY)
- Checkbox: "Incremental mode"
- Flow variable input: "Since date"
- Flow variable output: "Last query date"
- **Affects:** All nodes that query time-series data

#### 3. Enhanced Query Types (MEDIUM PRIORITY)

**Fixtures Node - Add:**
- Head-to-head query (UC #10)
- Explicit "Next X" and "Last X" options

**Players Node - Add:**
- Squad query (UC #14) - all players for a team
- Profiles query (UC #16) - basic player info with photo
- Seasons query (UC #15) - available seasons for player

**Odds Snapshot Node - Add:**
- Mapping query (UC #27, #44) - odds coverage
- Bookmakers query (UC #44) - available bookmakers

#### 4. Reference Data Enhancement (LOW PRIORITY)
- Add /countries endpoint if available (UC #2)
- Output countries with flags for UI/dashboard use

---

## Revised Node Structure

### CORE LAYER (4 nodes)
1. **Reference Data**
   - Current: Leagues, Seasons, Teams
   - Add: Countries (if endpoint exists)

2. **Fixtures Selector** (existing ✅)

3. **Players Selector** (existing ✅)

4. **Teams Selector** (new, validated by UC #6)

### DETAIL LAYER (3 nodes)

1. **Fixtures**
   - Current query types ✅
   - **Add:** Head-to-head (UC #10)
   - **Add:** Next/Last shortcuts (UC #8, #9)
   - **Enhance:** Date range picker
   - **Enhance:** Incremental mode

2. **Players**
   - Current query types ✅ (top scorers, assists, cards, by team, by name, by ID)
   - **Add:** Squad (UC #14)
   - **Add:** Profiles (UC #16)
   - **Add:** Seasons (UC #15)
   - **Enhance:** Multi-selection

3. **Teams** (no changes needed ✅)

### CONTEXT LAYER (3 composite nodes)

1. **Team Context** ✅
   - Combines: Coaches + Venue + Current Standing
   - Validated by: UC #6, #7 (partial)

2. **Player Profile** ✅
   - Combines: Transfers + Trophies + Sidelined + Teams (career)
   - Validated by: UC #17, #18, #19, #23, #36

3. **Match Predictions** ✅
   - Combines: Predictions + Team Injuries
   - Validated by: UC #27, #28, #33, #34, #37

### SPECIALIZED LAYER (5 nodes)

1. **Player Stats** (existing ✅)
   - Validated by: UC #41 (minutes trend)

2. **Standings** ✅
   - Validated by: UC #4, #21

3. **Venues** ✅
   - Standalone for stadium analysis

4. **Odds Snapshot** (new, refined)
   - Query types: Fixture Odds, Mapping, Bookmakers
   - Validated by: UC #26, #27, #28, #44, #46

5. **Skip Odds Movement** ❌
   - No use cases require historical odds time-series
   - Can be future enhancement if needed

**Total: 15 nodes** (was 15, refined scope)

---

## UI Specification Based on Use Cases

### 1. Date Range Component (23 use cases need this)

```
┌───────────────────────────────────────────────────────┐
│ Time Period Selection                                 │
├───────────────────────────────────────────────────────┤
│                                                        │
│ ◉ Date Range                                          │
│   From: [📅 2024-01-01 ▼]  To: [📅 2024-12-31 ▼]    │
│   Quick: [Today] [Tomorrow] [This Week] [This Month] │
│                                                        │
│ ○ Relative to Now                                     │
│   [Next ▼] [10 ▼] [matches ▼]                        │
│   Options: Next/Last | 1-100 | matches/days/weeks    │
│                                                        │
│ ○ Current Season (from Reference Data)                │
│   Automatically uses season start/end dates           │
│                                                        │
│ ☑ Incremental Mode                                    │
│   Fetch only data newer than: ${lastQueryDate}        │
│   (outputs updated lastQueryDate flow variable)       │
└───────────────────────────────────────────────────────┘
```

**Applies to:** Fixtures, Fixtures Selector, any time-based queries

### 2. Multi-Selection List (12 use cases need this)

```
┌───────────────────────────────────────────────────────┐
│ Team Selection                                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│ Search: [manchester________________] 🔍               │
│                                                        │
│ Available Teams:              Selected Teams:         │
│ ┌─────────────────────┐      ┌──────────────────┐   │
│ │ ☐ Arsenal           │      │ ☑ Man United     │   │
│ │ ☐ Aston Villa       │      │ ☑ Man City       │   │
│ │ ☐ Chelsea           │      │ ☑ Liverpool      │   │
│ │ ☐ ...               │      │                   │   │
│ └─────────────────────┘      └──────────────────┘   │
│                                                        │
│ [Select All] [Clear All]  Selected: 3 teams          │
│                                                        │
│ ⚠ Optional input connected: Using 3 Team IDs from    │
│   Selector node. Click [Modify Selection] to change. │
└───────────────────────────────────────────────────────┘
```

**Applies to:** Teams, Players, Leagues (in various nodes)

### 3. Incremental Mode Toggle

```
┌───────────────────────────────────────────────────────┐
│ Advanced Options                                      │
├───────────────────────────────────────────────────────┤
│                                                        │
│ ☑ Incremental Loading                                 │
│   Only fetch data newer than flow variable:           │
│   ${lastQueryDate} = "2024-12-01"                     │
│                                                        │
│   On execution, this node will:                       │
│   • Add filter: date >= $lastQueryDate                │
│   • Output new lastQueryDate = current timestamp      │
│   • Append new data to existing tables                │
│                                                        │
│   Use Case: Daily/weekly refresh workflows            │
└───────────────────────────────────────────────────────┘
```

**Applies to:** Fixtures, Players, Teams (any node with date/time data)

---

## Workflow Pattern Examples

### Pattern 1: Scouting Workflow (UC #43)
```
Reference Data
   ↓
Players Selector
   Query: League=Premier League, Season=2024, Age<23
   ↓ (50 young players)
KNIME Row Filter
   Filter: Goals + Assists > 10
   ↓ (15 players)
KNIME Top K Selector
   Top 10 by Goals per 90
   ↓ (10 player IDs)
┌──────────┴───────────┐
│                      │
Players            Player Profile
(season stats)     (career history)
   ↓                   ↓
   └─────── Join ──────┘
            ↓
      Final shortlist
```

**Architecture validation:** ✅ Selector → Filter → Composite works perfectly

### Pattern 2: Pre-Match Analysis (UC #27, #28, #33)
```
Reference Data
   ↓
Fixtures Selector
   Date: Next 7 days, League: Premier League
   ↓ (10 upcoming fixtures)
KNIME Row Filter
   Filter: Top 6 teams only
   ↓ (3 derby matches)
┌─────────┴─────────┬─────────────┐
│                   │             │
Fixtures      Match Predictions  Odds Snapshot
(H2H + form)  (win %, injuries)  (bookmaker odds)
   ↓                │             ↓
   └────── Join all on Fixture_ID ─┘
                   ↓
         Complete match preview
         (form + predictions + odds)
```

**Architecture validation:** ✅ Three specialized nodes combine perfectly

### Pattern 3: Incremental Daily Dashboard (UC #24, #39)
```
[First Run]
Variable: lastQueryDate = "2024-01-01"
   ↓
Fixtures (Incremental Mode = ON)
   Date >= $lastQueryDate
   ↓ (all fixtures since Jan 1)
Table Writer (cache.table)
Variable Output: lastQueryDate = "2024-12-29"

[Second Run - Next Day]
Variable: lastQueryDate = "2024-12-29" (from previous)
   ↓
Fixtures (Incremental Mode = ON)
   Date >= $lastQueryDate
   ↓ (only new fixtures since Dec 29)
Table Reader (cache.table)
   ↓
Concatenate (old + new)
   ↓
Table Writer (cache.table - updated)
Variable Output: lastQueryDate = "2024-12-30"
```

**Architecture validation:** ✅ Incremental mode enables this pattern

---

## Quota Impact Analysis

### High Quota Use Cases (>100 calls)
- UC #49: Full season pipeline (could be 1000+ calls)
- UC #50: Player deep-dive dashboard (100+ calls per player)
- UC #30: Elo rating calculator (500+ fixtures)

**Mitigation:**
- Incremental mode reduces repeat calls ✅
- Individual query pattern allows precise control ✅
- Caching strategies documented ✅

### Medium Quota Use Cases (10-100 calls)
- UC #21: Weekly standings (5-10 leagues)
- UC #24: Daily fixture updates (10-50 new matches)
- UC #38: Multi-league comparison (5 leagues × queries)

**Mitigation:**
- Multi-selection reduces need for loops ✅
- Selector pattern pre-filters before expensive calls ✅

### Low Quota Use Cases (<10 calls)
- UC #1-13: Reference data, top scorers, standings
- UC #26: Single match odds
- UC #35: Team of the week (aggregated)

**Architecture:** Efficient for these ✅

---

## Comparison: Proposed vs Alternative Architectures

### Alternative 1: "One Node Per Endpoint" (Original Plan)

**Pros:**
- Simple mapping API → Node
- Clear boundaries

**Cons:**
- 18+ nodes (overwhelming)
- Related data fragmented (UC #27 needs 3+ nodes)
- Complex workflows for common tasks

**Use case fit:** ❌ UC #27, #28, #33, #34 all require multiple nodes

### Alternative 2: "Single Mega-Node"

**Pros:**
- One node does everything
- Simple palette

**Cons:**
- Complex dialog (dozens of options)
- Hard to understand what it does
- Poor separation of concerns

**Use case fit:** ❌ Too complex, violates KNIME philosophy

### Alternative 3: "Composite Context Nodes" (Proposed)

**Pros:**
- Logical grouping (UC #17-19 in one node)
- 15 focused nodes (manageable)
- Clear purpose per node
- Composable workflows (UC #27, #43 examples)

**Cons:**
- More API calls per composite node
- Slightly more complex implementation

**Use case fit:** ✅ 17 use cases directly benefit, efficient workflows

**Winner:** Alternative 3 (Proposed Architecture) ✅

---

## Implementation Priority Based on Use Cases

### Phase 1: High-Impact Foundation (Serves 30+ use cases)

1. **Date Range Picker Enhancement**
   - Affects: 23 use cases
   - Complexity: Medium
   - Impact: HIGH
   - Timeline: 2-3 days

2. **Incremental Mode**
   - Affects: 8 use cases directly, all time-series use cases benefit
   - Complexity: Medium
   - Impact: HIGH (quota efficiency)
   - Timeline: 2-3 days

3. **Teams Selector**
   - Affects: UC #6, #32, #35, #38
   - Complexity: Low (copy Players Selector pattern)
   - Impact: Medium
   - Timeline: 1 day

4. **Multi-Selection (Teams, Players)**
   - Affects: 12 use cases
   - Complexity: Medium
   - Impact: HIGH
   - Timeline: 3-4 days

### Phase 2: Context Nodes (Serves 17 use cases)

1. **Player Profile** (UC #17, #18, #19, #23, #36)
   - Complexity: Medium
   - Impact: HIGH
   - Timeline: 3-4 days

2. **Match Predictions** (UC #27, #28, #33, #34, #37)
   - Complexity: Medium
   - Impact: HIGH
   - Timeline: 3-4 days

3. **Team Context** (UC #6, #7 partial)
   - Complexity: Medium
   - Impact: Medium
   - Timeline: 3-4 days

### Phase 3: Query Type Enhancements (Serves 12 use cases)

1. **Fixtures Head-to-Head** (UC #10)
   - Complexity: Low
   - Impact: Medium
   - Timeline: 1 day

2. **Players Squad/Profiles/Seasons** (UC #14, #15, #16)
   - Complexity: Low (add query types)
   - Impact: Medium
   - Timeline: 2 days

3. **Odds Snapshot** (UC #26, #27, #28, #44)
   - Complexity: Medium-High (new node)
   - Impact: Medium (betting use cases)
   - Timeline: 4-5 days

### Phase 4: Optional Enhancements

1. **Standings** (UC #4, #21)
   - Complexity: Low
   - Impact: Medium
   - Timeline: 2 days

2. **Venues** (UC #6 partial)
   - Complexity: Low
   - Impact: Low
   - Timeline: 1-2 days

3. **Reference Data Countries** (UC #2)
   - Complexity: Low (if endpoint exists)
   - Impact: Low
   - Timeline: 1 day

**Total Timeline Estimate:** 6-8 weeks for all phases

---

## Recommendations

### 1. Approve Refined Architecture ✅

**Composite Context Nodes:**
- Player Profile (transfers + trophies + sidelined + teams)
- Match Predictions (predictions + injuries)
- Team Context (coaches + venue + standings)

**Rationale:** 17 use cases directly benefit, reduces complexity

### 2. Prioritize UI Enhancements 🔧

**Must Have:**
1. Date range picker (23 use cases)
2. Incremental mode (8 use cases, huge quota savings)
3. Multi-selection (12 use cases)

**Nice to Have:**
- Pre-population from selectors
- Progressive disclosure for advanced options

### 3. Enhanced Query Types 🔧

**Fixtures:**
- Add: Head-to-head (UC #10)
- Add: Next/Last shortcuts (UC #8, #9)

**Players:**
- Add: Squad (UC #14)
- Add: Profiles (UC #16)
- Add: Seasons (UC #15)

### 4. Defer Live Endpoints ❌

**Rationale:**
- Only 2 use cases (UC #25, #40)
- Complex architecture (polling, streaming)
- High quota cost
- Can be achieved with KNIME loop + wait nodes (document pattern)

### 5. Single Odds Node with 3 Query Types 🔧

**Query Types:**
1. Fixture Odds (UC #26, #28)
2. Mapping (UC #27, #44)
3. Bookmakers (UC #44)

**Defer:** Odds Movement (no use cases)

---

## Success Metrics

After implementation, success = all 50 use cases achievable with:
- ✅ ≤ 15 nodes (vs 18+ with one-per-endpoint)
- ✅ ≤ 5 node connections for most workflows
- ✅ 50%+ reduction in API calls (via incremental mode + multi-selection)
- ✅ Clear workflow patterns documented
- ✅ Intuitive node selection (users know which node to use)

---

## Next Steps

1. **Review & Approve** refined architecture
2. **Decide Priority:** Phase 1 (foundation) vs Phase 2 (context nodes) first?
3. **Prototype:** Build date range picker + incremental mode (affects all nodes)
4. **Implement:** Follow phased timeline
5. **Validate:** Test against use cases 1-50
6. **Document:** Workflow patterns for common use cases

---

**End of Analysis**
