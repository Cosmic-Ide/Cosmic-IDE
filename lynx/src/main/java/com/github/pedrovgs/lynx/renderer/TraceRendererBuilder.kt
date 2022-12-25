/*
 * Copyright (C) 2015 Pedro Vicente Gomez Sanchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pedrovgs.lynx.renderer

import com.github.pedrovgs.lynx.LynxConfig
import com.github.pedrovgs.lynx.model.Trace
import com.github.pedrovgs.lynx.model.TraceLevel
import com.pedrogomez.renderers.RendererBuilder
import java.util.LinkedList

/**
 * Renderer builder implementation created to return `Renderer<Trace>` instances based on the
 * trace level. This builder will use six different `Renderer<Trace>` implementations, one for
 * each TraceLevel type.
 *
 *
 * To learn more about Renderers library take a look to the repository:
 * https://github.com/pedrovgs/Renderers
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class TraceRendererBuilder(lynxConfig: LynxConfig?) : RendererBuilder<Trace>() {
    init {
        val prototypes: MutableList<TraceRenderer> = LinkedList()
        prototypes.add(TraceRenderer(lynxConfig!!))
        prototypes.add(AssertTraceRenderer(lynxConfig))
        prototypes.add(DebugTraceRenderer(lynxConfig))
        prototypes.add(InfoTraceRenderer(lynxConfig))
        prototypes.add(WarningTraceRenderer(lynxConfig))
        prototypes.add(ErrorTraceRenderer(lynxConfig))
        prototypes.add(WtfTraceRenderer(lynxConfig))
        setPrototypes(prototypes)
    }

    override fun getPrototypeClass(content: Trace): Class<*> {
        val rendererClass: Class<*>?
        val traceLevel = content.level
        rendererClass = when (traceLevel) {
            TraceLevel.ASSERT -> AssertTraceRenderer::class.java
            TraceLevel.DEBUG -> DebugTraceRenderer::class.java
            TraceLevel.INFO -> InfoTraceRenderer::class.java
            TraceLevel.WARNING -> WarningTraceRenderer::class.java
            TraceLevel.ERROR -> ErrorTraceRenderer::class.java
            TraceLevel.WTF -> WtfTraceRenderer::class.java
            else -> TraceRenderer::class.java
        }
        return rendererClass
    }
}