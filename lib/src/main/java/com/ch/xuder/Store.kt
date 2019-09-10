package com.ch.xuder

/**
 * Represents a subscription to the [Store] and contains logic necessary
 * to [unsubscribe]
 */
interface Subscription {
  fun unsubscribe()
}

/**
 * Represents a pure function responsible for calculating state changes and
 * producing a [Store.Transition] for subscribers to consume.
 */
typealias Reduce<StateT> = (action: Any, state: StateT) -> Store.Transition<StateT>

/**
 * Represents an active receiver of [Store.Transition]s
 */
typealias Subscribe<StateT> = (Store.Transition<StateT>) -> Unit

/**
 * Represents a dispatch function
 */
typealias Dispatch = (Any) -> Unit

/**
 * The single source of truth for application state. Contains a list of [subscribers]
 * and receives [dispatch]ed actions.
 */
data class Store<StateT>(
  val initialState: StateT,
  val reducers: List<Reduce<StateT>>
) {
  /**
   * Represents the state machine's output. Reducers remain pure by packaging
   * [toState] and [commands] into the object.
   */
  class Transition<StateT>(
    val toState: StateT,
    vararg val commands: Any
  )

  var state: StateT = initialState
    private set

  private val subscribers: MutableList<Subscribe<StateT>> = mutableListOf()

  /**
   * Adds [onTransition] to the list of [subscribers], emits the latest state as a [Transition],
   * and returns a [Subscription] that can be used to [Subscription.unsubscribe]
   */
  fun subscribe(onTransition: (transition: Transition<StateT>) -> Unit): Subscription =
    subscribers
      .add(onTransition)
      .also { onTransition(Transition(state)) }
      .let {
        object : Subscription {
          override fun unsubscribe() {
            subscribers.remove(onTransition)
          }
        }
      }

  /**
   * Receives actions. Feeds each action to [reducers] and notifies
   * subscribers of new [Transition]s
   */
  val dispatch: Dispatch = { event: Any ->
    reducers.forEach { reduce ->
      with(reduce(event, state)) {
        state = toState
        subscribers.forEach { it(this) }
      }
    }
  }
}
