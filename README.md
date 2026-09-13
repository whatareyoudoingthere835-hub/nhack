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

Готовый jar: `build/libs/nhack-1.0.0.jar`

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

**Xray** (SkyEgames → Testing): порт `Xray` из Meteor Client. Всё, кроме руды, просто перестаёт рисоваться — мир
исчезает, и в пустоте висят **только `diamond_ore` и `deepslate_diamond_ore`**. `exposed-only` зашит в код и всегда
включён — руда, замурованная в камне, не показывается, видна только та, что уже открыта (пещера, воздух, вода).
Вайтлист тоже залочен, настройки блоков нет. Вместе с блоками прячутся жидкости и block entity (сундуки, печи,
кровати...), отключаются chunk occlusion и ambient occlusion. Настроек нет — только Bind; чанки пересобираются
при вкл/выкл.

Meteor гасит мир полупрозрачностью (`opacity`), nhack вместо этого целиком пропускает геометрию заблокированных
блоков (у Meteor это ветка `alpha == 0`). Так модуль ведёт себя одинаково с Sodium и без него: полупрозрачность
пришлось бы писать в собственный буфер квадов Sodium, а это зависимость от Sodium на этапе компиляции. Заодно
дешевле — не нужно сортировать тысячи ghost-квадов в translucent-слое.

Работает и с Sodium: ванильный пайплайн чанков он заменяет целиком, поэтому те же решения продублированы в
`mixin/sodium` (модели блоков, отсечение граней, жидкости). Chunk occlusion и block entity ловятся ванильными
хуками — Sodium их всё равно вызывает. Конфиг `nhack.sodium.mixins.json` помечен `@Pseudo` и `required: false`,
так что без Sodium он просто не применяется.

Конфиг пишется в `.minecraft/config/nhack/client.json`.

## Как добавить модуль

1. Скопируй `src/client/java/dev/nhack/client/module/modules/misc/ExampleModule.java`
2. Поставь категорию (`COMBAT`, `MOVEMENT`, `RENDER`, `PLAYER`, `MISC`, `CLIENT`, `SKYGAMES`).
   У `SKYGAMES` есть подвкладки — добавь четвёртым аргументом `SubCategory`
   (`COMBAT`, `MOVEMENT`, `MISC`, `TESTING`, `DETECTED`):

```java
super("Xray", "What it does", Category.SKYGAMES, SubCategory.TESTING);
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
  mixin/            хуки: тик, свет, input, пакеты и xray-рендер
  mixin/sodium/     те же xray-хуки для Sodium (применяются только с ним)
  util/             чат, цвет, клавиши
```

## Версии

Актуальные номера — на [fabricmc.net/develop](https://fabricmc.net/develop). Меняй их в `gradle.properties`.
