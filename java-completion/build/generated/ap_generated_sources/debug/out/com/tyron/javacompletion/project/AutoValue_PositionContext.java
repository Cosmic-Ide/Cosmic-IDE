package com.tyron.javacompletion.project;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.EndPosTable;
import com.tyron.javacompletion.model.EntityScope;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_PositionContext extends PositionContext {

  private final EntityScope scopeAtPosition;

  private final Module module;

  private final FileScope fileScope;

  private final TreePath treePath;

  private final int position;

  private final EndPosTable endPosTable;

  AutoValue_PositionContext(
      EntityScope scopeAtPosition,
      Module module,
      FileScope fileScope,
      TreePath treePath,
      int position,
      EndPosTable endPosTable) {
    if (scopeAtPosition == null) {
      throw new NullPointerException("Null scopeAtPosition");
    }
    this.scopeAtPosition = scopeAtPosition;
    if (module == null) {
      throw new NullPointerException("Null module");
    }
    this.module = module;
    if (fileScope == null) {
      throw new NullPointerException("Null fileScope");
    }
    this.fileScope = fileScope;
    if (treePath == null) {
      throw new NullPointerException("Null treePath");
    }
    this.treePath = treePath;
    this.position = position;
    if (endPosTable == null) {
      throw new NullPointerException("Null endPosTable");
    }
    this.endPosTable = endPosTable;
  }

  @Override
  public EntityScope getScopeAtPosition() {
    return scopeAtPosition;
  }

  @Override
  public Module getModule() {
    return module;
  }

  @Override
  public FileScope getFileScope() {
    return fileScope;
  }

  @Override
  public TreePath getTreePath() {
    return treePath;
  }

  @Override
  public int getPosition() {
    return position;
  }

  @Override
  public EndPosTable getEndPosTable() {
    return endPosTable;
  }

  @Override
  public String toString() {
    return "PositionContext{"
        + "scopeAtPosition=" + scopeAtPosition + ", "
        + "module=" + module + ", "
        + "fileScope=" + fileScope + ", "
        + "treePath=" + treePath + ", "
        + "position=" + position + ", "
        + "endPosTable=" + endPosTable
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof PositionContext) {
      PositionContext that = (PositionContext) o;
      return this.scopeAtPosition.equals(that.getScopeAtPosition())
          && this.module.equals(that.getModule())
          && this.fileScope.equals(that.getFileScope())
          && this.treePath.equals(that.getTreePath())
          && this.position == that.getPosition()
          && this.endPosTable.equals(that.getEndPosTable());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= scopeAtPosition.hashCode();
    h$ *= 1000003;
    h$ ^= module.hashCode();
    h$ *= 1000003;
    h$ ^= fileScope.hashCode();
    h$ *= 1000003;
    h$ ^= treePath.hashCode();
    h$ *= 1000003;
    h$ ^= position;
    h$ *= 1000003;
    h$ ^= endPosTable.hashCode();
    return h$;
  }

}
