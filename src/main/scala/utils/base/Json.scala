package pig
package utils.base

import io.circe.{Decoder, Encoder, Json}

import scala.collection.mutable

// 将实现了 Encoder 特质的类转换成 JSON 字符串
extension [T: Encoder](t: T) {
  def json: String = {
    import io.circe.syntax.*
    t.asJson.noSpaces
  }
}

extension (s: String) {
  def decode[T](using decoder: Decoder[T]): Either[io.circe.Error, T] = {
    io.circe.parser.decode[T](s)
  }
}

// 编码器：将 Any 转换为 Json
given anyEncoder: Encoder[Any] = Encoder.instance {
  case null                   => Json.Null
  case str: String            => Json.fromString(str)
  case int: Int               => Json.fromInt(int)
  case long: Long             => Json.fromLong(long)
  case double: Double         => Json.fromDoubleOrNull(double) // 使用 Circe 内置方法
  case float: Float           => Json.fromFloatOrNull(float)
  case bool: Boolean          => Json.fromBoolean(bool)
  case bigInt: BigInt         => Json.fromBigInt(bigInt)
  case bigDecimal: BigDecimal => Json.fromBigDecimal(bigDecimal)
  case list: List[_]          => Json.fromValues(list.map(anyEncoder(_)))
  case vector: Vector[_]      => Json.fromValues(vector.map(anyEncoder(_)))
  case map: Map[_, _]         =>
    Json.fromFields(map.map { case (k, v) => (k.toString, anyEncoder(v)) })
  case mutableMap: mutable.Map[_, _] =>
    Json.fromFields(mutableMap.map { case (k, v) => (k.toString, anyEncoder(v)) })
  case other => Json.fromString(other.toString) // fallback
}

// 解码器：将 Json 转换为 Any
given anyDecoder: Decoder[Any] = Decoder.instance { cursor =>
  val json = cursor.value

  json.fold(
    jsonNull = Right(null),
    jsonBoolean = Right(_),
    jsonString = Right(_),
    jsonNumber = { num =>
      // 尝试转换为最合适的数值类型
      num.toInt
        .map(Right(_))
        .orElse(num.toLong.map(Right(_)))
        .orElse(Some(Right(num.toFloat)))
        .orElse(Some(Right(num.toDouble)))
        .orElse(Some(Right(num.toByte)))
        .orElse(Some(Right(num.toShort)))
        .orElse(Some(Right(num.toBigInt)))
        .orElse(Some(Right(num.toBigDecimal)))
        .getOrElse(Right(num))
    },
    jsonArray = { jsonArray =>
      Right(jsonArray.map(anyDecoder.decodeJson).map(_.toOption).flatten)
    },
    jsonObject = { jsonObject =>
      Right(jsonObject.toMap.map { case (k, v) =>
        (k, anyDecoder.decodeJson(v).toOption)
      }.collect { case (k, Some(v)) => (k, v) })
    }
  )
}
