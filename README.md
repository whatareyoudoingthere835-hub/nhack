# nhack

Клиентский шаблон под **Minecraft 1.21.11 / Fabric**.

Не готовый комбат-клиент, а каркас: модули, настройки, ивент-шина, команды, конфиг и ClickGUI. Новые фичи добавляются одним классом.

|              |                          |
|--------------|--------------------------|
| Minecraft    | 1.21.11                  |
| Loader       | 0.19.3                   |
| Fabric API   | 0.141.6+1.21.11          |
| Loom         | 1.17 (`fabric-loom-remap`) |
| Mappings     | Official Mojang          |
| Java         | 21                       |

## Сборка

Нужны JDK 21+ и интернет (Loom качает игру и маппинги).

```bash
./gradlew build
```

Готовый jar: `build/libs/nhack-<version>.jar` (сейчас `nhack-1.0.3.jar`)

Положи его в `.minecraft/mods` вместе с [Fabric API](https://modrinth.com/mod/fabric-api) для 1.21.11.

Запуск клиента из IDE:

```bash
./gradlew runClient
```

IntelliJ: `./gradlew idea` или просто Open как Gradle-проект, затем конфигурация `Minecraft Client`.

## Управление

- Кастомное главное меню (звёздное небо). «Back to default menu» возвращает ванильный Title Screen.
- **Right Shift** — одно окно ClickGUI (стекло + блюр мира)
- вкладки слева выбирают категорию, у **SkyEgames** раскрываются подвкладки (Combat / Movement / Misc / Testing / !Detected!)
- ЛКМ по модулю — открыть настройки справа, тумблер — вкл/выкл
- ПКМ по модулю — сразу тоггл
- СКМ / строка Bind — назначить клавишу, `Esc`/`Delete` — снять
- Команды в чате с префиксом `.`

```
.help
.toggle Aura
.bind Fullbright G
.friend add Steve
.prefix ,
```

**Aura** (Combat): range, walls, FOV, Track/Interact/None rotations, auto-weapon, smart crit, shield-breaker, target filters. `.friend` исключает игроков.

**Test** (Combat) — обучаемая киллаура. Зажми **Record**-банд: появляется фейк-игрок, на которого ты наводишься, и модуль записывает твои микродвижения мыши. Потом он повторяет их на реальных целях с той же скоростью — поворот по кривой, а не по прямой. Настройки: `Range` (2.9–4 блока), `Rotation` (Curved — запись / Straight — прямая), `MissChance` (0–99%, при промахе просто не бьёт), `Silent` (только тело / тело+камера), `Speed`, `RotationSpeed`, `AttackDelay`, `Targets`.

**Timer** (Movement): Normal / Matrix / Shift / Grim, OnFlag, auto-disable.

**NoSlow** (Movement): NCP, StrictNCP, Matrix, Grim, GrimNew, MusteryGrief, LFCraft, Matrix2/3 + food/shield/blocks.

**ESP** (Render): 2D-боксы, HP, TNT, перлы, burrow, маяки, lingering clouds.

**CaveXRay** (Render, класс `ExposedDiamonds`): 2D-подсветка **открытой** алмазной руды — `diamond_ore` и
`deepslate_diamond_ore`, у которых хотя бы один сосед воздух, жидкость или несплошной блок (то есть руда уже видна
из пещеры; замурованная в камне не показывается). Чанки вокруг игрока сканируются по одному за тик в радиусе
`Radius` (1–6 чанков), потолок поиска — `MaxY` (по умолчанию 16, дно берётся из высоты мира, так что в Незере и
Энде лишнее не перебирается). На экране — квадратик, подпись `DIAMOND` и дистанция в метрах. Мир не
перестраивается и блоки не прячутся, поэтому с Sodium и без него модуль работает одинаково. Настроек три:
`Radius`, `MaxY`, `DiamondColor`.

> Старый `Xray` (SkyEgames → Testing, порт из Meteor Client, прятал весь мир кроме руды) вырезан из-за багов.
> Падал он так: миксин в `ModelBlockRenderer` целился в `tesselate(BlockAndTintGetter, List, BlockState, BlockPos, ...)`,
> которого в 1.21.11 больше нет — метод переименован в `tesselateBlock(...)`. Отсюда `InjectionError ... Scanned 0 target(s)`
> при старте игры и костыль с `defaultRequire: 0`. Категория `SKYGAMES` с подвкладками осталась: если модуль
> понадобится снова, его хуки надо писать под имена 1.21.11 (`tesselateBlock`, `Block.shouldRenderFace`,
> `LiquidBlockRenderer.tesselate`, `SectionCompiler`, `VisGraph`, `Minecraft.useAmbientOcclusion`).

Конфиг пишется в `.minecraft/config/nhack/client.json`.

## Как добавить модуль

1. Скопируй `src/client/java/dev/nhack/client/module/modules/misc/ExampleModule.java`
2. Поставь категорию (`COMBAT`, `MOVEMENT`, `RENDER`, `PLAYER`, `MISC`, `CLIENT`, `SKYGAMES`).
   У `SKYGAMES` есть подвкладки — добавь четвёртым аргументом `SubCategory`
   (`COMBAT`, `MOVEMENT`, `MISC`, `TESTING`, `DETECTED`):

```java
super("MyModule", "What it does", Category.SKYGAMES, SubCategory.TESTING);
```

   Без `SubCategory` модуль показывается во всех подвкладках своей категории.
3. Добавь настройки через `addSetting(...)`; если на изменение надо реагировать — `setting.onChanged(() -> ...)`
4. Пиши логику в `onEnable` / `onDisable` / `onTick` / `onRenderHud`
5. Зарегистрируй модуль в `ModuleManager.init()`

```java
public final class MyModule extends Module {
    private final BoolSetting example = addSetting(new BoolSetting("Example", "Demo toggle", true));

    public MyModule() {
        super("MyModule", "What it does", Category.MISC);
    }

    @Override
    public void onTick(TickEvent.Post event) {
        if (!example.get()) return;
        // your logic
    }
}
```

Для хуков, которых нет в Fabric API, клади mixin в `src/client/java/dev/nhack/client/mixin` и дописывай его в `src/client/resources/nhack.client.mixins.json`.

`injectors.defaultRequire` держим **1**. Если таргет не найден (обычно после обновления MC метод переименовали),
игра падает на старте с `InjectionError: Critical injection failure ... Scanned 0 target(s)`, и в логе сразу видно,
какой миксин и какой метод потерялся. `0` — не починка, а маскировка: с ним ничего не падает, но сломанный хук
тихо перестаёт работать (модуль «включён», а эффекта нет, и найти это в разы сложнее).

Minecraft 1.21.11 распространяется **без обфускации**, поэтому Loom собирает jar со статически переименованными
миксинами и без refmap: строка `No refMap loaded.` в отчёте об ошибке — норма, а не причина падения. Имена таргетов
в миксинах — обычные Mojang-имена, их можно сверять с декомпилированными исходниками версии.

Ивент-шина:

```java
@Subscribe
public void onPreTick(TickEvent.Pre event) { }
```

`EventBus.register(this)` вызывается автоматически при включении модуля.

## Структура

```
src/main            общие ресурсы, fabric.mod.json
src/client
  event/            EventBus + Tick / HUD / Key
  module/           Module, Category, SubCategory, ModuleManager
  setting/          Bool / Number / Mode
  command/          .toggle .bind .help .prefix
  config/           JSON save/load
  gui/              ClickGUI (вкладки категорий + подвкладки SkyEgames)
  mixin/            хуки: тик, свет (Fullbright), input, пакеты, таймер, NoSlow
  mixin/sodium/     зарезервировано под Sodium-хуки (папки пока нет, nhack.sodium.mixins.json пустой и required: false)
  util/             чат, цвет, клавиши
```

## Версии

Актуальные номера — на [fabricmc.net/develop](https://fabricmc.net/develop). Меняй их в `gradle.properties`.
