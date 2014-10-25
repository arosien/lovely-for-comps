import java.util.regex.Pattern
import scala.language.experimental.macros
import scala.reflect.macros.{ Context, TypecheckException }

/**
 * A utility which ensures that a code fragment does not typecheck.
 *
 * Credit: Stefan Zeiger (@StefanZeiger)
 *
 * Stolen from shapeless and modified to convert compiler warnings to strings.
 */
object illTyped {
  def apply(code: String): String = macro applyImplNoExp
  def apply(code: String, expected: String): String = macro applyImpl

  def applyImplNoExp(c: Context)(code: c.Expr[String]) = applyImpl(c)(code, null)

  def applyImpl(c: Context)(code: c.Expr[String], expected: c.Expr[String]): c.Expr[String] = {
    import c.universe._

    val Expr(Literal(Constant(codeStr: String))) = code
    val (expPat, expMsg) = expected match {
      case null => (null, "Expected some error.")
      case Expr(Literal(Constant(s: String))) =>
        (Pattern.compile(s, Pattern.CASE_INSENSITIVE), "Expected error matching: "+s)
    }

    try {
      c.typeCheck(c.parse("{ "+codeStr+" }"))
      c.abort(c.enclosingPosition, "Type-checking succeeded unexpectedly.\n"+expMsg)
    } catch {
      case e: TypecheckException =>
        val msg = e.getMessage
        if ((expected ne null) && !(expPat.matcher(msg)).matches)
          c.warning(c.enclosingPosition, "Type-checking failed in an unexpected way.\n"+expMsg+"\nActual error: "+msg)
        reify(c.Expr[String](Literal(Constant(msg))).splice)
    }
  }
}