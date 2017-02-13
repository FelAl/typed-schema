package ru.tinkoff.tschema.named

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server._
import Directives._
import ru.tinkoff.tschema.serve.ServableSingle
import ru.tinkoff.tschema.typeDSL._
import shapeless.ops.hlist._
import shapeless.ops.record.Values
import shapeless.{HList, HNil}

trait ServeSingle[T, In <: HList, Out] extends ServePartial[T, In, Out] {

  def handle(f: (In) ⇒ Route): Route
}

object ServeSingle {
  def apply[T] = new MkServe[T]

  def make[T](x: T) = new MkServe[T]

  class MkServe[T] {
    def apply[I <: HList, SI <: HList, O](servable: ServableSingle[SI, O])
                                (implicit
                                 serve: ServeSingle[T, I, O],
                                 values: Values.Aux[I, SI]): Route = serve(servable)
  }

  implicit def servePost[x]
  (implicit marshaller: ToResponseMarshaller[x]) = new ServeSingle[Post[x], HNil, x] {
    def handle(f: (HNil) => Route): Route = post(f(HNil))
  }

  implicit def serveGet[x]
  (implicit marshaller: ToResponseMarshaller[x]) = new ServeSingle[Get[x], HNil, x] {
    def handle(f: (HNil) => Route): Route = get(f(HNil))
  }

  implicit def serveCons[start, end, startInput <: HList, endInput <: HList, endOut]
  (implicit
   start: ServePrefix[start, startInput],
   end: ServeSingle[end, endInput, endOut],
   prepend: Prepend[startInput, endInput]) = new ServeSingle[start :> end, prepend.Out, endOut] {
    def handle(f: (prepend.Out) => Route): Route = start.handle(startIn ⇒ end.handle { endIn ⇒ f(prepend(startIn, endIn)) })
  }
}