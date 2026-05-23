package cn.myflycat.mat.client.api;

import cn.myflycat.mat.script.ScriptHandle;
import org.graalvm.polyglot.HostAccess;

public final class ApiRoot {
    @HostAccess.Export public final ChatBindings chat;
    @HostAccess.Export public final PlayerBindings player;
    @HostAccess.Export public final ControlBindings control;

    public ApiRoot(ScriptHandle handle) {
        this.chat = new ChatBindings();
        this.player = new PlayerBindings();
        this.control = new ControlBindings(handle);
    }

    @HostAccess.Export
    public void sleep(long millis) throws InterruptedException {
        control.sleep(millis);
    }

    @HostAccess.Export
    public boolean cancelled() {
        return control.cancelled();
    }
}
