## Vert.x virtual threads examples

NOTE: you can reuse this Maven project to start with.

### HTTP Client

[HTTP client](src/main/java/examples/core/HttpClientExample.java)

### Movie rating

The [application](src/main/java/examples/movierating/App.java) exposes a REST API for rating movies:

You can know more about a movie

```
> curl http://localhost:8080/movie/starwars
{"id":"starwars","title":"Star Wars"}
```

You can get the current rating of a movie:

```
> curl http://localhost:8080/getRating/indianajones
{"id":"indianajones","rating":5}
```

Finally you can rate a movie

```
> curl -X POST http://localhost:8080/rateMovie/starwars?rating=4
```

