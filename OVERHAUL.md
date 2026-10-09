# WynnBubbles 2.0 Overhaul — Dev Notes

Target: Minecraft **1.21.11**, Fabric Loom **1.13-SNAPSHOT** (resolved 1.13.6), Mojang mappings.  
Reference mod: [Talk-Balloons](https://github.com/CerbonesTeam/TalkBalloons) (Architectury, 1.21.11 branch).

---

## What Changed

### Build configuration
| Item | Before | After |
|---|---|---|
| MC version | 1.21.4 | 1.21.11 |
| Mappings | Yarn | `loom.officialMojangMappings()` |
| Fabric API | `0.111.x+1.21.4` | `0.140.0+1.21.11` |
| Cloth Config | previous | `21.11.153` |
| ModMenu | previous | `17.0.0-alpha.1` |
| Loom plugin | `1.6-SNAPSHOT` | `1.13-SNAPSHOT` |
| Gradle wrapper | 7.x | 9.4.0 |
| Wynntils | not present | local jar in `lib/` (drop jar there, gitignored) |

Key `build.gradle` changes:
- `base.archivesName = ...` replaces `archivesBaseName` (removed in Gradle 8+)
- `mappings loom.officialMojangMappings()` goes in `dependencies {}`, not `loom {}`
- `configurations.all { exclude group: "net.fabricmc.fabric-api", module: "fabric-content-registries-v0" }` — see Build Gotchas below

---

### Mapping rename cheatsheet (Yarn → Mojang 1.21.11)
| Yarn | Mojang 1.21.11 |
|---|---|
| `MatrixStack` | `PoseStack` |
| `VertexConsumerProvider` | `MultiBufferSource` |
| `TextRenderer` | `Font` |
| `PlayerEntityRenderer` | `AvatarRenderer` |
| `PlayerEntityRenderState` | `AvatarRenderState` |
| `AbstractClientPlayerEntity` | `AbstractClientPlayer` |
| `MinecraftClient` | `Minecraft` |
| `ChatHud` | `ChatComponent` |
| `Text` | `Component` |
| `RenderLayer` | `RenderType` |
| `Identifier` | `Identifier` (same class, `net.minecraft.resources`) |
| `ResourceLocation` | **does not exist** — use `Identifier` |
| `Identifier.of(String)` | `Identifier.parse("namespace:path")` |
| `LivingEntityRenderer.render()` | `LivingEntityRenderer.submit()` |
| new: | `CameraRenderState` (camera orientation quaternion) |
| new: | `SubmitNodeCollector` (replaces `MultiBufferSource` in render path) |
| new: | `AvatarRenderState.boundingBoxHeight` |

---

### New / changed files

#### New files
| File | Purpose |
|---|---|
| `util/ChatType.java` | Enum: NORMAL, PARTY, GUILD, PRIVATE |
| `util/HistoricalData.java` | Circular buffer (ported from Talk-Balloons), max N entries |
| `util/BubbleMessage.java` | Record: `Component message`, `ChatType chatType`, `int createdAtTick` |
| `util/WynnChatDetector.java` | All Wynncraft unicode detection / chat cleaning extracted here |
| `client/BubbleRenderer.java` | Full RenderPipeline renderer (adapted from Talk-Balloons for 1.21.11) |
| `compat/WynntilsCompat.java` | Detects Wynntils via FabricLoader, provides configurable Y offset |

#### Deleted files
| File | Reason |
|---|---|
| `util/RenderBubble.java` | Replaced entirely by `BubbleRenderer.java` |
| `mixin/DrawContextAccessor.java` | No longer needed |

#### Modified files
- `accessor/AbstractClientPlayerEntityAccessor.java` — now uses `HistoricalData<BubbleMessage>`
- `accessor/PlayerEntityRenderStateAccessor.java` — now uses `HistoricalData<BubbleMessage>`
- `mixin/AbstractClientPlayerEntityMixin.java` — stores `HistoricalData<BubbleMessage>(5)`
- `mixin/PlayerEntityRenderStateMixin.java` — target `AvatarRenderState`
- `mixin/PlayerEntityRendererMixin.java` — target `AvatarRenderer`, injects into `extractRenderState`, per-message expiry via `removeIf`
- `mixin/LivingEntityRendererMixin.java` — injects into `submit()` (not `render()`), passes `cameraState.orientation` to renderer
- `mixin/ChatHudMixin.java` — uses `WynnChatDetector`, stores `BubbleMessage` instead of raw strings
- `config/WynnBubblesConfig.java` — added: `wynntilsOffset`, `balloonsHeightOffset`, `maxBubbles`, `distanceBetweenBubbles`, `balloonPadding`, `debugMode`
- `WynnBubbles.java` — added `LOGGER`, calls `WynntilsCompat.init()`
- Textures (`backgroundNormal/Party/Guild/Friends.png`) — added downward-pointing arrow at UV (18,6), 7×4 px

---

### Renderer architecture (BubbleRenderer.java)

Uses Minecraft 1.21.5+ `RenderPipeline` API directly instead of `RenderSystem` calls:

```
BUBBLE_SNIPPET  ─┬─ MAIN_BUBBLE_PIPELINE  (depthBias 3,3  — 9-patch background)
                 └─ ARROW_PIPELINE        (depthBias 0,0  — arrow tip)
```

Per-frame render loop:
1. Rotate pose to face camera (yaw only from `CameraRenderState.orientation`)
2. Translate to `playerHeight + balloonsHeightOffset + chatHeight + wynntilsOffset`
3. Scale `-0.025 * chatScale`
4. Write tint (`backgroundRed/Green/Blue/Opacity`) into `DynamicUniforms`
5. `Font.split()` for word-wrap
6. Build 9 quads (9-patch) + 1 arrow quad into a single `BufferBuilder`
7. Upload via `uploadImmediateVertexBuffer` + `getSequentialBuffer`
8. `RenderPass`: draw 9-patch with `MAIN_BUBBLE_PIPELINE`, then arrow with `ARROW_PIPELINE`
9. Reset DynamicUniforms to white/opaque
10. `Font.drawInBatch()` for text (via `CLIENT.renderBuffers().bufferSource()`)

UV layout (WynnBubbles textures, 32×32):
- Corner columns: U = 0, 7, 14 (each 6 px wide)
- Stretch row: V = 7 (1 px tall)
- Corner rows: V = 0, 9 (each 6 px tall)
- Arrow: U=18, V=6, size 7×4

---

### Config options (full list)

| Field | Default | Description |
|---|---|---|
| `chatRange` | 30.0 | Max distance (blocks) to show bubbles |
| `chatTime` | 260 | Ticks before a bubble expires |
| `maxChatWidth` | 180 | Max line width before word-wrap |
| `chatColor` | 1315860 | Text color (decimal ARGB) |
| `chatHeight` | 0.0 | Extra Y offset on top of base height |
| `chatScale` | 1.0 | Bubble size multiplier |
| `backgroundOpacity` | 0.7 | Background alpha |
| `backgroundRed/Green/Blue` | 1.0 each | Background tint (multiplies texture color) |
| `maxUUIDWordCheck` | 0 | Max words to scan for sender name (0 = unlimited) |
| `showOwnBubble` | true | Show bubble above your own head |
| `wynntilsOffset` | 0.5 | Extra Y when Wynntils is installed (tune up for WynnTitles) |
| `balloonsHeightOffset` | 0.9 | Base Y above bounding box top |
| `maxBubbles` | 5 | Max stacked bubbles per player |
| `distanceBetweenBubbles` | 3 | Pixel gap between stacked bubbles |
| `balloonPadding` | 1 | Internal bubble padding |
| `debugMode` | false | Verbose logging to `latest.log` |

---

## Build Gotchas

### Loom version must match Fabric API
`fabric-api 0.140.0+1.21.11` was built with Loom 1.13.467.  
Using Loom 1.10.x gives: `Mod was built with a newer version of Loom (1.13.467)`.  
**Fix:** `id 'fabric-loom' version '1.13-SNAPSHOT'`

### ModJavadocProcessor namespace crash
Loom 1.9+ added `ModJavadocProcessor` which validates that bundled Javadoc in mod JARs uses `"intermediary"` as the source namespace. `fabric-content-registries-v0` ships Javadoc with `"named"` namespace, causing:
```
IllegalStateException: Javadoc provided by mod (fabric-content-registries-v0) must be have an intermediary source namespace
```
**Fix:** Exclude the module from all Gradle configurations. The classes are still present at runtime inside the user's `fabric-api.jar` — this only removes the standalone JAR from Loom's build-time processing.
```groovy
configurations.all {
    exclude group: "net.fabricmc.fabric-api", module: "fabric-content-registries-v0"
}
```

### archivesBaseName removed in Gradle 8+
```
Could not set unknown property 'archivesBaseName'
```
**Fix:** `base.archivesName = project.archives_base_name`  
And in `jar {}`: `rename { "${it}_${base.archivesName.get()}" }`

### officialMojangMappings() goes in dependencies, not loom {}
```groovy
// WRONG
loom { officialMojangMappings() }

// RIGHT
dependencies { mappings loom.officialMojangMappings() }
```

### Identifier.of(String) does not exist
`Identifier` (in `net.minecraft.resources`) is the correct class.  
The one-argument `.of(String)` factory doesn't exist in 1.21.11.  
**Fix:** `Identifier.parse("namespace:path")`

---

## Debugging Missing Bubbles

Enable `debugMode = true` in the ModMenu config. All log lines are prefixed `[WynnBubbles]` and go to `logs/latest.log`.

### Stage 1 — Message received by mixin
```
[WynnBubbles] addMessage: '<raw chat text>'
```
If this never appears, the `ChatHudMixin` injection is failing (wrong method descriptor, mixin not loading).  
Check `wynnbubbles.mixins.json` and the Mixin log output at startup.

### Stage 2 — Sender name extracted
```
[WynnBubbles] senderName='PlayerName'
```
If `senderName=''`, `extractSender()` couldn't match any word to a UUID in the social manager.  
Possible causes:
- On Wynncraft: player profiles may not be in the social manager's cache yet
- Chat format has changed — the word-split regex `(§.)|[^\\w§]+` may not tokenise names correctly
- `maxUUIDWordCheck` is set too low

### Stage 3 — UUID lookup
```
[WynnBubbles] senderUUID=00000000-0000-0000-0000-000000000000
```
All-zero UUID means `getPlayerSocialManager().getDiscoveredUUID(name)` returned `NIL_UUID`.  
The player hasn't been "discovered" yet. On Wynncraft this can happen if the player joined before you did (their profile packet was missed). Chat first, check again.

### Stage 4 — Nearby players & UUID match
```
[WynnBubbles] nearby players=3
[WynnBubbles] checking player Steve uuid=...
```
If nearby count is 0, `chatRange` may be too small or the player entity isn't loaded.  
If UUIDs don't match, the UUID Wynncraft puts in chat packets differs from the entity UUID (proxy UUID mismatch).

### Stage 5 — Bubble stored
```
[WynnBubbles] bubble added to PlayerName
```
If this appears but no bubble renders, the problem is in the render pipeline.

### Stage 6 — Render called
```
[WynnBubbles] renderBubbles: 1 message(s), height=1.8
```
If Stage 5 fires but Stage 6 never does, messages aren't surviving the tick-expiry check in `PlayerEntityRendererMixin.wynnbubbles$updateRenderState` or the `AvatarRenderState` transfer is broken.

If Stage 6 fires, the GPU pipeline is running. Issues at this point are likely in the `RenderPipeline` / `DynamicUniforms` setup or shader availability.

---

## Remaining TODOs

- [ ] `ModMenuIntegration.java`: `AutoConfig.getConfigScreen()` is deprecated and marked for removal in a future Cloth Config version — migrate when the new API stabilises
- [ ] Wynntils height: currently a static configurable offset (`wynntilsOffset`). Could be made dynamic by subscribing to Wynntils' `PlayerNametagRenderEvent` — deferred because Wynntils is on a separate mod loader
- [ ] `balloonPadding = 0` edge case: positions work but look cramped; consider enforcing a minimum of 1
- [ ] Arrow rendering: arrow is always white-filled (texture color) — if `backgroundRed/Green/Blue` tint is non-white, the arrow tints to match the config, not the specific chat-type texture color. Works fine at default config
