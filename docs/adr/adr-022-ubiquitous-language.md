# ADR 022: Formalization of Ubiquitous Language

## Status
Accepted

## Context
As the LogViewer project grows in complexity, with features like interleaving, structured data, and heuristic template detection, it is essential to have a shared, unambiguous vocabulary. Without a formalized Ubiquitous Language, developers and stakeholders might use different terms for the same concept (e.g., "Merge" vs "Interleave", "Record" vs "Entry"), leading to confusion in communication and inconsistencies in the codebase.

## Decision
We will establish and maintain a `docs/UBIQUITOUS_LANGUAGE.md` file that serves as the single source of truth for domain terminology. 

1.  **Terminology Alignment**: All new code, documentation, and communication must align with the terms defined in this document.
2.  **Continuous Evolution**: The Ubiquitous Language is not static. It should be updated whenever new domain concepts are introduced or existing ones are refined.
3.  **Code Consistency**: Core domain types in the `:domain` module should directly reflect the terms in the Ubiquitous Language (e.g., `LogEntry`, `LogSource`, `LogUpdate`).

## Consequences
- **Positive**: Improved communication between team members.
- **Positive**: Higher code maintainability due to consistent naming.
- **Positive**: Easier onboarding for new contributors.
- **Negative**: Slight overhead in maintaining an additional documentation file.
