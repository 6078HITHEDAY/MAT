# MAT — MC Automation Tools

## Overview

A Fabric client-side mod that embeds GraalJS 24.x in Minecraft 1.20.4 to execute JavaScript scripts for automation. Exposes pathfinding (Baritone), inventory, chat, and player state as composable API bindings. Scripts run in background threads with cooperative cancellation; all API calls dispatch to the Minecraft main thread.

Target: Minecraft 1.20.4, Java 17, Fabric Loader 0.19.1, Fabric API 0.97.3+1.20.4. Licensed LGPL-3.0.

## Build & Commands

```bash
./gradlew build          # produces build/libs/mat-*.jar
./gradlew runClient      # launch client (Fabric Loom)
```

GraalJS libraries are bundled via Loom's `include` (JIJ). Baritone is optional — detected at runtime via classpath introspection.

Key properties (gradle.properties): `graaljs_version=24.2.2`, `minecraft_version=1.20.4`, `loader_version=0.19.1`.

## Source Layout

The project uses Fabric Loom's `splitEnvironmentSourceSets()`:

```
src/
  main/                           # Shared (compiled into both client & server jars)
    java/cn/myflycat/mat/         #   Shared code
    resources/                    #   fabric.mod.json, mat.mixins.json (shared mixin config)
  client/                         # Client-only
    java/cn/myflycat/mat/client/  #   Client code
      api/                        #   JS API bindings (GraalJS @HostAccess.Export)
      command/                    #   /mat command tree (Brigadier)
      gui/                        #   Script Builder screen
      inventory/                  #   Client-side slot operations
      recorder/                   #   Action recording & JS codegen
    java/cn/myflycat/mat/mixin/client/  # Client mixins
    resources/                          # Client resources (mat.client.mixins.json)
```

## Architecture

### Script Engine (`src/main/java/.../script/`)

| Class | Role |
|-------|------|
| `ScriptEngine` | Wraps a GraalVM `Engine` (shared across contexts). Creates isolated `Context` per script. Sandboxed: no native access, no threading, no process creation, `HostAccess.EXPLICIT`. |
| `ScriptManager` | Manages lifecycle — start/stop/list/reload. Each script gets a daemon thread (`MAT-script-<name>`). Tracks running scripts in `ConcurrentHashMap`. |
| `ScriptHandle` | Script state machine: READY -> RUNNING -> { DONE \| FAILED \| CANCELLED }. Atomic state transitions. Exposes `CompletableFuture<Void> completion()` for exit notification. Cooperative cancellation via `AtomicBoolean`. |
| `ScriptSource` | Record holding name, path, mtime, and cached GraalVM `Source`. |
| `ScriptSourceLoader` | Reads `.js` files from `config/mat/scripts/`. Path traversal guard (`resolveInsideRoot`). Cyclic import detection via thread-local stack. Mtime-based cache invalidation. |
| `ModuleRegistry` | CommonJS-style `require()`: wraps each module in `(function(module,exports,require){...})`, caches exports. Entry script evaluated via `runEntry(name)`. |

Cancellation is cooperative: `mat.sleep()` polls every 50ms and throws `InterruptedException` on cancel. Pure computation loops must check `mat.cancelled()` manually.

### API Bindings (`src/client/.../api/`)

`ApiRoot` is the root object exposed as `mat` in JS. Each namespace is a `@HostAccess.Export` field initialized in constructor.

| Namespace | Class | Key methods |
|-----------|-------|-------------|
| `mat.chat` | `ChatBindings` | `log(msg)` — local chat HUD message, `send(msg)` — send to server (detects `/` prefix) |
| `mat.player` | `PlayerBindings` | `pos()`, `yaw()`, `pitch()`, `onGround()`, `health()`, `food()`, `saturation()`, `dimension()` |
| `mat.control` | `ControlBindings` | `sleep(ms)`, `cancelled()` — cooperative cancellation, polls every 50ms |
| `mat.inventory` | `InventoryBindings` | `slots()`, `find(query)`, `hand()`, `selectHotbar(n)`, `drop(ref)`, `quickMove(ref)`, `swap(hotbarSlot, ref)`, `closeContainer()`, `containerType()` |
| `mat.baritone` | `BaritoneBindings` | `gotoBlock(x,y,z)`, `gotoXZ(x,z)`, `gotoNear(x,y,z,radius)`, `mine(quantity, names...)`, `isPathing()`, `cancel()` — all no-op if Baritone absent |
| `mat.recorder` | `RecorderBindings` | `start()`, `stop()`, `isRecording()`, `save(name)` |

Main-thread dispatch via `ClientThread.runSync()` / `runSyncVoid()` — uses `MinecraftClient.submit()` + `CompletableFuture.get()`, rethrows `ExecutionException` cause.

**To add a new API namespace**: Create a class in `api/` with `@HostAccess.Export` methods, add a `@HostAccess.Export public final` field to `ApiRoot`, initialize in constructor.

