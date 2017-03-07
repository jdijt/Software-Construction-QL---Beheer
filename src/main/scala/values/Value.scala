package values

import java.text.SimpleDateFormat
import java.util.Date

import scala.math.BigDecimal.RoundingMode

sealed trait Value

case class BooleanValue(value: Boolean) extends Value {
  override def toString = value.toString
}

case class DateValue(value: Date) extends Value {
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  override def toString = dateFormat.format(value)
}

sealed trait NumericValue extends Value {
  val value: BigDecimal
}

case class IntegerValue(value: BigDecimal) extends NumericValue {
  override def toString = value.toString
}

case class DecimalValue(value: BigDecimal) extends NumericValue {
  override def toString = value.toString
}

case class MoneyValue(value: BigDecimal) extends NumericValue {
  override def toString = s"€${value.setScale(2, RoundingMode.HALF_EVEN)}"
}

case class StringValue(value: String) extends Value {
  override def toString = value
}

case object UndefinedValue extends Value {
  override def toString = "Value not defined"
}
