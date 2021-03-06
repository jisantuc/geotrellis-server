package geotrellis.server

import cats.implicits._
import geotrellis.vector.{io => _, _}
import io.circe._
import io.circe.parser.{decode, parse}
import io.circe.shapes.CoproductInstances
import shapeless._

import java.text.ParseException
import java.time.Instant

package object stac {
  type TwoDimBbox = Double :: Double :: Double :: Double :: HNil

  object TwoDimBbox {
    def apply(xmin: Double, ymin: Double, xmax: Double, ymax: Double): TwoDimBbox =
      xmin :: ymin :: xmax :: ymax :: HNil
  }

  type ThreeDimBbox = Double :: Double :: Double :: Double :: Double :: Double :: HNil

  object ThreeDimBbox {
    def apply(xmin: Double, ymin: Double, zmin: Double, xmax: Double, ymax: Double, zmax: Double): ThreeDimBbox =
      xmin :: ymin :: zmin :: xmax :: ymax :: zmax :: HNil
  }

  type Bbox = TwoDimBbox :+: ThreeDimBbox :+: CNil

  object Implicits extends CoproductInstances {

      // Stolen straight from circe docs
      implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emap { str =>
        Either.catchNonFatal(Instant.parse(str.stripMargin('"'))).leftMap(t => "Instant")
      }

      implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)


      implicit val geometryDecoder: Decoder[Geometry] = Decoder[Json] map { js =>
        js.spaces4.parseGeoJson[Geometry]
      }

      implicit val geometryEncoder: Encoder[Geometry] = new Encoder[Geometry] {
        def apply(geom: Geometry) = {
          parse(geom.toGeoJson) match {
            case Right(js) => js
            case Left(e)   => throw e
          }
        }
      }

      implicit val enc2DBbox: Encoder[TwoDimBbox] = new Encoder[TwoDimBbox] {
        def apply(box: TwoDimBbox) = Encoder[List[Double]].apply(box.toList)
      }

      implicit val enc3DBbox: Encoder[ThreeDimBbox] = new Encoder[ThreeDimBbox] {
        def apply(box: ThreeDimBbox) = Encoder[List[Double]].apply(box.toList)
      }

      // These `new Exception(message)` underlying exceptions aren't super helpful, but the wrapping
      // ParsingFailure should be sufficient to match on for any client that needs to do so
      implicit val dec2DBbox: Decoder[TwoDimBbox] = Decoder[List[Double]] map {
        case nums if nums.length == 4 =>
          nums(0) :: nums(1) :: nums(2) :: nums(3) :: HNil
        case nums if nums.length > 4 =>
          val message = s"Too many values for 2D bbox: $nums"
          throw new ParsingFailure(message, new Exception(message))
        case nums if nums.length < 3 =>
          val message = s"Too few values for 2D bbox: $nums"
          throw new ParsingFailure(message, new Exception(message))
      }

      implicit val dec3DBbox: Decoder[ThreeDimBbox] = Decoder[List[Double]] map {
        case nums if nums.length == 6 =>
          nums(0) :: nums(1) :: nums(2) :: nums(3) :: nums(4) :: nums(5) :: HNil
        case nums if nums.length > 6 =>
          val message = s"Too many values for 3D bbox: $nums"
          throw new ParsingFailure(message, new Exception(message))
        case nums if nums.length < 6 =>
          val message = s"Too few values for 3D bbox: $nums"
          throw new ParsingFailure(message, new Exception(message))
      }

      implicit val decTimeRange
          : Decoder[(Option[Instant], Option[Instant])] = Decoder[String] map {
        str =>
          val components = str.replace("[", "").replace("]", "").split(",") map {
            _.trim
          }
          components match {
            case parts if parts.length == 2 =>
              val start = parts.head
              val end = parts.drop(1).head
              (decode[Instant](start).toOption, decode[Instant](end).toOption)
            case parts if parts.length > 2 =>
              val message = "Too many elements for temporal extent: $parts"
              throw new ParsingFailure(message, new Exception(message))
            case parts if parts.length < 2 =>
              val message = "Too few elements for temporal extent: $parts"
              throw new ParsingFailure(message, new Exception(message))
          }
      }
  }
}
