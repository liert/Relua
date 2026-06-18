# SSA Refactor Plan

## Goal

Relua currently mixes register value propagation, expression recovery, dead assignment removal, and control-flow heuristics across `RegisterStateAnalyzer`, `InstructionToASTConverter`, and `DataFlowAnalyzer`. This makes repeated bugs hard to fix globally: each new bytecode shape tends to need another local guard.

The refactor goal is to make SSA the canonical analysis layer between bytecode CFG and AST generation:

1. Build a complete SSA form for every Lua chunk.
2. Preserve control-flow joins with explicit phi nodes instead of collapsing conflicting values to `UNKNOWN`.
3. Drive expression recovery, temporary elimination, closure capture handling, and structured AST generation from SSA def-use facts.
4. Keep the existing generator running during migration, then replace one behavior at a time behind tests.

## Current Problems

- CFG joins lose information: `RegisterUtils.mergeRegisterStates` keeps equal values but converts conflicting values to `UNKNOWN`.
- There is no explicit definition identity. A physical register name such as `R4` can represent many unrelated lifetimes.
- Closure capture protection uses reaching definition PCs as a local patch, but the broader optimizer still reasons mostly by register name.
- Data-flow cleanup is AST-local and conservative around labels/goto, so fixes in one shape often miss equivalent shapes elsewhere.
- AST generation reads register states at a PC, so expression reconstruction happens before a reliable global def-use model exists.

## Target Pipeline

```text
Chunk bytecode
  -> BasicBlockBuilder
  -> ControlFlowGraphBuilder
  -> SsaBuilder
       - instruction read/write summary
       - dominators and dominance frontiers
       - phi placement
       - SSA rename
       - def-use/use-def chains
  -> SSA simplification
       - copy propagation
       - constant propagation
       - pure temporary elimination
       - phi pruning
  -> region/control structuring
  -> AST lowering
  -> AST cleanup/codegen
```

During migration the existing `RegisterStateAnalyzer` remains active, and SSA is attached to `DecompilerPipeline` as a side analysis. Once parity is good enough, AST conversion should consume SSA values directly.

SSA is mandatory. A chunk with invalid SSA is not allowed to continue to AST generation, because every later pass must be able to rely on stable value identity.

## Additional Decompiler Mechanisms

SSA is necessary but not sufficient. The broader refactor should also introduce these standard decompiler mechanisms:

- Expression DAG recovery: build expressions from SSA definitions, not from mutable register snapshots.
- Sparse conditional constant propagation: propagate constants through SSA and prune impossible branches when proven.
- Copy propagation and value forwarding: remove MOVE chains and temporary aliases through use-def chains.
- Side-effect aware dead code elimination: delete pure dead SSA definitions, but keep calls, table writes, upvalue writes, and volatile global effects.
- Phi lowering: lower surviving phi nodes to structured locals at branch/loop boundaries.
- Region-based structuring: recover if/elseif/else, while/repeat, numeric for, generic for, and early-return regions from CFG/SSA instead of raw PC ranges.
- Interval/live-range based local naming: assign readable locals from SSA live ranges and debug local metadata, avoiding stale physical register reuse.
- Effect and alias model: treat table mutation, global access, upvalue mutation, calls, vararg, and multi-return as explicit effects so expression inlining is safe.
- Pattern library over SSA: recognize common Lua bytecode idioms such as boolean short-circuit, nil guards, method calls, constructors, and iterator loops as SSA patterns.

## SSA Model

- `SsaValue`: one versioned definition of a Lua physical register, e.g. `R4_7`.
- `SsaInstruction`: bytecode instruction annotated with SSA defs and uses.
- `SsaPhi`: block-entry merge for one physical register, with one incoming value per predecessor.
- `SsaBlock`: SSA view of a `BasicBlock`, including phis and annotated instructions.
- `SsaFunction`: complete SSA result for a chunk.

Phi nodes are not emitted as Lua. They are analysis nodes used by lowering. If a phi survives to source generation, lowering decides whether it becomes a local variable assignment before branches, branch-local assignments to a shared local, or a structured expression.

## Construction Algorithm

1. Summarize each instruction's physical-register defs and uses.
2. Compute dominators, immediate dominators, dominator tree, and dominance frontier from CFG.
3. Insert phi nodes for every register defined in more than one block using dominance-frontier worklists.
4. Rename definitions with per-register stacks:
   - parameters start as `R0_0`, `R1_0`, etc.
   - reads without a known stack get an implicit unknown input version.
   - each phi gets a new version before instructions in its block.
   - each instruction use reads the current stack top.
   - each instruction def creates and pushes a new value.
   - successor phi incoming values are filled from the current stack top.
5. Build def-use/use-def metadata.

## Migration Phases

### Phase 1: Parallel SSA

- Add `decompiler.ssa` package.
- Build SSA after CFG construction.
- Expose `DecompilerPipeline.getSsaFunction(functionName)`.
- Add tests for phi insertion and versioned rename.

### Phase 2: SSA Diagnostics

- Dump SSA when `DecompilerDebugger` is enabled.
- Add golden/debug output for selected real firmware chunks.
- Add invariant checks:
  - every SSA use has a value;
  - every phi in a reachable block has incoming values for all reachable predecessors;
  - definitions dominate uses, except phi incoming edge semantics.

### Phase 3: SSA-Driven Cleanup

- Replace `DataFlowAnalyzer` dead temporary removal with SSA liveness and side-effect checks.
- Replace captured-upvalue PC protection with direct `SsaValue` identity.
- Replace same-register lifetime heuristics with SSA def-use chains.
- Build a value summary table (`SsaExpressionAnalyzer`) with purity, constants, copies, calls, and table effects.

### Phase 4: SSA Expression Recovery

- Lower single-use pure SSA values into expressions.
- Stop using `UNKNOWN` as a merge answer; keep phi values until lowering.
- Add copy propagation and constant propagation over SSA.

### Phase 5: SSA AST Lowering

- Generate AST from SSA blocks/regions rather than raw instruction ranges.
- Emit locals based on SSA live ranges and phi lowering.
- Let structured control restoration consume SSA-backed regions.
- Keep SSA-to-source naming in `SsaAstNameResolver`; opcode converters must query this policy instead of hard-coding register or version naming locally.

### Phase 6: Structured Region Engine

- Replace PC range slicing in `InstructionHandler` with region detection over CFG.
- Use dominance/post-dominance plus loop headers to recover structured source.
- Lower phi nodes at region entries/exits.
- Keep irreducible control flow as labels/goto only when structuring is not sound.

## Testing Strategy

- Unit tests for instruction read/write summaries.
- Synthetic CFG tests for if/else phi, loops, and uninitialized inputs.
- Regression tests using existing Xiaomi/TP-Link fixtures.
- Source quality assertions for recurring failures: stale register reuse, wrong branch merge, closure capture, table mutation, call side effects.
- SSA summary assertions over Xiaomi fixtures: every instruction has def-use identity, expression summaries exist for supported definitions, and effectful operations are not marked pure.

## First Execution Slice

This change starts Phase 1:

- implement SSA model and builder;
- compute dominators/frontiers;
- insert and rename phi nodes;
- attach SSA result to `DecompilerPipeline`;
- add tests that prove a branch merge produces an explicit phi instead of `UNKNOWN`.