### Inventory (`src/main/.../inventory/` + `src/client/.../inventory/`)

- `SlotRef` (shared): Data class with `container`, `slot`, `id`, `count`. All fields `@HostAccess.Export`.
- `ItemQuery` (shared): Parsed from JS value — can be string (item ID) or object `{id, tag, minCount, container}`.
- `InventoryAccess` (client): All slot operations on the Minecraft main thread. Container-aware: when a `HandledScreen` is open, `collectSlots()` returns container slots + player inventory + hotbar (excludes armor/offhand). `screenSlot()` remaps slot indices based on container offset.

Container naming: `"container"` (0..N), `"hotbar"` (0-8), `"inventory"` (0-26), `"armor"` (0-3), `"offhand"` (0).

### Recording (`src/main/.../recorder/` + `src/client/.../recorder/`)

| Component | Layer | Role |
|-----------|-------|------|
| `ActionType` | shared | Enum: BREAK_BLOCK, PLACE_BLOCK, USE_ITEM, ATTACK_ENTITY, INTERACT_ENTITY, MOVE, CHAT |
| `ActionRecord` | shared | Immutable record of one action: type + timestamp + params map |
| `Recorder` | client | Singleton. `CopyOnWriteArrayList` of records. Client tick listener samples movement every 10 ticks (>0.5 block threshold). |
| `CodeGenerator` | client | Converts `List<ActionRecord>` to JS script with `mat.sleep()` delays and `// TODO` placeholders. |

Events intercepted via mixins in `MixinClientPlayerInteractionManager` (breakBlock, interactBlock/Place, interactItem, attackEntity, interactEntity) and `MixinClientPlayNetworkHandler` (sendChatMessage, sendChatCommand — filters out `/mat ` prefixed commands).

**To add a new action type**: Add to `ActionType` enum, add record method to `Recorder`, add codegen branch in `CodeGenerator.emitAction()`.

### Script Builder (`src/client/.../gui/`)

| Component | Role |
|-----------|------|
| `ActionStep` (shared, `editor/`) | Data model for one step in a visual script. Each has a type string (e.g., `"chat.send"`, `"baritone.gotoBlock"`) and a `LinkedHashMap<String,String>` of params. `toJavaScript()` generates the JS code for the step. |
| `ScriptBuilderScreen` (client) | Minecraft `Screen` subclass. Currently a skeleton — opens via `/mat new <name>` or `/mat edit <name>`. |
| `ScriptCodegen` (client) | Takes script name + `List<ActionStep>` → complete `.js` file content with header comment. |

ActionStep type constants are defined as `public static final String` in `ActionStep`. **To add a new action type to the Script Builder**: Add a `TYPE_*` constant + case in `toJavaScript()` switch, and a `case` in `ScriptCodegen.generate()`.

### Command System (`src/client/.../command/`)

Registered in `MatCommand.register(ScriptManager)` via `ClientCommandRegistrationCallback`. Uses Fabric API's `ClientCommandManager` (Brigadier).

Tree:
```
/mat
  run <name>          — start script (with tab completion of available scripts)
  stop <name|all>     — request cancellation
  list                — show available & running scripts
  reload              — clear source cache
  record start|stop|save <name>
  new <name>          — open Script Builder for new script
  edit <name>         — open Script Builder for existing script
```

Errors from cancelled/failed scripts are reported asynchronously via `ScriptHandle.completion()` callback. JS stack traces appear as multi-line red chat output (filters out host frames, shows up to 6 script frames).

**To add a new command**: Add a `.then(...)` to the `dispatcher.register(...)` tree in `MatCommand.register()`, implement a static handler method.

### Mixins

Client-only mixins in `mat.client.mixins.json`:

| Mixin | Target | Hooks |
|-------|--------|-------|
| `MixinClientPlayerInteractionManager` | `ClientPlayerInteractionManager` | `breakBlock`, `interactBlock`, `interactItem`, `attackEntity`, `interactEntity` — all at HEAD, pass to `Recorder` |
| `MixinClientPlayNetworkHandler` | `ClientPlayNetworkHandler` | `sendChatMessage`, `sendChatCommand` at HEAD, pass to `Recorder` (filters MAT internal commands) |

Shared mixin config (`mat.mixins.json`) is currently empty — reserved for potential server-side mixins.

**To add a new mixin**: Create class in `mixin/client/`, annotate with `@Mixin`, add `@Inject` methods, register in `mat.client.mixins.json`.

## Constraints

- Client-only mod (`"environment": "client"` in fabric.mod.json)
- No server-side code execution
- Scripts run on daemon background threads; all Minecraft API access must go through `ClientThread.runSync()`
- GraalJS `HostAccess.EXPLICIT` — only `@HostAccess.Export` members are visible to JS
- GraalJS 24.x pinned (25.x requires Java 21+)
- Baritone is optional — `BaritoneBindings` gracefully degrades to no-ops
