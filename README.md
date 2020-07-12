#Signals
_onNext_:
Emitted when publisher publishes successfully with data.

_OnComplete_:
Emitted when publishing is successful without any data.

_OnError_:
Emitted when there is an error during publishing.

#Operators
_map_:
Use for synchronous operations

_flatMap_:
Use for asynchronous operations. Chain flatmaps when operations are sequential.

For both map and flatmap try to stay away from nesting. Do one thing inside one operator and do it well.

_zip_ :
Use when two publishers are independent. Remember to handle empty scenarios for all zipped publishers.

_switchIfEmpty_ : 
switchIfEmpty() which takes a parameter is evaluated eagerly. Ensure that the operation inside doesn't have any impact if evaluated eagerly.
To defer evaluation use Mono.defer{}
switchIfEmpty{} which takes a lambda is lazily evaluated. This can also be used.

_doOnError_:
Use for side-effects, for example logging. This is true for all doOn methods.

_onErrorResume_:
Use inorder to return another stream of elements when an error is encountered.

_fromCallable_:
Use when making blockingCalls to protect the thread from blocking. Simple saying to toMono or Mono.just will not help.

_subscribeOn_:
Use when making multiple blockingCalls and they can be parallelized. ReactiveProgrammming is asynchronous and not concurrent. To achieve concurrency always delegate to different thread pools.