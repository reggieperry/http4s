package org.http4s.rho
package bits

import shapeless.ops.hlist.Prepend
import shapeless.{::, HNil, HList}

import scala.reflect.Manifest

/**
 * Created by Bryce Anderson on 6/29/14.
 */

/** Actual elements which build up the AST */

object PathAST {

  /** The root type of the parser AST */
  private[rho] sealed trait PathRule[T <: HList] {
    def documentation: Option[String] = None
  }

  sealed trait CombinablePathRule[T <: HList] extends PathRule[T] {
    /** These methods differ in their return type */
    def and[T2 <: HList](p2: CombinablePathRule[T2])(implicit prep: Prepend[T2, T]): CombinablePathRule[prep.Out] =
      PathAnd(this, p2)

    def &&[T2 <: HList](p2: CombinablePathRule[T2])(implicit prep: Prepend[T2, T]): CombinablePathRule[prep.Out] = and(p2)

    def or(p2: CombinablePathRule[T]): CombinablePathRule[T] = PathOr(this, p2)

    def ||(p2: CombinablePathRule[T]): CombinablePathRule[T] = or(p2)

    def /(s: String): CombinablePathRule[T] = PathAnd(this, PathMatch(s))

    def /(s: Symbol): CombinablePathRule[String :: T] = PathAnd(this, PathCapture(StringParser.strParser))

    def /[T2 <: HList](t: CombinablePathRule[T2])(implicit prep: Prepend[T2, T]): CombinablePathRule[prep.Out] =
      PathAnd(this, t)
  }

  private[rho] case class PathAnd[T <: HList](p1: PathRule[_ <: HList], p2: PathRule[_ <: HList]) extends CombinablePathRule[T]

  private[rho] case class PathOr[T <: HList](p1: PathRule[T], p2: PathRule[T]) extends CombinablePathRule[T]

  private[rho] case class PathMatch(s: String, override val documentation: Option[String] = None) extends CombinablePathRule[HNil]

  private[rho] case class PathCapture[T](parser: StringParser[T],
                                         override val documentation: Option[String] = None)
                                        (implicit val m: Manifest[T]) extends CombinablePathRule[T :: HNil]

  // These don't fit the  operations of CombinablePathSyntax because they may
  // result in a change of the type of PathBulder
  // TODO: can I make this a case object?
  case class CaptureTail(override val documentation: Option[String] = None) extends PathRule[List[String] :: HNil]

  private[rho] case object PathEmpty extends PathRule[HNil]

  trait MetaData extends PathRule[HNil]

  case class PathDescription(desc: String) extends MetaData

}
