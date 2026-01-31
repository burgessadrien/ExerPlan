# ExerPlan Development Rules

These rules MUST be adhered to for all Kotlin and Android development in this project.

## 0. KISS (Keep It Simple, Stupid) - **MOST IMPORTANT**
- **Simplicity over Complexity:** If there is a simpler way to implement a feature, use it. Avoid "over-engineering."
- **Readability is Priority:** Write code for humans first, computers second.
- **Minimalism:** Don't add features or abstractions "just in case" you might need them later (YAGNI - You Ain't Gonna Need It).

## 1. Functional Programming - **SECOND HIGHEST PRIORITY**
- **Immutability:** Use `val` instead of `var` wherever possible. Use immutable collections (`List`, `Map`, `Set`).
- **Pure Functions:** Aim for functions that depend only on their input arguments and produce no side effects.
- **Avoid Side Effects:** Minimize global state changes. Keep state localized and manageable, especially within ViewModels.
- **Expressions over Statements:** Favor `if`, `when`, and `try` as expressions that return values.
- **Declarative Code:** Use Kotlin's functional operators (`map`, `filter`, `fold`, `flatMap`) over manual loops.

## 2. SOLID Principles
- **S: Single Responsibility Principle (SRP):** A class or module should have one, and only one, reason to change.
- **O: Open/Closed Principle (OCP):** Software entities should be open for extension, but closed for modification. Use interfaces and abstract classes.
- **L: Liskov Substitution Principle (LSP):** Subtypes must be substitutable for their base types. If it looks like a duck and quacks like a duck but needs batteries, you probably have the wrong abstraction.
- **I: Interface Segregation Principle (ISP):** Clients should not be forced to depend on methods they do not use. Prefer many small, specific interfaces over one large, general one.
- **D: Dependency Inversion Principle (DIP):** Depend on abstractions, not on concretions. High-level modules should not depend on low-level modules.

## 3. Meaningful Names
- **Use Intention-Revealing Names:** Names should tell you why it exists, what it does, and how it is used.
- **Avoid Disinformation:** Do not refer to a grouping of workouts as `workoutList` unless it is actually a `List`.
- **Make Meaningful Distinctions:** Avoid `workoutData` vs `workoutInfo`. Use names that distinguish the objects clearly.
- **Use Pronounceable Names:** Favor `WorkoutPlan` over `WktPln`.
- **Class Names:** Should be nouns or noun phrases (e.g., `WorkoutRepository`, `WorkoutViewModel`).
- **Method Names:** Should be verbs or verb phrases (e.g., `insertPlan`, `getAllWorkouts`).

## 4. Functions
- **Small!:** Functions should be very small (ideally < 20 lines).
- **Do One Thing:** A function should do one thing and do it well.
- **One Level of Abstraction per Function:** Mixing levels makes the function hard to read.
- **Function Arguments:** Ideal is zero (niladic), then one (monadic), then two (dyadic). Avoid three or more arguments.
- **Have No Side Effects:** Functions should not make hidden changes to state or global variables.
- **Command Query Separation:** Functions should either do something or answer something, but not both.
- **Don't Repeat Yourself (DRY):** Duplication is the root of all evil in software.

## 5. Comments
- **Don't comment bad code—rewrite it.**
- **Legal/Informative Comments:** Use only when necessary (e.g., specific business logic or regex explanations).
- **Clarification:** Only if the code is truly un-expressive (rare in Kotlin).
- **TODO Comments:** Use sparingly for future work.
- **Avoid Noise/Redundant Comments:** Don't explain what the code clearly says.

## 6. Formatting
- **Team Consistency:** Follow the project's vertical and horizontal formatting rules.
- **Vertical Openness:** Use empty lines to separate related concepts.
- **Vertical Density:** Keep related code close together.
- **Conceptual Affinity:** Keep dependent functions vertically close.

## 7. Error Handling
- **Use Exceptions rather than Return Codes:** Use Kotlin's `try-catch` or specialized result wrappers.
- **Provide Context with Exceptions:** Include enough information to trace the failure.
- **Don't Return Null:** Avoid returning nulls from methods to prevent NullPointerExceptions.
- **Don't Pass Null:** Avoid passing null into methods unless strictly required by an external API.

## 8. Classes
- **Small!:** Classes should be small and follow SRP.
- **Cohesion:** Classes should have a small number of instance variables that are used by many methods.
- **Organize for Change:** Structure classes to minimize the impact of changes.

## 9. Emergence (Simple Design Rules)
1. Runs all the tests.
2. Contains no duplication.
3. Expresses the intent of the programmer.
4. Minimizes the number of classes and methods.

## 10. Unit Tests
- **Keep Tests Clean:** Test code is as important as production code.
- **F.I.R.S.T Rules:**
    - **Fast:** Tests should be quick.
    - **Independent:** Tests should not depend on each other.
    - **Repeatable:** Tests should run in any environment.
    - **Self-Validating:** Tests should have a boolean output.
    - **Timely:** Tests should be written just before production code.

## 11. Design Patterns

### Object-Oriented Patterns (Creational)
- **Singleton:** Ensure a class has only one instance (e.g., `AppDatabase`).
- **Factory:** Use a central place to create objects with complex dependencies (e.g., `ViewModelProvider.Factory`).
- **Dependency Injection:** Provide dependencies from the outside rather than creating them inside (e.g., passing `WorkoutRepository` to `WorkoutPlanViewModel`).
- **Builder:** Construct complex objects step-by-step.

### Object-Oriented Patterns (Structural)
- **Repository:** Centralize data access logic and provide a clean API for the UI. Acts as the single source of truth.
- **Adapter:** Convert the interface of a class into another interface that clients expect (e.g., converting Domain models to UI models).
- **Facade:** Provide a simplified interface to a complex set of classes or libraries.
- **Proxy/Decorator:** Control access to an object or add behavior dynamically without changing its structure.

### Object-Oriented Patterns (Behavioral)
- **Observer:** Allow objects to be notified of state changes (e.g., using `StateFlow.collectAsState` in Compose).
- **Strategy:** Define a family of algorithms and make them interchangeable.
- **Command:** Encapsulate a request as an object, allowing for action parameterization (e.g., UI callbacks passed to composables).
- **State:** Allow an object to alter its behavior when its internal state changes (e.g., `UiState` objects in ViewModels).
- **Template Method:** Define the skeleton of an algorithm in a base class, letting subclasses override specific steps.

### Functional Patterns
- **Monads & Result Wrappers:** Wrap operations that can fail (e.g., `Result<T>`) to handle success and failure explicitly without nested try-catches.
- **Higher-Order Functions:** Functions that take functions as parameters or return them (e.g., `.map { ... }`, `.filter { ... }`).
- **Function Composition:** Building complex logic by chaining simple, single-purpose functions together.
- **Partial Application & Currying:** Creating new functions by fixing a number of arguments to an existing function.
- **Lazy Evaluation:** Deferring heavy computation until the result is absolutely needed (e.g., `by lazy`).
- **Memoization:** Caching the results of expensive function calls based on input parameters.
- **Tail Recursion:** Optimizing recursive calls to avoid stack overflow errors (using `tailrec`).
- **Declarative Data Pipelines:** Processing data through a series of transformations (`filter`, `map`, `reduce`, `flatMap`).
