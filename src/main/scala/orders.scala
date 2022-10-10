import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.annotation.tailrec

object orders {
  def ageCalculator(monthAgo: Int)(orderDate: LocalDateTime): LocalDateTime =
    orderDate.atZone(ZoneOffset.UTC).minusMonths(monthAgo).toLocalDateTime

  def intervalCategorizer(categories: Map[String, (LocalDateTime, LocalDateTime)], date: LocalDateTime): String = {
    println(s"Looking for $date in $categories")
    categories.filter { category =>
      date.isAfter(category._2._2) &&
        date.isBefore(category._2._1)
    }.keys.headOption.orNull
  }

  def groupByInMultipleGroups(orderList: List[Order],
                              categories: LocalDateTime => Map[String, (LocalDateTime, LocalDateTime)]): Map[String, List[Order]] = {
    @tailrec
    def occurrences(orders: List[Order], items: List[Item], groups: Map[String, List[Order]]): Map[String, List[Order]] = {
      if (orders.isEmpty & items.isEmpty) Map.empty
      else if (items.isEmpty) occurrences(orders.tail, orders.head.purchases, groups)
      else {
        val category = intervalCategorizer(categories(orders.head.orderDate), items.head.product.creationDate)
        val currentGroups = groups.getOrElse(category, List.empty)
        occurrences(orders, items.tail, groups + (category -> (currentGroups :+ orders.head)))
      }
    }

    occurrences(orderList, List.empty, Map.empty)
  }

  def main(args: Array[String]): Unit = {

    val DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val startDate = LocalDateTime.parse(args(0), DATE_PATTERN)
    val endDate = LocalDateTime.parse(args(1), DATE_PATTERN)

    def intervals(creation: LocalDateTime): Map[String, (LocalDateTime, LocalDateTime)] =
      Map("1-3 months" -> (0, 3), "4-6 months" -> (3, 6), "7-12 months" -> (6, 12), ">12 months" -> (12, null)) map {
        case name -> (end1: Int, end2) if end2 == null =>
          name -> (ageCalculator(end1)(creation), ageCalculator(Int.MaxValue)(creation))
        case name -> (end1: Int, end2: Int) =>
          name -> (ageCalculator(end1)(creation), ageCalculator(end2)(creation))
      }


    // this is the bonus feature :)
    val INTERVAL_CODECS: List[String] = if(args.lift(2).isDefined) args.lift(2).orNull.split(",").map {
      case "1-3" => "1-3 months"
      case "4-6" => "4-6 months"
      case "7-12" => "7-12 months"
      case ">12" => ">12 months"
    }.toList else List.empty

    def selectedIntervals(creation: LocalDateTime): Map[String, (LocalDateTime, LocalDateTime)] =
      if (INTERVAL_CODECS.isEmpty) intervals(creation)
      else intervals(creation).filter(interval => INTERVAL_CODECS.contains(interval._1))


    // creating sample data for testing purpose
    val ali = Customer("Ali", "ali.t.asl@outlook.com", 98921)
    val aliHome = Address("Iran", "Tehran", "", "", "X233")
    val orderDate = LocalDateTime.parse("2022-10-01 00:00:00", DATE_PATTERN)
    val product = Product("blower", "technical", 35, 2000, LocalDateTime.parse("2022-10-11 00:00:00", DATE_PATTERN))
    val item = Item(product, 2, 3990, 200, 400)
    val ordersDatabase: List[Order] = (1 to 100).toList map { num =>
      Order(List(item), ali, aliHome, 0, orderDate.minusMonths(num))
    }

    val filteredOrders = ordersDatabase.filter { order =>
      order.orderDate.isBefore(endDate)
        && order.orderDate.isAfter(startDate)
    }

    val groups = groupByInMultipleGroups(ordersDatabase, selectedIntervals)

    println(groups mkString "\n")
//    groups.foreach { case name -> list =>
//      println(s"$name: ${list.length} orders")
//    }
  }
}
