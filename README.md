Events
======

[![Maven Central](https://img.shields.io/maven-central/v/com.alexvasilkov/events.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.alexvasilkov/events)

Android event bus for remote methods calls and multithreading.  
Besides components decoupling `Events` allows background subscribers execution
with callback methods which effectively replaces AsyncTask.
Unlike class-per-event approach of [EventBus](https://github.com/greenrobot/EventBus)
and [Otto](http://square.github.io/otto), `Events` uses plain string keys
as events identifiers and provides dynamic events payload.

### Usage ###

#### Posting ####

Posting event is the first part of event's journey through the bus.
Each event consists of mandatory event key (string) and optional parameters and tags. 

Posting event without parameters:

```java
Events.post(EventsKeys.ON_CLICK);
```

Posting event with dynamic parameters:

```java
Events.create(EventsKeys.ON_CLICK).param(item, true).post();
```

#### Subscribing ####

Next we'll need to register events subscribers. Subscribers are methods which will be called
when event is posted into the bus. This is next step of event's journey.

Subscribing to particular event:

```java
@Subscribe(EventsKeys.ON_CLICK)
private void onClick(Item item, boolean expand) {
    ...
}
```

By default subscriber method is always called on main thread, but this can be modified,
see Multithreading section below.  
Any method names and access modifiers are allowed. Method parameters can be of any number and any
types but they should match the one which was posted by sender.
Another options and more info can be found in
[`Events.Subscribe`](http://static.javadoc.io/com.alexvasilkov/events/1.0.0/com/alexvasilkov/events/Events.Subscribe.html)
javadoc. 

Now we need to register subscriber within event bus. That's done by passing instance of the object
containing previous method:

```java
Events.register(this);
```

Note that all methods from immediate class type as well as from all super types will be registered.

Don't forget to unregister subscriber when it is no more needed:

```java
Events.unregister(this);
```

#### Multithreading ####

Subscriber can choose to be executed in background to offload the main thread.
This is done using
[`Events.Background`](http://static.javadoc.io/com.alexvasilkov/events/1.0.0/com/alexvasilkov/events/Events.Background.html)
annotation:

```java
@Background
@Subscribe(EventsKeys.LOAD_REPOSITORY)
private static void loadRepository(String repoId) {
    ...
}
```

The only difference from regular subscription is that method must be static now.

In order to register such subscriber you will need to pass class type instead of particular instance:

```java
Events.register(BackgroundTasks.class);
```

This ensures that you will not accidentally leak an object (i.e. Activity) which should not be kept
in memory during background execution.

#### Receiving callbacks ####

Once method is posted you may want to track it's execution. Few methods are available here.

##### Status #####

Subscribing for execution started / finished state:

```java
@Status(EventsKeys.LOAD_REPOSITORY)
private static void onLoadRepositoryStatus(EventStatus status) {
    ...
}
```

`STARTED` state is always called before any results or failure callbacks (see further)
and `FINISHED` state is always the last one. See
[`Events.Status`](http://static.javadoc.io/com.alexvasilkov/events/1.0.0/com/alexvasilkov/events/Events.Status.html)
for more details.

##### Result #####

To receive subscriber execution results you will need to specify return value for subscriber method:

```java
@Subscribe(EventsKeys.LOAD_REPOSITORY)
private static List<Repository> loadRepository(String repoId) {
    ...
    return list;
}
```

And then you can receive the result using:

```java
@Result(EventsKeys.LOAD_REPOSITORY)
private void onLoadRepositoryResult(List<Repository> list) {
    ...
}
```

Note that you may receive several results if, for example, several subscribers were registered
or several results were posted from single subscriber. There are also a few ways to provide
subscriber execution result. See 
[`Events.Result`](http://static.javadoc.io/com.alexvasilkov/events/1.0.0/com/alexvasilkov/events/Events.Result.html)
javadoc for more details.

##### Failures #####

Errors may occur during subscriber execution. All errors are caught and logged by default,
but you may also want to provide custom error handling logic.

Consider you have next subscriber:

```java
@Subscribe(EventsKeys.LOAD_REPOSITORY)
private static List<Repository> loadRepository(String repoId) throws IOException {
    ...
    throw new IOException();
}
```

Such exception can be handled using:

```java
@Failure(EventsKeys.LOAD_REPOSITORY)
private void onLoadRepositoryFailure(Throwable error) {
    ...
}
```

You may also add global error handler by skipping event key:

```java
@Failure
private static void onAnyError(Throwable error) {
    ...
}
```

More info can be found in
[`Events.Failure`](http://static.javadoc.io/com.alexvasilkov/events/1.0.0/com/alexvasilkov/events/Events.Failure.html)
javadoc.


Note that all callback methods are called on main thread, there is no option to execute them
in background.

#### Gradle ####

```groovy
dependencies {
    compile 'com.alexvasilkov:events:1.0.0'
}
```

[Javadoc](http://www.javadoc.io/doc/com.alexvasilkov/events/1.0.0) documentation.

#### License ####

    Copyright 2015 Alex Vasilkov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
