case class Order(purchases: List[Item], customer: Customer, shippingAddress: Address, grandTotal: Int, orderDate: java.time.LocalDateTime)
