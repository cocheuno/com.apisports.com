# Contributing to Carovex API-Sports KNIME Extension

Thank you for your interest in contributing to the Carovex API-Sports KNIME Extension! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)
- [Contact](#contact)

## Code of Conduct

This project follows our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to info@caronelabs.com.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally
3. **Set up the development environment** (see below)
4. **Create a branch** for your changes
5. **Make your changes** and test them
6. **Submit a pull request**

## How to Contribute

### Types of Contributions

We welcome many types of contributions:

- **Bug fixes** - Fix issues in existing functionality
- **New features** - Add new nodes or capabilities
- **Documentation** - Improve or add documentation
- **Tests** - Add or improve test coverage
- **Performance** - Optimize existing code
- **Translations** - Help translate documentation

### What We're Looking For

- New query nodes for additional API-Sports endpoints
- Improved error handling and user feedback
- Better caching strategies
- Documentation improvements
- Example workflows (see `docs/workflows/` for format)
- Performance optimizations

## Development Setup

### Prerequisites

- **Java 17** or higher (JDK, not just JRE)
- **Maven 3.8+**
- **Eclipse IDE** with Tycho support (recommended)
- **Git**
- **API-Sports.io API key** for testing

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/cocheuno/com.apisports.com.git
   cd com.apisports.com
   ```

2. **Build the project**
   ```bash
   mvn clean verify
   ```

3. **Import into Eclipse**
   - File > Import > Maven > Existing Maven Projects
   - Select the repository root directory
   - Import all projects

4. **Set Target Platform**
   - Window > Preferences > Plug-in Development > Target Platform
   - Add a target platform pointing to KNIME 5.5.0+

5. **Run KNIME from Eclipse**
   - Right-click any bundle > Run As > Eclipse Application
   - Select KNIME product configuration

### Project Structure

```
com.apisports.com/
├── bundles/                    # OSGi bundles (the actual code)
│   ├── com.apisports.knime.core/       # Core utilities
│   ├── com.apisports.knime.port/       # Port objects
│   ├── com.apisports.knime.connector/  # Connector nodes
│   └── com.apisports.knime.football/   # Football nodes
├── features/                   # Eclipse features
├── releng/                     # Release engineering
├── docs/                       # Documentation
└── pom.xml                     # Parent POM
```

### Running Tests

```bash
# Run all tests
mvn verify

# Run tests for a specific bundle
mvn verify -pl bundles/com.apisports.knime.football
```

## Coding Standards

### Java Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use meaningful variable and method names

### KNIME Node Standards

- All nodes must have:
  - `*NodeFactory.java` - Factory class
  - `*NodeFactory.xml` - Node description
  - `*NodeModel.java` - Business logic
  - `*NodeDialog.java` - Configuration dialog (if needed)
  - `Carovex.png` - Node icon (16x16 PNG)

- Follow KNIME naming conventions:
  - Node names should be descriptive
  - Use title case in dialog labels
  - Provide helpful tooltips

### Documentation

- Add Javadoc to all public classes and methods
- Update user documentation for user-facing changes
- Include code examples where helpful

### File Headers

All source files must include the Apache 2.0 license header:

```java
/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation only
- `style` - Code style (formatting, etc.)
- `refactor` - Code refactoring
- `test` - Adding tests
- `chore` - Maintenance tasks

### Examples

```
feat(fixtures): add head-to-head query mode

fix(connector): handle rate limit exceeded gracefully

docs(readme): update installation instructions

refactor(core): extract HTTP client to separate class
```

## Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/my-new-feature
   ```

2. **Make your changes**
   - Write clean, documented code
   - Add tests for new functionality
   - Update documentation as needed

3. **Test your changes**
   ```bash
   mvn clean verify
   ```

4. **Commit your changes**
   - Use meaningful commit messages
   - Reference issues where applicable

5. **Push to your fork**
   ```bash
   git push origin feature/my-new-feature
   ```

6. **Create a Pull Request**
   - Use the PR template
   - Describe your changes clearly
   - Link related issues

7. **Code Review**
   - Address review feedback
   - Keep the PR focused and manageable

### PR Checklist

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] New code has appropriate test coverage
- [ ] Documentation is updated
- [ ] License headers are present
- [ ] Commit messages follow conventions
- [ ] PR description is complete

## Reporting Issues

### Before Reporting

1. Check existing issues for duplicates
2. Try the latest version
3. Gather relevant information

### Issue Template

When creating an issue, please include:

- **Description** - Clear description of the issue
- **Steps to Reproduce** - How to reproduce the problem
- **Expected Behavior** - What should happen
- **Actual Behavior** - What actually happens
- **Environment** - KNIME version, Java version, OS
- **Screenshots** - If applicable
- **Logs** - Relevant error messages

### Feature Requests

For feature requests, please describe:

- **Use Case** - Why is this feature needed?
- **Proposed Solution** - How should it work?
- **Alternatives** - Other approaches considered
- **Additional Context** - Any other relevant information

## Adding New Nodes

To add a new query node:

1. **Create the node package** in `com.apisports.knime.football.nodes.query`

2. **Implement required classes**:
   - `MyNodeFactory.java`
   - `MyNodeFactory.xml`
   - `MyNodeModel.java`
   - `MyNodeDialog.java`
   - Copy `Carovex.png` icon

3. **Register in plugin.xml**:
   ```xml
   <node category-path="/carovex/apisports/football"
         factory-class="...MyNodeFactory"/>
   ```

4. **Update documentation**:
   - Add to README.md
   - Add to user guide

5. **Add tests**

## Contact

- **Email**: info@caronelabs.com
- **Issues**: [GitHub Issues](https://github.com/cocheuno/com.apisports.com/issues)
- **Discussions**: [GitHub Discussions](https://github.com/cocheuno/com.apisports.com/discussions)

## License

By contributing to this project, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing to the Carovex API-Sports KNIME Extension!
