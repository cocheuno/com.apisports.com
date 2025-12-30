# Phase 1 Implementation Plan - Foundation UI Components

**Status:** In Progress
**Start Date:** 2025-12-30
**Target Completion:** 4 weeks
**Approved By:** User Decision (Architecture validated by 50 use cases)

---

## Objectives

Implement foundational UI enhancements that serve 30+ use cases:
1. **Date Range Picker** - Serves 23 use cases
2. **Multi-Selection Lists** - Serves 12 use cases
3. **Incremental Loading** - Serves 8 use cases + quota efficiency for all
4. **Live Endpoint Support** - Serves 2 use cases (UC #25, #40)

---

## Architecture Decisions

### ✅ Approved Architecture
- 15-node structure with 3 composite context nodes
- Selector pattern for heavy nodes
- Individual query pattern with error handling
- **Hybrid live endpoint approach (Option C)**

### 🔴 Live Endpoints Strategy (Option C)
**Approach:** Nodes support live endpoints as query options + documented polling patterns

**Implementation:**
1. Fixtures node adds `/fixtures/live` as endpoint option
2. Odds node adds `/odds/live` as endpoint option
3. Document KNIME loop + wait polling pattern in `/docs/LIVE_POLLING_PATTERNS.md`
4. Users build polling workflows with standard KNIME nodes

**Benefits:**
- Simple implementation (no complex polling logic in nodes)
- Flexible (users control polling frequency, conditions)
- Low maintenance (leverage KNIME's existing loop/wait capabilities)
- Can add built-in polling later if demand increases

---

## Phase 1 Components

### Component 1: DateRangePanel

**File:** `src/main/java/com/apisports/knime/football/ui/DateRangePanel.java`

**Features:**
- Radio button modes: Date Range / Relative / Current Season / Incremental
- Date Range mode: From/To date pickers with quick shortcuts
- Relative mode: Next/Last X matches/days/weeks
- Current Season mode: Auto-populate from Reference Data flow variable
- Incremental mode: Flow variable selector for "since last run"

**UI Mockup:**
```
┌─────────────────────────────────────────────────────┐
│ Date Selection                                      │
│                                                      │
│ ◉ Date Range                                        │
│   From: [📅 2024-01-01 ▼]  To: [📅 2024-12-31 ▼]  │
│   Quick: [Today] [Tomorrow] [This Week] [Next 7]   │
│                                                      │
│ ○ Relative                                          │
│   [Next ▼] [10 ▼] [matches ▼]                      │
│                                                      │
│ ○ Current Season                                    │
│   Uses season dates from Reference Data             │
│                                                      │
│ ○ Incremental                                       │
│   Since: ${lastQueryDate} = "2024-12-01"            │
│   Outputs: lastQueryDate (current timestamp)        │
└─────────────────────────────────────────────────────┘
```

**Usage:**
```java
public class FixturesNodeDialog extends NodeDialogPane {
    private DateRangePanel m_dateRangePanel;

    public FixturesNodeDialog() {
        m_dateRangePanel = new DateRangePanel(
            "lastQueryDate",  // flow variable name
            true              // allow incremental mode
        );
        addTab("Date Selection", m_dateRangePanel);
    }
}
```

**Settings Models:**
```java
// In NodeModel
protected final SettingsModelString m_dateMode =
    new SettingsModelString("dateMode", "range"); // range/relative/season/incremental

protected final SettingsModelString m_fromDate =
    new SettingsModelString("fromDate", "");

protected final SettingsModelString m_toDate =
    new SettingsModelString("toDate", "");

protected final SettingsModelString m_relativeDirection =
    new SettingsModelString("relativeDirection", "next"); // next/last

protected final SettingsModelInteger m_relativeCount =
    new SettingsModelInteger("relativeCount", 10);

protected final SettingsModelString m_relativeUnit =
    new SettingsModelString("relativeUnit", "matches"); // matches/days/weeks

protected final SettingsModelString m_incrementalVariable =
    new SettingsModelString("incrementalVariable", "lastQueryDate");
```

---

### Component 2: MultiSelectionPanel

**File:** `src/main/java/com/apisports/knime/football/ui/MultiSelectionPanel.java`

**Features:**
- JList with MULTIPLE_INTERVAL_SELECTION mode
- Search/filter text box
- Select All / Clear All buttons
- Selection count display
- Support for pre-population from selector input port

**UI Mockup:**
```
┌───────────────────────────────────────────────────┐
│ Team Selection                                    │
│                                                    │
│ Search: [manchester____________] 🔍               │
│                                                    │
│ ┌─────────────────────────────────────────────┐  │
│ │ ☑ Manchester United                          │  │
│ │ ☑ Manchester City                            │  │
│ │ ☐ Arsenal                                    │  │
│ │ ☐ Liverpool                                  │  │
│ │ ☐ Chelsea                                    │  │
│ │ ...                                          │  │
│ └─────────────────────────────────────────────┘  │
│                                                    │
│ [Select All] [Clear All]     Selected: 2 teams   │
│                                                    │
│ ⚠ Input port connected: 2 Team IDs pre-selected  │
│   [Modify Selection] [Use Input As-Is]           │
└───────────────────────────────────────────────────┘
```

**Usage:**
```java
public class TeamsNodeDialog extends NodeDialogPane {
    private MultiSelectionPanel<String> m_teamSelectionPanel;

    public TeamsNodeDialog() {
        m_teamSelectionPanel = new MultiSelectionPanel<>(
            "Select Teams",
            this::loadAvailableTeams  // callback to load items
        );
        addTab("Teams", m_teamSelectionPanel);
    }

    private List<String> loadAvailableTeams() {
        // Load from Reference Data port or database
        return Arrays.asList("Manchester United", "Arsenal", ...);
    }
}
```

**Settings Models:**
```java
// In NodeModel
protected final SettingsModelStringArray m_selectedTeamIds =
    new SettingsModelStringArray("selectedTeamIds", new String[]{});
```

---

### Component 3: IncrementalModePanel

**File:** `src/main/java/com/apisports/knime/football/ui/IncrementalModePanel.java`

**Features:**
- Checkbox to enable/disable incremental mode
- Flow variable selector dropdown
- Information text explaining behavior
- Integration with DateRangePanel

**UI Mockup:**
```
┌───────────────────────────────────────────────────┐
│ Incremental Loading                                │
│                                                    │
│ ☑ Enable Incremental Mode                         │
│                                                    │
│   Only fetch data newer than flow variable:       │
│   Flow Variable: [lastQueryDate          ▼]       │
│   Current Value:  2024-12-01 14:30:00              │
│                                                    │
│   ℹ On execution:                                 │
│   • Adds filter: date >= $lastQueryDate           │
│   • Outputs lastQueryDate = current timestamp     │
│   • Use with Table Reader/Writer for caching      │
│                                                    │
│   Use Case: Daily/weekly refresh workflows        │
└───────────────────────────────────────────────────┘
```

**Usage:**
```java
public class FixturesNodeDialog extends NodeDialogPane {
    private IncrementalModePanel m_incrementalPanel;

    public FixturesNodeDialog() {
        m_incrementalPanel = new IncrementalModePanel("lastQueryDate");
        addTab("Advanced", m_incrementalPanel);
    }
}
```

**Settings Models:**
```java
// In NodeModel
protected final SettingsModelBoolean m_incrementalMode =
    new SettingsModelBoolean("incrementalMode", false);

protected final SettingsModelString m_incrementalVariable =
    new SettingsModelString("incrementalVariable", "lastQueryDate");
```

**NodeModel Integration:**
```java
@Override
protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) {
    // If incremental mode enabled
    if (m_incrementalMode.getBooleanValue()) {
        // Get "since" date from flow variable
        String sinceDate = getAvailableFlowVariables().get(
            m_incrementalVariable.getStringValue()
        ).getStringValue();

        // Add to query params
        params.put("from", sinceDate);

        // After query, output new timestamp
        pushFlowVariableString(
            m_incrementalVariable.getStringValue(),
            LocalDateTime.now().toString()
        );
    }
}
```

---

## Node Enhancements

### 1. Fixtures Node

**Enhancements:**
- ✅ Already has individual query pattern with error handling
- 🔧 Add DateRangePanel (replaces current date fields)
- 🔧 Add IncrementalModePanel
- 🔧 Add Head-to-Head query type
- 🔧 Add `/fixtures/live` endpoint option

**New Query Types:**
```java
static final String QUERY_BY_DATE_RANGE = "By Date Range";
static final String QUERY_BY_TEAM = "By Team";
static final String QUERY_HEAD_TO_HEAD = "Head-to-Head";
static final String QUERY_NEXT_MATCHES = "Next X Matches";
static final String QUERY_LAST_MATCHES = "Last X Matches";
static final String QUERY_LIVE = "Live Matches"; // NEW for UC #25
```

**Live Endpoint Support:**
```java
private String getEndpoint(String queryType) {
    if (QUERY_LIVE.equals(queryType)) {
        return "/fixtures/live";
    }
    return "/fixtures";
}
```

### 2. Fixtures Selector Node

**Enhancements:**
- 🔧 Add DateRangePanel
- No other changes (already lightweight)

### 3. Players Node

**Enhancements:**
- ✅ Already has individual query pattern with error handling
- 🔧 Add MultiSelectionPanel for team/player selection
- 🔧 Add new query types: Squad, Profiles, Seasons
- 🔧 Add IncrementalModePanel (for historical stats queries)

**New Query Types:**
```java
static final String QUERY_SQUAD = "Team Squad";        // UC #14
static final String QUERY_PROFILES = "Player Profiles"; // UC #16
static final String QUERY_SEASONS = "Available Seasons"; // UC #15
```

**Implementation:**
```java
private String getEndpoint(String queryType) {
    switch (queryType) {
        case QUERY_TOP_SCORERS: return "/players/topscorers";
        case QUERY_TOP_ASSISTS: return "/players/topassists";
        case QUERY_SQUAD: return "/players/squads";       // NEW
        case QUERY_PROFILES: return "/players/profiles";  // NEW
        case QUERY_SEASONS: return "/players/seasons";    // NEW
        default: return "/players";
    }
}
```

### 4. Teams Node

**Enhancements:**
- 🔧 Add MultiSelectionPanel for team selection
- Keep existing functionality

### 5. Teams Selector Node (NEW)

**Purpose:** Lightweight team discovery for filtering before Teams node

**Pattern:** Copy Players Selector structure

**Output Schema:**
```
Team_ID (int)
Team_Name (string)
Team_Code (string)
Country (string)
Founded (int)
Logo (string - URL)
```

**Query Options:**
- By League + Season
- By Country
- By Name search

---

## New Nodes

### Odds Snapshot Node (NEW)

**File:** `OddsNodeModel.java`, `OddsNodeDialog.java`, `OddsNodeFactory.java`

**Query Types:**
1. **Fixture Odds** (UC #26, #28)
   - Input: Fixture ID(s) or Date Range + League
   - Output: Odds per market per bookmaker

2. **Odds Mapping** (UC #27, #44)
   - Input: League, Date Range
   - Output: Coverage map (which fixtures have odds available)

3. **Bookmakers** (UC #44)
   - Input: None (reference data)
   - Output: All available bookmakers

4. **Live Odds** (UC #40) - NEW
   - Input: Fixture ID(s)
   - Output: Current live odds
   - Note: For polling workflows (documented separately)

**Output Schema:**
```
Fixture_ID (int)
Bookmaker_ID (int)
Bookmaker_Name (string)
Bet_ID (int)
Bet_Name (string)
Value_ID (int)
Value_Name (string)
Odd (double)
Update_Timestamp (string)
```

**Dialog:**
- Query type dropdown
- Date range picker (for mapping query)
- Fixture ID input (optional, from selector)
- Bookmaker filter (multi-select)

---

## Documentation

### Live Polling Patterns Document

**File:** `/docs/LIVE_POLLING_PATTERNS.md`

**Content:**

```markdown
# Live Polling Patterns for API-Sports Football Nodes

## Overview

For real-time data (live matches, live odds), use KNIME's loop and wait nodes
to create polling workflows.

## Pattern 1: Live Match Tracker (UC #25)

### Workflow Structure:
```
Variable Loop Start
   condition: ${matchFinished} = false
   ↓
Wait Node (120 seconds = 2 minutes)
   ↓
Fixtures Node
   Query Type: Live Matches
   ↓
Extract Events (Row Filter on new events)
   ↓
Check if Match Finished
   condition: Status = "FT"
   set ${matchFinished} = true
   ↓
Variable Loop End
```

### Configuration:

**Fixtures Node:**
- Query Type: "Live Matches"
- No additional filters (gets all live matches)

**Wait Node:**
- Wait duration: 120000 ms (2 minutes)
- Reason: API refreshes live data every ~2 minutes

**Variable Loop:**
- Initialize: matchFinished = false
- Loop condition: matchFinished == false
- Update condition in "Check if Match Finished" node

### Quota Impact:
- ~45 calls per match (90 min / 2 min polling)
- Use sparingly during peak times

## Pattern 2: Live Odds Polling for Value Spikes (UC #40)

### Workflow Structure:
```
Table Creator
   Fixture ID, Baseline Odds
   ↓
Variable Loop Start
   condition: ${matchStarted} = false
   ↓
Wait Node (60 seconds)
   ↓
Odds Snapshot Node
   Query Type: Live Odds
   Fixture ID: ${fixtureId}
   ↓
Joiner (with baseline odds)
   ↓
Math Formula
   Calculate: (CurrentOdds - BaselineOdds) / BaselineOdds
   ↓
Row Filter
   Filter: Change > 10% (value spike detected)
   ↓
Send Email / Alert
   ↓
Check if Match Started
   condition: Time >= KickoffTime
   set ${matchStarted} = true
   ↓
Variable Loop End
```

### Configuration:

**Odds Snapshot Node:**
- Query Type: "Live Odds"
- Fixture ID: From flow variable
- Bookmaker filter: (your preferred bookmakers)

**Math Formula:**
- Formula: `(column("Current_Odd") - column("Baseline_Odd")) / column("Baseline_Odd") * 100`

**Row Filter:**
- Filter: Percentage change > 10%

### Quota Impact:
- ~60-120 calls per match (1-2 hours pre-match polling)
- Monitor quota carefully for multiple matches

## Best Practices

1. **Polling Frequency:**
   - Live matches: 2-5 minutes (API updates ~2 min)
   - Live odds: 1-5 minutes (depends on bookmaker refresh)
   - Never poll faster than API refresh rate

2. **Quota Management:**
   - Calculate total calls: Duration / Polling Interval
   - Example: 90 min match, 2 min polling = 45 calls
   - Limit simultaneous polling workflows

3. **Error Handling:**
   - Add Try/Catch around API calls
   - Continue loop even if one call fails
   - Log errors for debugging

4. **Match Filtering:**
   - Only poll matches you care about
   - Use Fixtures Selector to pre-filter
   - Stop polling when match finishes

5. **Caching:**
   - Write intermediate results to table
   - Allows resuming if workflow fails
   - Enables historical analysis

## Example: Complete Live Match Dashboard

```
[Initialization]
Fixtures Selector
   Date: Today
   Status: Not Started
   ↓
Row Filter (select matches of interest)
   ↓
[Polling Loop]
Variable Loop Start
   ↓
Wait 2 minutes
   ↓
Fixtures Node (Live)
   ↓
Merge with previous data
   ↓
Update dashboard table
   ↓
Check all matches finished
   ↓
Loop End

[Output]
Dashboard visualization
Live scoreboard
Event timeline
```

## Troubleshooting

**Issue:** Polling too slow, missing events
**Solution:** Reduce wait time (but respect API limits)

**Issue:** High quota consumption
**Solution:** Increase polling interval, reduce match count

**Issue:** Loop doesn't stop
**Solution:** Check end condition logic, add max iterations limit

**Issue:** Duplicate events
**Solution:** Add timestamp comparison, filter already seen events
```

---

## Implementation Timeline

### Week 1 (Days 1-5)
- **Day 1:** Create DateRangePanel component
- **Day 2:** Test DateRangePanel, integrate with Fixtures Selector
- **Day 3:** Create MultiSelectionPanel component
- **Day 4:** Test MultiSelectionPanel, integrate with Players dialog
- **Day 5:** Create IncrementalModePanel component

### Week 2 (Days 6-10)
- **Day 6:** Test IncrementalModePanel, integrate with Fixtures node
- **Day 7:** Enhance Fixtures node (date picker, incremental, H2H, live)
- **Day 8:** Enhance Fixtures Selector (date picker)
- **Day 9:** Enhance Players node (multi-select, new query types)
- **Day 10:** Test enhanced nodes, fix bugs

### Week 3 (Days 11-15)
- **Day 11:** Enhance Teams node (multi-select)
- **Day 12:** Create Teams Selector node
- **Day 13:** Test Teams Selector, integration with Teams node
- **Day 14:** Start Odds Snapshot node (structure, dialog)
- **Day 15:** Continue Odds Snapshot node (implement query types)

### Week 4 (Days 16-20)
- **Day 16:** Complete Odds Snapshot node (all 4 query types)
- **Day 17:** Test Odds Snapshot node
- **Day 18:** Write LIVE_POLLING_PATTERNS.md documentation
- **Day 19:** Integration testing all Phase 1 enhancements
- **Day 20:** Bug fixes, code review, commit & push

---

## Success Criteria

Phase 1 complete when:
- ✅ All 3 UI components created and tested
- ✅ Fixtures node enhanced (date picker, incremental, H2H, live)
- ✅ Players node enhanced (multi-select, 3 new query types)
- ✅ Teams Selector created
- ✅ Odds Snapshot created (4 query types including live)
- ✅ Live polling patterns documented
- ✅ All changes committed and pushed
- ✅ Integration tests pass
- ✅ Ready for Phase 2 (composite context nodes)

---

## Next Phase Preview

**Phase 2: Composite Context Nodes** (Weeks 5-6)
1. Player Profile node (transfers + trophies + sidelined + teams)
2. Match Predictions node (predictions + injuries)
3. Team Context node (coaches + venue + standings)

**Phase 3: Specialized Nodes** (Weeks 7-8)
1. Standings node
2. Venues node
3. Final testing and documentation

---

**Status:** Ready to begin - Starting with DateRangePanel
**Next Step:** Create DateRangePanel.java component
