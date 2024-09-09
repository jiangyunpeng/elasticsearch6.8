## 说明

maven 仓库可能会删除已经老的包，所以非常有可能遇到曾经编译通过，过一段时间编译失败的问题，比如：
```shell
* What went wrong:
Execution failed for task ':buildSrc:compileJava'.
> Could not resolve all files for configuration ':buildSrc:compileClasspath'.
   > Could not find com.avast.gradle:gradle-docker-compose-plugin:0.13.4.
     Searched in the following locations:
       - https://repo.maven.apache.org/maven2/com/avast/gradle/gradle-docker-compose-plugin/0.13.4/gradle-docker-compose-plugin-0.13.4.pom
       - https://plugins.gradle.org/m2/com/avast/gradle/gradle-docker-compose-plugin/0.13.4/gradle-docker-compose-plugin-0.13.4.pom
     Required by:
         project :buildSrc
```

解决办法只能升级相关包，已经修改过buildSrc/build.gradle
