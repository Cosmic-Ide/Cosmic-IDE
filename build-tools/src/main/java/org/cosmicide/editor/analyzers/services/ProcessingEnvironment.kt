/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package jdkx.annotation.processing

import java.util.Locale
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * An annotation processing tool framework will [ ][Processor.init] so the processor can use facilities
 * provided by the framework to write new files, report error
 * messages, and find other utilities.
 *
 *
 * Third parties may wish to provide value-add wrappers around the
 * facility objects from this interface, for example a `Filer`
 * extension that allows multiple processors to coordinate writing out
 * a single source file.  To enable this, for processors running in a
 * context where their side effects via the API could be visible to
 * each other, the tool infrastructure must provide corresponding
 * facility objects that are `.equals`, `Filer`s that are
 * `.equals`, and so on.  In addition, the tool invocation must
 * be able to be configured such that from the perspective of the
 * running annotation processors, at least the chosen subset of helper
 * classes are viewed as being loaded by the same class loader.
 * (Since the facility objects manage shared state, the implementation
 * of a wrapper class must know whether or not the same base facility
 * object has been wrapped before.)
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah
 * @since 1.6
 */
interface ProcessingEnvironment {
    /**
     * {@return the processor-specific options passed to the annotation
     * * processing tool}  Options are returned in the form of a map from
     * option name to option value.  For an option with no value, the
     * corresponding value in the map is `null`.
     *
     *
     * See documentation of the particular tool infrastructure
     * being used for details on how to pass in processor-specific
     * options.  For example, a command-line implementation may
     * distinguish processor-specific options by prefixing them with a
     * known string like `"-A"`; other tool implementations may
     * follow different conventions or provide alternative mechanisms.
     * A given implementation may also provide implementation-specific
     * ways of finding options passed to the tool in addition to the
     * processor-specific options.
     */
    val options: Map<String?, String?>?

    /**
     * {@return the messager used to report errors, warnings, and other
     * * notices}
     */
    val messager: Messager?

    /**
     * {@return the filer used to create new source, class, or auxiliary
     * * files}
     */
    val filer: Filer?

    /**
     * {@return an implementation of some utility methods for
     * * operating on elements}
     */
    val elementUtils: Elements?

    /**
     * {@return an implementation of some utility methods for
     * * operating on types}
     */
    val typeUtils: Types?

    /**
     * {@return the source version that any generated {@linkplain
     * * Filer#createSourceFile source} and {@linkplain
     * * Filer#createClassFile class} files should conform to}
     *
     * @see Processor.getSupportedSourceVersion
     */
    val sourceVersion: SourceVersion?

    /**
     * {@return the current locale or {@code null} if no locale is in
     * * effect}  The locale can be be used to provide localized
     * [messages][Messager].
     */
    val locale: Locale?
    val isPreviewEnabled: Boolean
        /**
         * Returns `true` if *preview features* are enabled
         * and `false` otherwise.
         * @return whether or not preview features are enabled
         *
         * @implSpec The default implementation of this method returns
         * `false`.
         *
         * @since 13
         */
        get() = false
}