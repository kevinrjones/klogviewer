# ADR-002: Functional Error Handling with Arrow Either

## Status
Accepted

## Context
Traditional exception-based error handling can make the control flow difficult to follow and often results in "blind spots" where errors are not explicitly handled. For a log viewer, where parsing and file access can fail in various ways, we need a more robust and explicit mechanism.

## Decision
We will use Arrow's `Either<L, R>` type for error handling throughout the domain and core layers.
- `L` (Left) will represent a failure, modeled using sealed interfaces for exhaustive error handling.
- `R` (Right) will represent a success.

This ensures that potential failures are reflected in the function signatures and forces the caller to handle them.

## Consequences
- **Pros**:
    - Explicit error handling at compile time.
    - Improved composability of operations (using `flatMap`, `map`, etc.).
    - Better alignment with functional programming principles.
- **Cons**:
    - Steeper learning curve for developers unfamiliar with functional patterns.
    - Slightly more boilerplate in simple cases.
