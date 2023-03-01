

## 构建 elasticsearch 项目

几个重要的文件
- ./buildSrc/src/main/groovy/org/elasticsearch/gradle/BuildPlugin.groovy:
- ./build.gradle
- ./buildSrc/build.gradle
- ./server/build.gradle

### 1. BuildPlugin.groovy

BuildPlugin 是es自己实现的BuildPlugin，里面定义了要执行的task

``` java

class BuildPlugin implements Plugin<Project> {
    
    @Override
    void apply(Project project) {
        project.pluginManager.apply('java')
        project.pluginManager.apply('carrotsearch.randomized-testing')
        //配置项目的依赖传递
        configureConfigurations(project)
        //为jar增加 manifest
        configureJars(project) // jar config must be added before info broker
        // these plugins add lots of info to our jars
        project.pluginManager.apply('nebula.info-broker')
        project.pluginManager.apply('nebula.info-basic')
        project.pluginManager.apply('nebula.info-java')
        project.pluginManager.apply('nebula.info-scm')
        project.pluginManager.apply('nebula.info-jar')

        project.getTasks().create("buildResources", ExportElasticsearchBuildResourcesTask)
        //全局构建信息，只会执行一次
        globalBuildInfo(project)
        //配置仓库
        configureRepositories(project)
        project.ext.versions = VersionProperties.versions
        //配置source目录
        configureSourceSets(project)
        //配置JavaCompile task
        configureCompile(project)
        //配置Javadoc task
        configureJavadoc(project)
        //配置SourcesJar task
        configureSourcesJar(project)
        configurePomGeneration(project)

        applyCommonTestConfig(project)
        configureTest(project)
        //配置依赖检查，包括checkStyle,Forbidden,License
        configurePrecommit(project)
        configureDependenciesInfo(project)
    }

    
}

```


注意apply方法是在 Configure 阶段执行。 

### 2. build.gradle
根目录中的build.gradle 定义了更项目的build配置

``` build.gradle

plugins {
    id 'com.gradle.build-scan' version '3.5'
    id 'base'
}

apply plugin: 'nebula.info-scm'
apply from: 'gradle/build-scan.gradle'
apply from: 'gradle/build-complete.gradle'
apply from: 'gradle/ide.gradle'

//project的Script block，配置项目 
allprojects{
    group = 'org.elasticsearch'
    version = VersionProperties.elasticsearch
    description = "Elasticsearch subproject ${project.path}"  
}

//配置子项目
subprojects{
    
}

```



### 3. buildSrc/build.gradle

``` build.gradle

plugins {
  id 'java-gradle-plugin'
  id 'groovy'
}

group = 'org.elasticsearch.gradle'

File propsFile = project.file('version.properties')
Properties props = VersionPropertiesLoader.loadBuildSrcVersion(propsFile)
//es的版本
version = props.getProperty("elasticsearch")

//java_plugin下的task
processResources {
    
}
//java_plugin下的task
sourceSets{
    
}

//设置Project下的 dependencies
dependencies{
    
}
```

### 4. server/build.gradle

```

apply plugin: 'elasticsearch.build'
apply plugin: 'nebula.optional-base'
apply plugin: 'nebula.maven-base-publish'
apply plugin: 'nebula.maven-scm'

//项目依赖
dependencies {
    
}
//单元测试
unitTest {
    
}

//集成测试
task integTest{
    
}

//定义 forbiddenPatterns
forbiddenPatterns{
    
}
```