# Codebase Maintenance Instructions

## Goal

Perform routine maintenance on random parts of the meme-wiki-be codebase to ensure code quality, consistency, and catch potential issues early. This is not about being overly pedantic or making changes for the sake of changes - it's about keeping an eye on the codebase and identifying genuine issues.

## Approach

### 1. Select Random Area

Choose a random part of the codebase to inspect. Use one of these strategies:

```bash
# Get a random Java file
find src -name "*.java" -not -path "*/build/*" -not -path "*/.gradle/*" -not -path "*/generated/*" | shuf -n 1

# Get a random package/directory
find src -type d -not -path "*/build/*" -not -path "*/.gradle/*" | shuf -n 1

# Pick from core areas randomly
# - src/main/java/spring/memewikibe/api/controller/
# - src/main/java/spring/memewikibe/application/
# - src/main/java/spring/memewikibe/domain/
# - src/main/java/spring/memewikibe/infrastructure/
# - src/test/java/spring/memewikibe/
```

**Important**: You're not limited to the file you randomly selected. If investigating reveals related files that need attention, follow the trail. The random selection is just a starting point.

## 2. What to Check

### Code Style & Formatting
- **Java conventions**: Proper use of records, sealed classes, switch expressions (Java 21 features)
- **Naming consistency**: Follow existing patterns in the codebase
- **Import organization**: Remove unused imports, prefer explicit imports over wildcards
- **Code structure**: Proper indentation, spacing, line breaks
- **Documentation**: Javadoc comments where needed (public APIs, complex logic)
- **Lombok usage**: Appropriate use of @Getter, @Builder, @RequiredArgsConstructor, etc.

### Code Quality Issues
- **Null safety**: Proper use of Optional, null checks, @NonNull annotations
- **Error handling**: Appropriate exception handling, meaningful error messages
- **Code duplication**: Identify repeated code that could be extracted
- **Dead code**: Unused methods, parameters, variables
- **TODOs/FIXMEs**: Check if old TODOs are still relevant or can be addressed
- **Magic numbers/strings**: Should be named constants or configuration properties
- **Complex conditionals**: Can they be simplified or extracted?

### Potential Bugs
- **Off-by-one errors**: Especially in loops and pagination logic
- **Edge cases**: Empty collections, null values, boundary conditions
- **Type safety**: Unnecessary casts, unchecked casts
- **Resource handling**: Proper cleanup, try-with-resources
- **Concurrency issues**: Thread safety if applicable
- **State management**: Proper initialization, mutation patterns
- **Transaction boundaries**: Verify @Transactional is used correctly

### Architecture & Design
- **Separation of concerns**: Does the code have a single responsibility?
- **Dependency direction**: Are dependencies pointing the right way? (Controller -> Service -> Repository)
- **Abstraction level**: Consistent level of abstraction within methods
- **Spring conventions**: Proper use of Spring Boot features and annotations
- **REST API design**: Consistent endpoint naming, HTTP methods, status codes

### Testing
- **Test coverage**: Are there tests for the code you're reviewing?
    - If checking a specific service or controller, verify that tests exist for it
    - If tests exist, check if they cover the needed cases (edge cases, error conditions, typical usage)
    - If tests don't exist or coverage is incomplete, consider creating comprehensive test coverage
- **Test quality**: Do tests cover edge cases?
- **Test naming**: Clear, descriptive test names
- **Test isolation**: Proper use of mocking, test fixtures

## 3. Investigation Strategy

Don't just look at surface-level issues. Dig deeper:

1. **Read the code**: Understand what it does before suggesting changes
2. **Check related files**: Look at callers, implementations, tests
3. **Look at git history**: `git log --oneline <file>` to understand context
4. **Find related issues**: Search for TODOs, FIXMEs, or commented code
5. **Run tests**: If you make changes, ensure tests pass
6. **Check dependencies**: Look for outdated or vulnerable dependencies

## 4. When to Make Changes

