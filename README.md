# Google Closure Compiler Maven Plugin

This project is forked from https://github.com/gli/closure-compiler-maven-plugin
to add these features:

* Add `<entryPoint>` configuration to include only transitive dependencies of
  specified entry point.
* Patched Closure Compiler so `goog.require` can appear inside `goog.scope` and
  it returns a namespace you can assign to an alias.


## Configure Your Maven Project To Use Plugin


### Configure Maven Repository

Configure your project to download the plugin from this repository:

    <repositories>
      <!-- ... -->

      <repository>
        <id>pukkaone-releases</id>
        <url>https://github.com/pukkaone/maven-repository/raw/master/releases</url>
      </repository>

      <repository>
        <id>pukkaone-snapshots</id>
        <url>https://github.com/pukkaone/maven-repository/raw/master/snapshots</url>
        <releases>
          <enabled>false</enabled>
        </releases>
        <snapshots>
          <enabled>true</enabled>
          <updatePolicy>always</updatePolicy>
        </snapshots>
      </repository>

      <!-- ... -->
    </repositories>


### Configure Plugin

You can configure the plugin like this:

    <properties>
      <closure.source>src/main/js</closure.source>
      <closure.externs>src/main/externs</closure.externs>
      <closure.outputFile>${project.build.directory}/${project.build.finalName}/compiled.js</closure.outputFile>
    </properties>

    <build>
      <plugins>
        <!-- ... -->

        <plugin>
          <groupId>com.github.pukkaone</groupId>
          <artifactId>closure-compiler-maven-plugin</artifactId>
          <version>1.0.0-SNAPSHOT</version>
          <executions>
            <execution>
              <phase>process-resources</phase>
              <goals>
                <goal>compile</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <sourceDirectory>${closure.source}</sourceDirectory>
            <externsSourceDirectory>${closure.externs}</externsSourceDirectory>
            <entryPoint>application.Main</entryPoint>
            <outputFile>${closure.outputFile}</outputFile>
            <compilationLevel>SIMPLE_OPTIMIZATIONS</compilationLevel>
            <merge>true</merge>
            <loggingLevel>WARNING</loggingLevel>
            <warningLevel>VERBOSE</warningLevel>
            <generateExports>true</generateExports>
            <addDefaultExterns>true</addDefaultExterns>
            <logExternFiles>true</logExternFiles>
            <logSourceFiles>true</logSourceFiles>
            <stopOnErrors>true</stopOnErrors>
            <stopOnWarnings>true</stopOnWarnings>
          </configuration>
        </plugin>

        <!-- ... -->
      </plugins>
    </build>
