package com.tyron.javacompletion.typesolver;

import com.google.common.collect.ImmutableSet;
import com.tyron.javacompletion.model.Entity;
import com.tyron.javacompletion.model.EntityScope;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.model.SolvedTypeParameters;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ExpressionSolver_ExpressionDefinitionScannerParams extends ExpressionSolver.ExpressionDefinitionScannerParams {

  private final EntityScope baseScope;

  private final Module module;

  private final ImmutableSet<Entity.Kind> allowedEntityKinds;

  private final int position;

  private final SolvedTypeParameters contextTypeParameters;

  private AutoValue_ExpressionSolver_ExpressionDefinitionScannerParams(
      EntityScope baseScope,
      Module module,
      ImmutableSet<Entity.Kind> allowedEntityKinds,
      int position,
      SolvedTypeParameters contextTypeParameters) {
    this.baseScope = baseScope;
    this.module = module;
    this.allowedEntityKinds = allowedEntityKinds;
    this.position = position;
    this.contextTypeParameters = contextTypeParameters;
  }

  @Override
  EntityScope baseScope() {
    return baseScope;
  }

  @Override
  Module module() {
    return module;
  }

  @Override
  ImmutableSet<Entity.Kind> allowedEntityKinds() {
    return allowedEntityKinds;
  }

  @Override
  int position() {
    return position;
  }

  @Override
  SolvedTypeParameters contextTypeParameters() {
    return contextTypeParameters;
  }

  @Override
  public String toString() {
    return "ExpressionDefinitionScannerParams{"
        + "baseScope=" + baseScope + ", "
        + "module=" + module + ", "
        + "allowedEntityKinds=" + allowedEntityKinds + ", "
        + "position=" + position + ", "
        + "contextTypeParameters=" + contextTypeParameters
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ExpressionSolver.ExpressionDefinitionScannerParams) {
      ExpressionSolver.ExpressionDefinitionScannerParams that = (ExpressionSolver.ExpressionDefinitionScannerParams) o;
      return this.baseScope.equals(that.baseScope())
          && this.module.equals(that.module())
          && this.allowedEntityKinds.equals(that.allowedEntityKinds())
          && this.position == that.position()
          && this.contextTypeParameters.equals(that.contextTypeParameters());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= baseScope.hashCode();
    h$ *= 1000003;
    h$ ^= module.hashCode();
    h$ *= 1000003;
    h$ ^= allowedEntityKinds.hashCode();
    h$ *= 1000003;
    h$ ^= position;
    h$ *= 1000003;
    h$ ^= contextTypeParameters.hashCode();
    return h$;
  }

  @Override
  ExpressionSolver.ExpressionDefinitionScannerParams.Builder toBuilder() {
    return new Builder(this);
  }

  static final class Builder extends ExpressionSolver.ExpressionDefinitionScannerParams.Builder {
    private EntityScope baseScope;
    private Module module;
    private ImmutableSet<Entity.Kind> allowedEntityKinds;
    private int position;
    private SolvedTypeParameters contextTypeParameters;
    private byte set$0;
    Builder() {
    }
    private Builder(ExpressionSolver.ExpressionDefinitionScannerParams source) {
      this.baseScope = source.baseScope();
      this.module = source.module();
      this.allowedEntityKinds = source.allowedEntityKinds();
      this.position = source.position();
      this.contextTypeParameters = source.contextTypeParameters();
      set$0 = (byte) 1;
    }
    @Override
    ExpressionSolver.ExpressionDefinitionScannerParams.Builder baseScope(EntityScope baseScope) {
      if (baseScope == null) {
        throw new NullPointerException("Null baseScope");
      }
      this.baseScope = baseScope;
      return this;
    }
    @Override
    ExpressionSolver.ExpressionDefinitionScannerParams.Builder module(Module module) {
      if (module == null) {
        throw new NullPointerException("Null module");
      }
      this.module = module;
      return this;
    }
    @Override
    ExpressionSolver.ExpressionDefinitionScannerParams.Builder allowedEntityKinds(ImmutableSet<Entity.Kind> allowedEntityKinds) {
      if (allowedEntityKinds == null) {
        throw new NullPointerException("Null allowedEntityKinds");
      }
      this.allowedEntityKinds = allowedEntityKinds;
      return this;
    }
    @Override
    ExpressionSolver.ExpressionDefinitionScannerParams.Builder position(int position) {
      this.position = position;
      set$0 |= 1;
      return this;
    }
    @Override
    ExpressionSolver.ExpressionDefinitionScannerParams.Builder contextTypeParameters(SolvedTypeParameters contextTypeParameters) {
      if (contextTypeParameters == null) {
        throw new NullPointerException("Null contextTypeParameters");
      }
      this.contextTypeParameters = contextTypeParameters;
      return this;
    }
    @Override
    ExpressionSolver.ExpressionDefinitionScannerParams build() {
      if (set$0 != 1
          || this.baseScope == null
          || this.module == null
          || this.allowedEntityKinds == null
          || this.contextTypeParameters == null) {
        StringBuilder missing = new StringBuilder();
        if (this.baseScope == null) {
          missing.append(" baseScope");
        }
        if (this.module == null) {
          missing.append(" module");
        }
        if (this.allowedEntityKinds == null) {
          missing.append(" allowedEntityKinds");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" position");
        }
        if (this.contextTypeParameters == null) {
          missing.append(" contextTypeParameters");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ExpressionSolver_ExpressionDefinitionScannerParams(
          this.baseScope,
          this.module,
          this.allowedEntityKinds,
          this.position,
          this.contextTypeParameters);
    }
  }

}