**DO fix**:
- Clear bugs or logic errors
- Obvious code quality issues (unused imports, etc.)
- Misleading or incorrect documentation
- Code that violates established patterns
- Security vulnerabilities
- Performance issues with measurable impact
- Deprecated API usage

**DON'T fix**:
- Stylistic preferences if existing code is consistent
- Working code just to use "newer" patterns
- Minor formatting if it's consistent with surrounding code
- Things that are subjective or arguable
- Massive refactorings without clear benefit

**When in doubt**: Document the issue in your report but don't make changes.

## 5. Making Changes

If you decide to make changes:

1. **Create a feature branch**: Use descriptive branch naming
    - Format: `maintenance/<area>-<brief-description>`
    - Examples:
      - `maintenance/meme-service-null-safety`
      - `maintenance/controller-unused-imports`
      - `maintenance/quiz-service-refactor`
    - Keep it short but descriptive
2. **Make focused commits**: One logical change per commit
    - If the change affects many files or is complicated, split it into multiple step-by-step commits
    - This makes it easier for reviewers to understand the changes
    - Example: First commit removes unused imports, second commit refactors method, third commit adds tests
3. **Write clear commit messages**: Explain why, not just what
4. **Run tests**: `./gradlew test`
5. **Check build**: `./gradlew build`

## 6. Examples

### Good Maintenance Examples

**Example 1: Found and fixed null safety issue**
```
Inspected: src/main/java/spring/memewikibe/application/MemeAggregationServiceImpl.java

Issues found:
- Several methods accessing Optional.get() without isPresent() check
- Could cause NoSuchElementException in edge cases

Changes:
- Replaced Optional.get() with orElseThrow() with meaningful exception
- Added null checks with proper error handling
- Added Javadoc explaining preconditions
```

**Example 2: No changes needed**
```
Inspected: src/main/java/spring/memewikibe/api/controller/MemeController.java

Checked:
- Code style and formatting 
- Null safety 
- Error handling 
- Tests present and comprehensive 

Observations:
- Code is well-structured and follows Spring conventions
- Good test coverage including edge cases
- Documentation is clear
- No issues found
```

**Example 3: Found issues but didn't fix**
```
Inspected: src/test/java/spring/memewikibe/application/QuizServiceTest.java

Issues noted:
- Some test names could be more descriptive
- Potential for extracting common setup code
- Tests are comprehensive but could add edge case for empty quiz list

Recommendation: These are minor quality-of-life improvements.
Not critical, but could be addressed in future cleanup.
```

## Meme-Wiki-BE Specific Considerations

- **Spring Boot 3.5**: Use latest Spring Boot features and patterns
- **Java 21**: Take advantage of modern Java features (records, pattern matching, virtual threads)
- **JPA/QueryDSL**: Follow repository patterns, proper transaction management
- **REST API**: Maintain consistent response format (ApiResponse, ErrorMessage)
- **Firebase**: Proper handling of notification service integration
- **AWS S3**: Ensure image upload service handles errors gracefully
- **Clova API**: Check external API integration and error handling
- **Security**: Verify SecurityConfig and authentication/authorization logic
- **Caching**: Review cache invalidation and consistency (@Cacheable usage)
- **Scheduled tasks**: Check cron expressions and task execution logic

## Commands Reference

```bash
# Run tests
./gradlew test

# Run specific test class
./gradlew test --tests "ClassName"

# Build project
./gradlew build

# Clean build
./gradlew clean build

# Check for dependency vulnerabilities
./gradlew dependencyCheckAnalyze

# Run application locally
./gradlew bootRun
```

## Final Notes

- **Be thorough but practical**: Don't waste time on nitpicks
- **Context matters**: Understand why code is the way it is before changing
- **Quality over quantity**: One good fix is better than ten trivial changes
- **Document your process**: Help future maintainers understand your thinking
- **Learn from the code**: Use this as an opportunity to understand the codebase better
- **Security first**: Always consider security implications of changes
- **Performance awareness**: Be mindful of database queries, API calls, and caching

Remember: The goal is to keep the codebase healthy, not to achieve perfection. Focus on genuine improvements that make the code safer, clearer, or more maintainable.
