package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*

/**
 * Marks the annotated class, function, property, variable or parameter as deprecated.
 * @property message the message explaining the deprecation and recommending an alternative API to use.
 * @property replaceWith if present, specifies a code fragment which should be used as a replacement for
 *     the deprecated API usage.
 */
@Target(CLASS, FUNCTION, PROPERTY, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_SETTER, PROPERTY_GETTER, TYPEALIAS)
@MustBeDocumented
public annotation class Deprecated(
        val message: String,
        val replaceWith: ReplaceWith = ReplaceWith(""),
        val level: DeprecationLevel = DeprecationLevel.WARNING
)

/**
 * Specifies a code fragment that can be used to replace a deprecated function, property or class. Tools such
 * as IDEs can automatically apply the replacements specified through this annotation.
 *
 * @property expression the replacement expression. The replacement expression is interpreted in the context
 *     of the symbol being used, and can reference members of enclosing classes etc.
 *     For function calls, the replacement expression may contain argument names of the deprecated function,
 *     which will be substituted with actual parameters used in the call being updated. The imports used in the file
 *     containing the deprecated function or property are NOT accessible; if the replacement expression refers
 *     on any of those imports, they need to be specified explicitly in the [imports] parameter.
 * @property imports the qualified names that need to be imported in order for the references in the
 *     replacement expression to be resolved correctly.
 */
@Target()
@Retention(BINARY)
@MustBeDocumented
public annotation class ReplaceWith(val expression: String, vararg val imports: String)

/**
 * Contains levels for deprecation levels.
 */
public enum class DeprecationLevel {
    /** Usage of the deprecated element will be marked as a warning. */
    WARNING,
    /** Usage of the deprecated element will be marked as an error. */
    ERROR,
    /** Deprecated element will not be accessible from code. */
    HIDDEN
}

/**
 * Suppresses errors about variance conflict
 */
@Target(TYPE)
@Retention(SOURCE)
@MustBeDocumented
public annotation class UnsafeVariance


/**
 * Specifies that this part of internal API is effectively public exposed by using in public inline function
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("1.1")
public annotation class PublishedApi