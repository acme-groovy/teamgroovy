## download and use dynamic jdbc driver

```groovy
@Grab(group='com.h2database', module='h2', version='1.4.196', scope='test')
import groovyx.acme.teamgroovy.helpers.*;

SqlHelper.withInstance(
  driver: "org.h2.Driver",
  url:"jdbc:h2:mem:test",
  user:"sa",
  password:"",
){sql->
  println sql.rows([a:'hello world !'],"select :a as txt")
}
```
