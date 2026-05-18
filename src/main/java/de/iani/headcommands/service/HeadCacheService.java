package de.iani.headcommands.service;

import de.iani.headcommands.HeadCommandsConfig;
import de.iani.headcommands.api.HeadApiClient;
import de.iani.headcommands.api.HeadApiException;
import de.iani.headcommands.model.ApiResponse;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCacheSnapshot;
import de.iani.headcommands.model.HeadCategory;
import de.iani.headcommands.storage.HeadCommandsDatabase;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class HeadCacheService {
    private final JavaPlugin plugin;
    private final ArrayDeque<WorkEntry<?>> work;
    private HeadCommandsConfig config;
    private WorkerThread worker;
    private HeadCommandsDatabase database;
    private volatile HeadCacheSnapshot snapshot;

    public HeadCacheService(JavaPlugin plugin, HeadCommandsConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.work = new ArrayDeque<>();
        this.snapshot = HeadCacheSnapshot.empty();
    }

    public synchronized void start() {
        if (worker != null) {
            return;
        }
        worker = new WorkerThread();
        worker.start();
    }

    public void shutdown() {
        WorkerThread toStop;
        synchronized (this) {
            toStop = worker;
            if (toStop == null) {
                return;
            }
            worker = null;
        }
        synchronized (work) {
            toStop.stopping = true;
            work.notifyAll();
        }
        boolean interrupted = false;
        while (toStop.isAlive()) {
            try {
                toStop.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public HeadCacheSnapshot snapshot() {
        return snapshot;
    }

    public void reconfigureAndLoad(HeadCommandsConfig newConfig, HeadCacheCallback<HeadCacheSnapshot> callback) {
        enqueue(new WorkEntry<>(callback) {
            @Override
            protected HeadCacheSnapshot execute() throws Exception {
                config = newConfig;
                reconnectDatabase();
                HeadCacheSnapshot loaded = database.loadSnapshot();
                snapshot = loaded;
                return loaded;
            }
        });
    }

    public void loadSnapshot(HeadCacheCallback<HeadCacheSnapshot> callback) {
        enqueue(new WorkEntry<>(callback) {
            @Override
            protected HeadCacheSnapshot execute() throws Exception {
                ensureDatabase();
                HeadCacheSnapshot loaded = database.loadSnapshot();
                snapshot = loaded;
                return loaded;
            }
        });
    }

    public void refresh(HeadCacheCallback<RefreshResult> callback) {
        enqueue(new WorkEntry<>(callback) {
            @Override
            protected RefreshResult execute() throws Exception {
                String validationError = config.validateForRefresh();
                if (validationError != null) {
                    throw new HeadApiException(validationError);
                }
                ensureDatabase();

                HeadApiClient client = new HeadApiClient(config);
                ApiResponse<HeadCategory> categoriesResponse = client.fetchCategories();
                List<HeadCategory> categories = categoriesResponse.data();

                List<CachedHead> heads = new ArrayList<>();
                List<String> warnings = new ArrayList<>(categoriesResponse.warnings());
                String apiVersion = categoriesResponse.meta().apiVersion();
                String license = categoriesResponse.meta().license();
                boolean dataLimited = categoriesResponse.meta().dataLimited();

                for (HeadCategory category : categories) {
                    ApiResponse<CachedHead> firstPage = client.fetchHeads(category.id(), 1);
                    if (firstPage.pagination() == null) {
                        throw new HeadApiException("Custom-heads response for category " + category.id() + " does not contain pagination.");
                    }
                    heads.addAll(firstPage.data());
                    warnings.addAll(firstPage.warnings());
                    if (firstPage.meta().apiVersion() != null) {
                        apiVersion = firstPage.meta().apiVersion();
                    }
                    license = firstPage.meta().license();
                    dataLimited = dataLimited || firstPage.meta().dataLimited();

                    int lastPage = Math.max(1, firstPage.pagination().lastPage());
                    for (int page = 2; page <= lastPage; page++) {
                        ApiResponse<CachedHead> response = client.fetchHeads(category.id(), page);
                        heads.addAll(response.data());
                        warnings.addAll(response.warnings());
                        if (response.meta().apiVersion() != null) {
                            apiVersion = response.meta().apiVersion();
                        }
                        license = response.meta().license();
                        dataLimited = dataLimited || response.meta().dataLimited();
                    }
                }

                String syncId = UUID.randomUUID().toString();
                Map<String, String> meta = new LinkedHashMap<>();
                meta.put("last_successful_sync_at", Instant.now().toString());
                meta.put("api_version", apiVersion);
                meta.put("license", license);
                meta.put("data_limited", Boolean.toString(dataLimited));
                meta.put("warnings", String.join("\n", warnings));
                meta.put("category_count", Integer.toString(categories.size()));
                meta.put("head_count", Integer.toString(heads.size()));
                database.storeSync(syncId, categories, heads, meta);
                snapshot = database.loadSnapshot();
                return new RefreshResult(categories.size(), heads.size(), syncId);
            }
        });
    }

    private void reconnectDatabase() throws SQLException {
        if (database != null) {
            database.disconnect();
            database = null;
        }
        String validationError = config.validateForDatabase();
        if (validationError != null) {
            throw new SQLException(validationError);
        }
        database = new HeadCommandsDatabase(config.sqlConfig());
    }

    private void ensureDatabase() throws SQLException {
        if (database == null) {
            reconnectDatabase();
        }
    }

    private <T> void enqueue(WorkEntry<T> entry) {
        synchronized (work) {
            work.addLast(entry);
            work.notifyAll();
        }
    }

    private abstract class WorkEntry<T> {
        private final HeadCacheCallback<T> callback;

        private WorkEntry(HeadCacheCallback<T> callback) {
            this.callback = callback;
        }

        protected abstract T execute() throws Exception;

        private void run() {
            T result = null;
            Throwable error = null;
            try {
                result = execute();
            } catch (Throwable e) {
                error = e;
                plugin.getLogger().log(Level.SEVERE, "HeadCommands worker task failed", e);
            }
            if (callback != null) {
                T finalResult = result;
                Throwable finalError = error;
                if (plugin.isEnabled()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.onComplete(finalResult, finalError));
                }
            }
        }
    }

    private class WorkerThread extends Thread {
        private boolean stopping;

        private WorkerThread() {
            super("HeadCommands-CacheWorker");
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    WorkEntry<?> entry;
                    synchronized (work) {
                        entry = work.pollFirst();
                        if (entry == null) {
                            if (stopping) {
                                return;
                            }
                            try {
                                work.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            continue;
                        }
                    }
                    entry.run();
                }
            } finally {
                if (database != null) {
                    database.disconnect();
                    database = null;
                }
            }
        }
    }
}
