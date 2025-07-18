//> using options -Wunused:implicits

/* This goes around the "trivial method" detection */
val default_int = 1

object Xd {
  private def f1(a: Int) = a // OK
  private def f2(a: Int) = 1 // OK
  private def f3(a: Int)(using Int) = a // warn
  private def f4(a: Int)(using Int) = default_int // warn
  private def f6(a: Int)(using Int) = summon[Int] // OK
  private def f7(a: Int)(using Int) = summon[Int] + a // OK
  private def f8(a: Int)(using foo: Int) = a // warn
  private def f9(a: Int)(using Int) = ??? // OK trivial
  private def g1(a: Int)(implicit foo: Int) = a // warn
}

trait T
object T:
  def hole(using T) = ()

class C(using T) // no warn marker trait is evidence only

class D(using T):
  def t = T.hole // nowarn

object Example:
  import scala.quoted.*
  given OptionFromExpr[T](using Type[T], FromExpr[T]): FromExpr[Option[T]] with
    def unapply(x: Expr[Option[T]])(using Quotes) = x match
      case '{ Option[T](${Expr(y)}) } => Some(Option(y))
      case '{ None } => Some(None)
      case '{ ${Expr(opt)} : Some[T] } => Some(opt)
      case _ => None

object ExampleWithoutWith:
  import scala.quoted.*
  given [T] => (Type[T], FromExpr[T]) => FromExpr[Option[T]]:
    def unapply(x: Expr[Option[T]])(using Quotes) = x match
      case '{ Option[T](${Expr(y)}) } => Some(Option(y))
      case '{ None } => Some(None)
      case '{ ${Expr(opt)} : Some[T] } => Some(opt)
      case _ => None

//nowarning names on matches of quote trees requires consulting non-abstract types in QuotesImpl
object Unmatched:
  import scala.quoted.*
  def transform[T](e: Expr[T])(using Quotes): Expr[T] =
    import quotes.reflect.*
    def f(tree: Tree) =
      tree match
      case Ident(name) =>
      case _ =>
    e

trait Ctx:
  val state: Int
case class K(i: Int)(using val ctx: Ctx) // nowarn
class L(val i: Int)(using val ctx: Ctx) // nowarn
class M(val i: Int)(using ctx: Ctx) // warn

package givens:

  trait X:
    def doX: Int

  trait Y:
    def doY: String

  trait Z

  given X:
    def doX = 7

  given X => Y: // warn protected param to given class
    def doY = "7"
  /* desugared. It is protected so that its type can be used in member defs without leaking.
   * possibly it should be protected only for named parameters.
      given class given_Y(using x$1: givens.X) extends Object(), givens.Y {
        protected given val x$1: givens.X
        def doY: String = "7"
      }
      final given def given_Y(using x$1: givens.X): givens.given_Y =
        new givens.given_Y(using x$1)()
  */

  given namely: (x: X) => Y: // warn protected param to given class
    def doY = "8"

  def f(using => X) = println() // warn
  def g(using => Z) = println() // nowarn marker trait
end givens

object i22895:
  trait Test[F[_], Ev] {
    def apply[A, B](fa: F[A])(f: A => B)(using ev: Ev): F[B]
  }
  given testId: Test[[a] =>> a, Unit] =
    new Test[[a] =>> a, Unit] {
      def apply[A, B](fa: A)(f: A => B)(using ev: Unit): B = f(fa) // nowarn override
    }
  class C:
    def f(using s: String) = s.toInt
  class D(i: Int) extends C:
    override def f(using String) = compute(i) // nowarn override
    def g(using sss: String) = compute(i) // warn
    def compute(i: Int) = i * 42 // returning a class param is deemed trivial, make it non-trivial
