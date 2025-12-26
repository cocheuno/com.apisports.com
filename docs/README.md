# Documentation Directory

This directory contains all documentation for the API-Sports KNIME extension project.

## Directory Structure

```
docs/
├── architecture/           # Technical architecture and design documents
│   └── descriptor-schema.md
│
├── development/           # Implementation plans and summaries
│   ├── implementation-plan-v2.md
│   └── v2-implementation-summary.md
│
├── requirements/          # Feature requests and requirements (for Claude)
│   └── README.md         # Template for writing requirements
│
├── user/                 # User-facing documentation
│   └── football-nodes-guide.md
│
└── README.md             # This file
```

## Document Types

### Architecture (`architecture/`)
Technical design documents, schemas, and architectural decisions.

**Audience**: Developers, architects
**Examples**:
- API endpoint descriptor schemas
- Database schema designs
- Integration patterns
- Architecture Decision Records (ADRs)

---

### Development (`development/`)
Implementation plans, progress summaries, and development notes.

**Audience**: Developers, project managers
**Examples**:
- Implementation plans for major features
- Sprint summaries
- Technical debt tracking
- Migration guides

---

### Requirements (`requirements/`)
**⭐ This is where you put feature requests for Claude to implement!**

Feature requests, enhancements, and requirements written in a structured format that Claude can read and implement.

**Audience**: Product owners, Claude AI
**How to use**:
1. Copy the template from `requirements/README.md`
2. Fill in your feature requirements
3. Commit to the repo
4. Ask Claude: "Implement docs/requirements/your-feature.md"

**Examples**:
- New node features
- Bug fixes
- API enhancements
- UI improvements

---

### User Documentation (`user/`)
End-user guides, tutorials, and how-to documents.

**Audience**: KNIME users, data analysts
**Examples**:
- Football Nodes User Guide
- Quick start tutorials
- Common use cases
- Troubleshooting guides

---

## Working with Claude

Claude can read any markdown file in this repository. Here's how to use documentation effectively:

### For New Features:
```
User: "Please implement the feature described in docs/requirements/player-filters.md"
Claude: [Reads file, implements code, commits changes]
```

### For Multiple Features:
```
User: "Review all requirements in docs/requirements/ and implement high priority items"
Claude: [Reads all files, prioritizes, implements in order]
```

### For Understanding Architecture:
```
User: "Read docs/architecture/descriptor-schema.md and explain how it works"
Claude: [Reads file, provides explanation]
```

### For Updates:
```
User: "Update the user guide in docs/user/ with the new cache TTL feature"
Claude: [Updates documentation]
```

---

## Best Practices

### For Requirements Documents:
✅ Use the template in `requirements/README.md`
✅ Include specific examples and test cases
✅ Reference API documentation
✅ List affected files/nodes
✅ Specify priority

### For Architecture Documents:
✅ Include diagrams (can be ASCII art or links to images)
✅ Explain "why" decisions were made
✅ Document trade-offs
✅ Keep up-to-date with code changes

### For Development Documents:
✅ Track implementation progress
✅ Document blockers and solutions
✅ Link to related pull requests
✅ Update status as work progresses

### For User Documentation:
✅ Write for non-technical users
✅ Include screenshots/examples
✅ Provide step-by-step instructions
✅ Include troubleshooting section

---

## Contributing

When adding new documentation:

1. **Choose the right directory** based on audience and purpose
2. **Use markdown format** (.md extension)
3. **Follow existing naming conventions**:
   - Use kebab-case: `my-feature-name.md`
   - Be descriptive: `player-position-filter.md` not `feature1.md`
4. **Include metadata** at the top:
   ```markdown
   # Document Title
   **Date**: YYYY-MM-DD
   **Author**: Your Name
   **Status**: Draft/Final/Deprecated
   ```
5. **Link related documents**:
   ```markdown
   See also:
   - [Architecture Overview](../architecture/overview.md)
   - [Implementation Plan](../development/plan.md)
   ```

---

## Document Status

| Document | Status | Last Updated |
|----------|--------|--------------|
| architecture/descriptor-schema.md | Final | 2025-12-19 |
| development/implementation-plan-v2.md | Final | 2025-12-19 |
| development/v2-implementation-summary.md | Final | 2025-12-19 |
| user/football-nodes-guide.md | Final | 2025-12-26 |
| requirements/README.md | Final | 2025-12-26 |

---

## Quick Links

- **User Guide**: [Football Nodes Guide](user/football-nodes-guide.md)
- **How to Write Requirements**: [Requirements README](requirements/README.md)
- **v2.0 Architecture**: [Implementation Summary](development/v2-implementation-summary.md)
- **Endpoint Schema**: [Descriptor Schema](architecture/descriptor-schema.md)
