# ADR-003: UI Architecture with MVI

## Status
Accepted

## Context
As the UI becomes more complex with real-time updates, highlighting, and search, managing state becomes challenging. We need a predictable way to handle state changes and UI events.

## Decision
We will use the Model-View-Intent (MVI) architecture for the UI layer.
- **Model (State)**: An immutable data class representing the entire state of a screen.
- **View**: Compose functions that render the State.
- **Intent (Action)**: User actions (e.g., `LoadFile`, `Search`) that are sent to the ViewModel.
- **Event (Side Effect)**: One-time events (e.g., `ShowToast`, `Navigate`) that don't change the state directly.

The `KLogViewerViewModel` will process Intents and produce new States in a unidirectional data flow.

## Consequences
- **Pros**:
    - Predictable state management.
    - Improved testability of the presentation logic.
    - Unidirectional data flow reduces bugs related to inconsistent UI state.
- **Cons**:
    - Can lead to some boilerplate for very simple UI interactions.
