# 6 - Event-driven Architecture and CQRS

Welcome to the final exercise in this workshop! To get a better understanding of
how to effectively use the core concepts we have learned so far, we will implement
a complex distributed architecture using the Apache Kafka.

Meet Biff Jezos, founder of the soon-to-be leading online shopping platform:
```yaml
 ____                                     
|  _ \ __ _ _ __ ___ ___  _ __ ___   ___ | | ___
| |_) / _` | '__/ __/ _ \| '_ ` _ \ / _ \| |/ _ \
|  __/ (_| | | | (_| (_) | | | | | | (_) | | (_) |
|_|   \__,_|_|  \___\___/|_| |_| |_|\___/|_|\___/
```

An eCommerce system offers many points of contact for event-driven architecture and
CQRS – orders, payments, inventory, shipping, notifications, risk scoring, etc.

Let's help Biff by applying all our newly acquired knowledge and skills around
Apache Kafka!


## Before we start

- This is a team exercise! Due to the very limited time constraint, we have to
work in parallel. Communication is king!
- Let's agree on a basic architecture and design before starting to hack.
- Remember: this is just a demo project! Don't waste too much time on beauty or
sophistication. Things don't have to be perfect. Quick hacks will do most of the
time.
- Remember one of the most important rules for software architects and developers: KISS!


## Level 6.1 - Service Choreography

As with almost every e-commerce system, the core of **Parcomolo** is the customer's
ability to place orders, pay, and receive goods. Let's start by
implementing the basic services for these activities!

### Learning Objectives
- Reasing about an architecture driven by events, what data to share and how to implement
different flows
- Implementing a basic event choreography

### Tasks
1. OrderService - Place orders and inform downstream services
   1. create a REST endpoint `/orders` which acts as `PlaceOrderCommand`
   2. create a REST endpoint `/cancelOrder` which acts as `CancelOrderCommand`
   3. persist the Write model: Order Entity with status (PENDING, CANCELLED)
   4. emit a respective event to Kafka (OrderPlacedEvent, OrderCancelled)
2. Inventory Service - React to OrderPlacedEvent from Kafka and reserve stock asynchronously.
   1. Listen to order-events Kafka topic
   2. Check & reserve stock
   3. Publish: StockReservedEvent or OutOfStockEvent
3. Payment Service - React to successful stock reservations and simulate a payment process. On success or failure, emit the appropriate event.
   1. Listen to stock-events (for StockReservedEvent)
   2. Simulate payment logic (success/failure)
   3. Publish: PaymentSucceededEvent or PaymentFailedEvent
4. Shipping Service - React to successful payments and simulate scheduling + shipping of the order.
   1. Listen for PaymentSucceededEvent
   2. Simulate shipping flow
   3. Emit both, ShippingScheduledEvent and ItemShippedEvent


## Level 6.2 - Real-Time Read Model

With the core services running, Biff wants the users to be able to track the current status of their orders.

Use Kafka Streams to materialize the current order state by processing the full event flow.

### Learning Objectives
- Implementing a CQRS query model

### Tasks

1. Create a Kafka Streams topology that listens to all relevant events:
   - OrderPlacedEvent
   - StockReservedEvent / OutOfStockEvent
   - PaymentSucceededEvent / PaymentFailedEvent
   - ShippingScheduledEvent / ItemShippedEvent
2. Build an aggregated order state
3. Persist this state
4. Provide a REST endpoint: GET /orders/{orderId} to return the full order status


## Level 6.3 - Service Orchestrator

Biff has come to realise that there are many orders which have not been paid
successfully and their corresponding inventory reservations have not been
released. He requests a centralized service that manages the entire process
to gain better visibility into all individual steps of the order process.

### Learning Objectives
- Handle failure compensation and workflow coordination using a central Orchestrator Service

### Tasks
1. Create a new application OrderSagaOrchestrator that acts as central orchestrator for the
following flow:
   ```bash
   [Orders] → Send OrderPlacedEvent
      ↓
   [Orchestrator] → Send ReserveStockCommand
      ↓
   [Inventory] → StockReservedEvent
      ↓
   [Orchestrator] → Send ProcessPaymentCommand
      ↓
   [Payment] → PaymentSucceededEvent
      ↓
   [Orchestrator] → Send ScheduleShippingCommand
      ↓
   [Shipping] → ItemShippedEvent
      ↓
   [Orchestrator] → Emit OrderCompletedEvent
   ```
2. Identify all situations that require compensating actions (e.g. ReleaseStockCommand)
3. Update all other services to only react to the saga commands.


### Level 6.4 - More Exercises

At the moment we only have a bare minimum required for a full eCommerce shop.
There is a big variety of other use cases that such a system could and should support,
including but not limited to:

- an InvoiceService for creating invoices for all successful orders
- an AddressService to mangage different living and shipping addresses
- a SupplierService to enable third-party suppliers to use our platform to sell their products
- a CustomerService to enable customers to register and manage their account
- a RiskService to detect fraudulent behavior and block orders from being shipped
- a NotificationService to inform users about important events (e.g.: order shipped, payment failed)
- a CRMService to create marketing campaings with special offers.

Most of the services will emit events that have direct or indirect effect on other workflows.
Go wild and have fun exploring :-)
