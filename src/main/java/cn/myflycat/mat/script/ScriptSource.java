package cn.myflycat.mat.script;

import org.graalvm.polyglot.Source;

import java.nio.file.Path;

public record ScriptSource(String name, Path path, long mtimeMillis, Source source) {
}
