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

- **Right Shift** — ClickGUI (также в Options → Controls → nhack)
- ЛКМ по модулю — вкл/выкл
- ПКМ по модулю — настройки
- СКМ / строка Bind — назначить клавишу, `Esc`/`Delete` — снять
- Команды в чате с префиксом `.`

```
.help
.toggle Sprint
.bind Fullbright G
.prefix ,
```

Конфиг пишется в `.minecraft/config/nhack/client.json`.

## Как добавить модуль

1. Скопируй `src/client/java/dev/nhack/client/module/modules/misc/ExampleModule.java`
2. Поставь категорию (`COMBAT`, `MOVEMENT`, `RENDER`, `PLAYER`, `MISC`, `CLIENT`)
3. Добавь настройки через `addSetting(...)`
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
  module/           Module, Category, ModuleManager
  setting/          Bool / Number / Mode
  command/          .toggle .bind .help .prefix
  config/           JSON save/load
  gui/              ClickGUI
  mixin/            пример хука в Minecraft.tick
  util/             чат, цвет, клавиши
```

## Версии

Актуальные номера — на [fabricmc.net/develop](https://fabricmc.net/develop). Меняй их в `gradle.properties`.
