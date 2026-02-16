# Queue Design

## Files

- `src/main/scala/com/example/linkextractor/queue/Queue.scala`
- `src/main/scala/com/example/linkextractor/queue/QueueConfig.scala`
- `src/main/scala/com/example/linkextractor/queue/LockFreeBoundedQueue.scala`

## Queue API (`Queue[A]`)

- `enqueue(value: A): Queue.EnqueueResult`
- `dequeue(): Future[Option[A]]`
- `close(): Unit`

`EnqueueResult` cases:

- `Enqueued`
- `EnqueuedAfterDroppingOldest`
- `Closed`

## Bounded Policy

- Capacity comes from `QueueConfig.BoundedQueueConfig`.
- On full buffer:
  - oldest item is dropped,
  - new item is appended,
  - result is `EnqueuedAfterDroppingOldest`.

## Lock-Free Algorithm

`LockFreeBoundedQueue` stores one immutable `State` in `AtomicReference`.

State fields:

- `buffer: scala.collection.immutable.Queue[A]`
- `size: Int` (O(1) capacity checks)
- `waiters: Queue[Promise[Option[A]]]`
- `closed: Boolean`

Core operations:

- `enqueue`
  - if closed: return `Closed`
  - if waiter exists: complete oldest waiter directly (`Some(value)`)
  - else append to bounded buffer (with drop-oldest on overflow)
- `dequeue`
  - fast-path attempts to claim buffered value
  - if empty + open: register waiter promise
  - if empty + closed: return `Future.successful(None)`
- `close`
  - idempotently set `closed = true`
  - resolve pending waiters from remaining buffered items, then with `None`

All transitions use CAS retry loops (`@tailrec`) and avoid explicit locks.

## Complexity

- Enqueue: amortized O(1)
- Dequeue: amortized O(1)
- Close: O(number of pending waiters + remaining buffered items)

