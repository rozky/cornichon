cornichon [![Build Status](https://travis-ci.org/agourlay/cornichon.png?branch=master)](https://travis-ci.org/agourlay/cornichon) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.agourlay/cornichon_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.agourlay/cornichon_2.11) [![License](http://img.shields.io/:license-Apache%202-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
=========

[![Join the chat at https://gitter.im/agourlay/cornichon](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/agourlay/cornichon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A Scala DSL for testing JSON HTTP API 

Quick example? It looks like [this](#usage)

Inside a Scala project, place your Cornichon tests in ```src/test/scala``` and run them using ```sbt test```.

## Motivation

Offering an extensible DSL to specify behaviours of JSON HTTP APIs in Scala.

## Installation

``` scala
libraryDependencies += "com.github.agourlay" %% "cornichon" % "0.1.1" % "test"
```

## Structure

A Cornichon test is the definition of a so-called ```feature```. 

A ```feature``` can have several ```scenarios``` which can have several ```steps```.

In the example below, we have one ```feature``` with one ```scenario``` with two ```steps```.

```scala
class CornichonExamplesSpec extends CornichonFeature {
  def feature = Feature("Checking google")(
  
      Scenario("Google is up and running") { implicit b ⇒
  
        When I GET("http://google.com")
  
        Then assert status_is(302)
      }
  )
}
```

A ```feature``` fails - if one or more ```scenarios``` fail.

A ```scenario``` fails - if at least one ```step``` fails.

A ```scenario``` will stop at the first failed step encountered and ignore the remaining ```steps```.

Check this [section](#implicit-builder) if you wonder what this ```implicit b =>``` thingy is.

## DSL

Statements start with one of the prefixes below followed by a ```step``` definition :

- Given I | a
- When I | a
- And I | a | assert | assert_not (expects the step to fail)
- Then I | a | assert | assert_not (expects the step to fail)

Those prefixes do not change the behaviour of the steps.

First run a ```step``` with a side effect or a result then assert its value in a second ```step```.

## Built-in steps

Cornichon has a set of built-in steps for various HTTP calls and assertions on the response.


- GET and DELETE share the same signature

 (url, optional params String tuples*)(optional tuple headers Seq)

```scala
GET("http://superhero.io/daredevil")

GET("http://superhero.io/daredevil", params = "firstParam" → "value1", "secondParam" → "value2")

DELETE("http://superhero.io/daredevil")(headers = Seq(("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==")))
```

- POST and UPDATE share the same signature

 (url, payload as String, optional params String tuples*)(optional tuple headers Seq)

```scala
POST("http://superhero.io/batman", payload = "JSON description of Batman goes here")

PUT("http://superhero.io/batman", payload = "JSON description of Batman goes here", params = "firstParam" → "value1", "secondParam" → "value2")

POST("http://superhero.io/batman", payload = "JSON description of Batman goes here")(headers = Seq(("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==")))
```

- assert response status

```scala
status_is(200)

```

- assert response headers

```scala
headers_contain("cache-control" → "no-cache")

```

- assert response body comes with different flavours (ignoringKeys, whiteList))

```scala
body_is(
  """
  {
    "name": "Batman",
    "realName": "Bruce Wayne",
    "city": "Gotham city",
    "hasSuperpowers": false,
    "publisher":{
      "name":"DC",
      "foundationYear":1934,
      "location":"Burbank, California"
    }
  }
  """)

body_is(
  """
  {
    "name": "Batman",
    "realName": "Bruce Wayne"
  }
  """, ignoring = "city", "hasSuperpowers", "publisher")

body_is(whiteList = true, expected = """
  {
    "name": "Batman",
    "realName": "Bruce Wayne"
  }
  """)
```

It also possible to use [Json4s XPath](http://json4s.org/#xpath--hofs) extractors
  
```scala
body_is(_ \ "city", "Gotham city")

body_is(_ \ "hasSuperpowers", false)

body_is(_ \ "publisher" \ "name", "DC")

body_is(_ \ "publisher" \ "foundationYear", 1934)

```

If the endpoint returns a collection assert response body has several options (ordered, ignoring and using data table)

```scala
body_is(ordered = true,
  """
  [{
    "name": "Batman",
    "realName": "Bruce Wayne"
  },
  {
    "name": "Superman",
    "realName": "Clark Kent"
  }]
  """, ignoring = "city", "hasSuperpowers", "publisher")

body_is(ordered = false,
  """
  [{
    "name": "Superman",
    "realName": "Clark Kent"
  },
  {
    "name": "Batman",
    "realName": "Bruce Wayne"
  }]
  """, ignoring = "city", "hasSuperpowers", "publisher")
  
body_is(ordered = true, expected = """
  |    name     |    realName    |     city      |  hasSuperpowers |
  | "Batman"    | "Bruce Wayne"  | "Gotham city" |      false      |
  | "Superman"  | "Clark Kent"   | "Metropolis"  |      true       |
""", ignoring = "publisher")  
  
response_array_size_is(2)
  
response_array_contains("""
  {
    "name": "Batman",
    "realName": "Bruce Wayne",
    "city": "Gotham city",
    "hasSuperpowers": false,
    "publisher":{
      "name":"DC",
      "foundationYear":1934,
      "location":"Burbank, California"
    }
  }
  """)
  
```

response_array_size_is and response_array_contains have also the possibility to accept an extra first argument (a root key or a JValue extractor) to work on a nested collection.

- setting a value in ```session```

```scala
save("favorite-superhero" → "Batman")
```

- saving value to ```session``

```scala
save_body_key("city", "batman-city")

save_from_body(_ \ "city", "batman-city")

```

- asserting value in ```session```

```scala
session_contains("favorite-superhero" → "Batman")
```

- showing sessing content for debugging purpose

```scala
 show_session

 show_last_status

 show_last_response_body

 show_last_response_headers
```

- repeating a series of ```steps``` (can be nested)

```scala
Repeat(3) {
  When I GET("http://superhero.io/batman")

  Then assert status_is(200)
}
```

- repeating a series of ```steps``` until it succeeds over a period of time at a specified interval (handy for eventually consistent endpoints)

```scala
Eventually(maxDuration = 15.seconds, interval = 200.milliseconds) {

    When I GET("http://superhero.io/random")

    Then assert body_is(
      """
    {
      "name": "Batman",
      "realName": "Bruce Wayne",
      "city": "Gotham city"
    }
    """, ignoring = "hasSuperpowers", "publisher"
    )
  }
```

- WithHeaders bloc automatically sets headers for several steps useful for authenticated scenario.

```scala
WithHeaders(("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==")){
  When I GET("http://superhero.io/secured")
  When I GET("http://superhero.io/secured")
}

```
        
- validating response against Json schema

```scala
body_against_schema("http://link.to.json.schema")

```

- experimental support for Server-Sent-Event.
 
 SSE streams are aggregated over a period of time in an array, therefore the previous array predicates can be re-used.

```scala
When I GET_SSE(s"http://superhero.io/stream", takeWithin = 1.seconds, params = "justName" → "true")

Then assert response_array_size_is(2)

Then assert body_is("""
  |   eventType      |    data     |
  | "superhero name" |  "Batman"   |
  | "superhero name" | "Superman"  |
""")
```

Those descriptions might be already outdated, in case of doubt always refer to these [examples](https://github.com/agourlay/cornichon/blob/master/src/test/scala/com/github/agourlay/cornichon/examples/CornichonExamplesSpec.scala) as they are executed as part of Cornichon's test suite.

## Steps

A ```step``` is an abstraction describing an action which is a function turning a ```Session``` into a result, a new ```Session``` and an expected result value.

In terms of Scala data type it is

```scala
case class ExecutableStep[A](
  title: String,
  action: Session ⇒ (A, Session, A)
)
```

A ```step``` can access and return a modified ```session``` object. A ```session``` is a Map-like object used to propagate state throughout a ```scenario```.


So the simplest executable statement in the DSL is

```scala
When I ExecutableStep("do nothing", s => (true, s, true))
```

Let's try to assert the result of a computation

```scala
When I ExecutableStep("calculate", s => (2 + 2, s, 4))
```

The ```session``` is used to store the result of a computation in order to reuse it or to apply more advanced assertions on it later.


```scala
When I ExecutableStep(
  title = "run crazy computation",
  action = s => {
    val res = crazy-computation()
    (res.isSuccess, s.add("result", res.infos), true)
  })

Then assert ExecutableStep(
  title = "check computation infos",
  action = s => {
    val resInfos = s.get("result)
    (resInfos, s, "Everything is fine")
  })
```

This is extremely low level and you should never write your test like that.

Fortunately a bunch of built-in steps and primitive building blocs are already available.

Most of the time you will create your own trait containing your custom steps :

```scala
trait MySteps {
  this: CornichonFeature ⇒

  // here access all the goodies from the DSLs and the HttpService.
}

```

## Placeholders

Most built-in steps can use placeholders in their arguments, those will be automatically resolved from the ```session```:

- Url
- Expected body
- HTTP params (name and value)
- HTTP headers (name and value)

```scala
Given I save("favorite-superhero" → "Batman")

Then assert session_contains("favorite-superhero" → "Batman")

When I GET("http://localhost:8080/superheroes/<favorite-superhero>")

Then assert body_is(
  """
  {
    "name": "<favorite-superhero>",
    "realName": "Bruce Wayne",
    "city": "Gotham city",
    "publisher": "DC"
  }
  """
)

And I save_body_key("city", "batman-city")

Then assert session_contains("batman-city" → "Gotham city")

Then assert body_is(
  """
  {
    "name": "<favorite-superhero>",
    "realName": "Bruce Wayne",
    "city": "<batman-city>",
    "publisher": "DC"
  }
  """
)

```

It is also possible to inject random values inside placeholders using:

- <random-uuid> for a random UUID
- <random-positive-integer> for a random Integer between 0-100
- <random-string> for a random String of length 5

```scala
POST("http://url.io/somethingWithAnId", payload = """
  {
    "id" : "<random-uuid>"
  }
""")
```

## Usage

Create a test Scala class extending ```CornichonFeature``` and implement the ```feature``` function as presented below.

```scala
class CornichonReadmeExample extends CornichonFeature {

  def feature =
    Feature("Cornichon feature Example")(

      Scenario("Test read demo") { implicit b ⇒
        When I GET("myUrl/superheroes/Batman")

        Then assert status_is(200)

        And assert body_is(
          """
          {
            "name": "Batman",
            "realName": "Bruce Wayne",
            "city": "Gotham city",
            "publisher": "DC"
          }
          """
        )
      }
    )
}
```

Cornichon is currently integrated with ScalaTest, so you just have to run ```sbt run``` to trigger its execution.

For more examples see the following [file](https://github.com/agourlay/cornichon/blob/master/src/test/scala/com/github/agourlay/cornichon/examples/CornichonExamplesSpec.scala).

## Feature options

To implement a ```CornichonFeature``` it is only required to implement the ```feature``` function. However a number of useful options are available using override.

### Before and after hooks

Hooks are available to set up and tear down things as usual but this feature is not integrated into the DSL.

Four hooks are available to be overridden in ```CornichonFeature``` with self-explanatory names:

```scala
def beforeFeature(): Unit
def afterFeature(): Unit

val beforeEachScenario: Seq[ExecutableStep[_]]
val afterEachScenario: Seq[ExecutableStep[_]]
```

### Base URL

Instead of repeating at each HTTP statement the full URL, it is possible to set a common URL for the entire ```feature``` by overriding:

```scala
override lazy val baseUrl = s"http://localhost:8080"

```

and then only provide the missing part in the HTTP step definition

```scala
 When I GET("/superheroes/Batman")
 
 When I POST("/superheroes", payload ="")
 
 When I DELETE("/superheroes/GreenLantern")

```

### Execution model

By default everything is executed sequentially:

- for each ```Feature```
- for each ```Scenario```
- execute each ```Step```

This makes it really easy to follow what is going on.

It is also possible to run the ```Scenario``` of specific ```Features``` in parallel by overriding the property ```parallelExecution``` in ```CornichonFeature```.

```scala
class ParallelFeatureExample extends CornichonFeature {

  override val parallelExecution = true

  def feature =
    Feature("Cornichon parallel scenarios Example")(
    ...
    )
}
```

Before using this feature make sure that the scenarios are completely independent as it would create race-condition otherwise.

It is important to notice that the logging of ```Steps``` execution still happens in real-time making it more difficult to follow the execution.

## Implicit builder

In order to have a clean look Cornichon uses mutation to build a ```scenario```. The argument ```implicit b =>``` represents an implicit step builder required to construct a ```scenario```.

The ```feature``` construction uses a varargs of ```scenario```, that is why it is not using curly braces and still requires ```,``` between ```scenarios```.

Until a better solution is implemented, do not forget those :)