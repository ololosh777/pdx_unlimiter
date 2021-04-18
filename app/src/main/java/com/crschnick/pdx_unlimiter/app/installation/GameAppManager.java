package com.crschnick.pdx_unlimiter.app.installation;

import com.crschnick.pdx_unlimiter.app.core.SavegameManagerState;
import com.crschnick.pdx_unlimiter.app.core.settings.Settings;
import com.crschnick.pdx_unlimiter.app.savegame.SavegameActions;
import com.crschnick.pdx_unlimiter.app.util.ThreadHelper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class GameAppManager {

    private static final Logger logger = LoggerFactory.getLogger(GameAppManager.class);
    private static final GameAppManager INSTANCE = new GameAppManager();
    private final ObjectProperty<GameApp> activeGame = new SimpleObjectProperty<>(null);
    private Instant lastImport;

    private GameAppManager() {
    }

    public static void init() {
        ThreadHelper.create("game watcher", true, () -> {
            while (true) {
                INSTANCE.update();
                ThreadHelper.sleep(20);
            }
        }).start();
    }

    private static boolean isInstanceOfGame(String cmd, Game game) {
        var install = GameInstallation.ALL.get(game);
        return install != null && cmd.contains(install.getExecutable().toString());
    }

    public static GameAppManager getInstance() {
        return INSTANCE;
    }

    private void update() {
        if ((activeGame.get() == null)) {
            var process = Arrays.stream(Game.values())
                    .map(g -> new GameApp(ProcessHandle.allProcesses()
                            .filter(p -> p.info().command()
                                    .map(cs -> isInstanceOfGame(cs, g)).orElse(false))
                            .findAny().orElse(null), g)
                    )
                    .filter(ga -> ga.getProcess() != null)
                    .findAny();
            if (process.isPresent()) {
                activeGame.set(process.get());
                activeGame.get().onStart();
                SavegameManagerState.get().selectGameAsync(process.get().getGame());
            }
        } else {
            updateImportTimer();

            if (!activeGame.get().isAlive()) {
                activeGame.get().onShutdown();
                activeGame.set(null);
            }
        }
    }

    private void updateImportTimer() {
        if (!Settings.getInstance().enabledTimedImports.getValue()) {
            return;
        }

        if (lastImport == null) {
            lastImport = Instant.now();
        }

        if (Duration.between(lastImport, Instant.now()).compareTo(
                Duration.of(Settings.getInstance().timedImportsInterval.getValue(), ChronoUnit.MINUTES)) > 0) {
            logger.info("Importing latest savegame because timed imports is enabled");
            if (Settings.getInstance().playSoundOnBackgroundImport.getValue()) {
                Toolkit.getDefaultToolkit().beep();
            }
            importLatest();
            lastImport = Instant.now();
        }
    }

    public void importLatest() {
        var g = getActiveGame();
        if (g != null) {
            logger.info("Importing latest savegame");
            SavegameActions.importLatestSavegame(g.getGame());
        }
    }

    public void kill() {
        var g = getActiveGame();
        if (g != null) {
            logger.info("Killing active game");
            g.kill();
        }
    }

    public GameApp getActiveGame() {
        return activeGame.get();
    }

    public ObjectProperty<GameApp> activeGameProperty() {
        return activeGame;
    }
}
