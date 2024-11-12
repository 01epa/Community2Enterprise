package com.epa;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@Service
@State(
        name = "PluginInstallState",
        storages = @Storage("pluginState.xml")
)
public final  class PluginInstallState implements PersistentStateComponent<PluginInstallState.State> {

    private State state = new State();

    public static class State {
        public String version = "";
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public String getVersion() {
        return state.version;
    }

    public void setVersion(String version) {
        state.version = version;
    }
}
